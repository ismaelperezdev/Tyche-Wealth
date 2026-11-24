# Spike: Estrategia de consulta de checkpoints de precios de mercado

Versión en inglés: [market-price-checkpoint-query-spike.en.md](market-price-checkpoint-query-spike.en.md).

## Investigación

Este spike decide cómo debe responder el endpoint de historial de Portfolio para gráficas sin exponer paginación al frontend, respetando la fecha de creación de cada activo y evitando presentar una respuesta incompleta como si fuera completa. Sustituye la pregunta sobre rango y granularidad que quedó pendiente en [el spike original de historial de precios](../market-data-price-history/market-data-price-history-spike.en.md); no reabre las demás decisiones de aquel documento.

El flujo relevante ya existe:

- `PortfolioMarketDataApi.getPriceHistory` expone `GET .../market-data/history` con `portfolioIds` o `assetIds`, `from` y `to` opcionales. La respuesta es una `List<AssetPriceCheckpointDto>`; el contrato público no tiene parámetros de paginación.
- `PortfolioMarketDataServiceImpl.getPriceHistory` resuelve solo activos vigentes del usuario autenticado, calcula el inicio efectivo, llama una vez a MDS con `offset=0` y `limit=maxResults`, valida la respuesta, cuenta las filas expandidas por activo y después las mapea. `app.market-data.max-results` tiene un valor predeterminado de 5.000.
- `PortfolioMarketDataHelper.effectiveHistoryFrom` limita un `from` omitido o anterior a la fecha de creación del activo seleccionado más antiguo. `countPriceHistoryResults` y `mapPriceHistory` expanden cada checkpoint de símbolo a los activos correspondientes y filtran checkpoints cuyo `capturedAt` precede a la fecha de creación de ese activo (o al `from` solicitado, si es posterior). Activos con el mismo símbolo pueden tener historiales elegibles distintos.
- `PortfolioMarketDataValidationHelper.validateHistoryCount` detecta una página MDS incompleta (`totalCount > content.size()`), y `validateExpandedHistoryCount` rechaza con el error existente `422` si el resultado expandido excede el límite configurado. La implementación no devuelve un prefijo truncado como respuesta exitosa.
- `MarketPriceApi` de MDS expone una ruta paginada de historial. `MarketPriceApiController` devuelve `X-Total-Count` y enlaces de paginación; `MarketPriceReadServiceImpl` delega en `MarketPriceCheckpointService.findCheckpoints`, y `MarketPriceReadHelper` construye la página filtrada y ordenada. Los checkpoints persistidos se capturan cada seis horas; los filtros actuales de historial usan límites inclusivos de `capturedAt`.
- El `MarketDataClient.getPriceHistory` interno ya acepta `from`, `to`, `offset` y `limit`, y lee el encabezado de conteo de MDS. MDS puede conservar paginación para consumidores internos de datos crudos mientras Portfolio presenta una respuesta completa orientada a gráficas.

Portfolio permite seleccionar hasta cinco carteras (`MAX_PORTFOLIOS_PER_USER`) o hasta 200 activos directamente (`MAX_ASSETS_PER_PORTFOLIO`); cada cartera puede contener hasta 200 activos. El uso habitual de gráficas discutido es bastante menor: unos 20 activos por cartera o alrededor de 100 activos entre cinco carteras. Aun así, el límite de 5.000 filas sigue siendo necesario para selecciones o historiales excepcionalmente grandes.

Los períodos habituales de la gráfica son un día, una semana, un mes, seis meses, un año y el máximo historial disponible. Los rangos largos no necesitan todos los checkpoints de seis horas. «Máximo» significa todo el historial disponible para los activos seleccionados, sin comenzar antes de su creación; no es una cantidad fija de años. Una serie dispersa debe utilizar checkpoints reales, no promedios ni precios interpolados o inventados.

## Problemas y restricciones

- Devolver todos los checkpoints de seis horas para rangos largos aumenta el tamaño de la respuesta sin aportar detalle útil a la gráfica. Con muchos activos, un checkpoint de símbolo compartido se expande a una fila por activo.
- La paginación de MDS es actualmente un mecanismo interno de lectura, no el contrato que necesita el frontend. Portfolio solicita hoy solo la primera página y responde correctamente `422` si está incompleta. No debe eliminarse esa comprobación ni tratarse la primera página como el historial completo.
- El límite de 5.000 se aplica tanto a checkpoints de MDS como a las filas de activos expandidas por Portfolio. El muestreo debe ocurrir antes de comprobar el límite expandido; este sigue siendo una protección final, no una razón para devolver datos parciales.
- Las consultas de MDS se basan en símbolos, pero las fechas de creación pertenecen a activos individuales de Portfolio. Portfolio debe seguir comprobando cada checkpoint contra el `createdAt` de cada activo; un límite inferior por símbolo no basta para validar un punto para todos los activos que comparten símbolo.
- El punto elegido para cada intervalo debe ser un checkpoint real. No se deben promediar precios ni generar puntos en límites artificiales de intervalo.
- Se conservan la forma pública de respuesta y las comprobaciones autenticadas de propiedad de activos y carteras. No se expone MDS directamente ni se agrega paginación pública para un frontend que consume la serie completa.
- No se impone un rango temporal máximo arbitrario. Solicitar fechas anteriores al activo no inventa precios: Portfolio limita el inicio y luego aplica el filtro individual por activo.
- El límite general de 5.000 filas sigue siendo pertinente: con unos 100 activos, el muestreo trimestral produce aproximadamente 400 filas por año, pero historiales más largos todavía pueden alcanzar el máximo. Con los límites máximos del repositorio —hasta cinco carteras y 200 activos por cartera— puede alcanzarse antes. En ambos casos se rechaza explícitamente; nunca se trunca en silencio.

## Opciones posibles

### Opción: Exigir fechas acotadas y conservar el límite actual de resultados

#### Enfoque

Exigir `from` y `to`, rechazar opcionalmente rangos superiores a una duración configurable y conservar el error actual de Portfolio cuando se superen 5.000 filas. MDS seguiría devolviendo checkpoints de seis horas.

#### Cambios esperados

Cambiar la validación de solicitud de `PortfolioMarketDataApi` y posiblemente añadir configuración de rango máximo y un error tipado. Los contratos de paginación de MDS y `MarketDataClient` podrían permanecer sin cambios.

#### Ventajas

- Es el cambio más pequeño sobre el flujo actual.
- Conserva el error explícito existente cuando la selección supera el límite de resultados.

#### Desventajas

- Consultas habituales como un año o el máximo historial todavía pueden fallar aunque la gráfica solo necesite una serie dispersa.
- Un límite temporal no garantiza menos de 5.000 filas, porque los resultados se expanden por activo.
- Contradice el comportamiento acordado de «todo el historial disponible» y obliga al cliente a escoger fechas artificiales.

### Opción: Paginación pública por ventanas temporales o filas

#### Enfoque

Exponer ventanas de fechas consecutivas o páginas de filas expandidas de Portfolio para que el consumidor las combine. MDS podría seguir usando su paginación actual para checkpoints crudos.

#### Cambios esperados

Cambiar el contrato de la API de Portfolio, la carga/composición del frontend y posiblemente la orquestación del cliente interno. La paginación por filas requeriría orden y cursor estables después de expandir; las ventanas necesitarían límites temporales inclusivos/exclusivos bien definidos.

#### Ventajas

- Permite recuperar historiales arbitrariamente grandes sin una única respuesta extensa.
- MDS ya ofrece lecturas paginadas de checkpoints crudos.

#### Desventajas

- El frontend no necesita paginación y ensamblar páginas o ventanas complica la carga de la gráfica.
- Las páginas MDS cuentan checkpoints por símbolo; Portfolio tendría que contar filas expandidas por activo. Las fechas de creación distintas impiden intercambiar ambos tipos de página.
- Añade complejidad de contrato y ordenación sin resolver la densidad innecesaria de seis horas.

### Opción: Reducir la resolución y devolver una respuesta completa de Portfolio — seleccionada

#### Enfoque

Elegir un intervalo de muestreo según la duración consultada. Para cada intervalo, conservar el último checkpoint real; Portfolio mantiene el filtro individual por fecha de creación y devuelve todos los resultados seleccionados con la forma de respuesta actual. El límite de 5.000 sigue siendo un error explícito de protección. «Máximo» usa puntos trimestrales cuando el historial supera dos años y no tiene un límite temporal fijo.

#### Cambios esperados

Añadir selección de resolución y muestreo durante la lectura MDS-Portfolio. La API pública de Portfolio continúa devolviendo una lista única. La API interna de MDS mantiene paginación para consumidores que necesitan datos crudos; la llamada de gráficas de Portfolio solicita la resolución seleccionada. No se requieren cambios de esquema: el muestreo se hace sobre checkpoints de seis horas ya almacenados.

#### Ventajas

- Se ajusta a la necesidad real de las gráficas y mantiene sencilla la API del frontend.
- Evita enviar puntos de seis horas para períodos largos y normalmente mantiene la respuesta por debajo del límite configurado.
- Conserva la separación actual: MDS es dueño de los checkpoints; Portfolio, de la autorización, composición y fronteras de creación de activos.
- Mantiene disponibles las lecturas paginadas crudas de MDS para otros consumidores internos.
- Usa precios reales y no requiere política de retención ni fecha máxima arbitraria.

#### Desventajas

- Añade selección de intervalo y muestreo al flujo de lectura, que necesita pruebas deterministas en los límites de rangos e intervalos.
- Portfolio debe seguir descartando un punto elegido que sea anterior a la creación de un activo posterior. Ese activo podría no tener punto en ese intervalo; nunca se debe devolver un punto previo a su creación para rellenarlo.
- El límite de 5.000 todavía puede rechazar selecciones o historiales excepcionales, intencionadamente.

## Solución elegida

### Decisión

Elegir muestreo con resolución adaptada para las consultas de gráficas de Portfolio y devolver una única respuesta completa y no paginada cuando el resultado esté dentro del límite configurado. Mantener el error explícito `422` si la página MDS está incompleta o el resultado final expandido por activo supera 5.000 filas; nunca truncar silenciosamente.

Política inicial de resolución, según la duración del historial solicitado/disponible:

| Duración del historial | Resolución |
| --- | --- |
| Hasta 1 semana | Checkpoints existentes cada 6 horas |
| Más de 1 semana y hasta 1 mes | Un punto cada 3 días |
| Más de 1 mes y hasta 6 meses | Semanal |
| Más de 6 meses y hasta 2 años | Mensual |
| Más de 2 años, incluido el máximo historial | Trimestral |

Para cada intervalo se usa el último checkpoint real. No se promedian ni interpolan precios ni se sintetizan puntos. «Máximo» comienza en la fecha de creación más antigua entre los activos seleccionados y no tiene una antigüedad máxima fija. Portfolio sigue aplicando a cada activo su propia frontera de creación después de recibir los checkpoints muestreados.

La carga habitual estimada de unos 100 activos genera aproximadamente 400 filas por año con resolución trimestral. Es una estimación, no un nuevo límite de API; se conserva la protección de 5.000 filas porque el repositorio permite selecciones e historiales mayores.

Esta opción se prefiere a limitar fechas porque el producto necesita el máximo historial y la duración por sí sola no limita las filas expandidas. Se prefiere a paginar porque el frontend quiere una serie completa y las páginas de checkpoints de MDS no corresponden a las filas expandidas por activo de Portfolio. Extiende proporcionalmente la separación existente: MDS lee/muestrea checkpoints; Portfolio autoriza y compone filas por activo.

### Diseño detallado

#### Clases existentes que cambiarían

**`PortfolioMarketDataApi`**  
Ruta: `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioMarketDataApi.java`

Define actualmente la solicitud autenticada de historial para el frontend, con `from`/`to` opcionales y una lista como respuesta. Mantener la respuesta pública sin paginación. La política de duración puede requerir aclarar si el rango se proporciona explícitamente o mediante un preset existente; no añadir parámetros públicos de página.

**`PortfolioMarketDataServiceImpl`**  
Ruta: `services/portfolio-service/src/main/java/com/tychewealth/service/impl/PortfolioMarketDataServiceImpl.java`

Actualmente resuelve activos autorizados, limita el inicio efectivo, llama a `MarketDataClient` con offset cero y el máximo configurado, y valida que la respuesta esté completa y dentro del máximo expandido. Determinará o delegará la resolución solicitada, llamará la lectura interna de MDS con esa resolución y conservará ambas comprobaciones. Las validaciones de propiedad y la obtención del token no cambian.

**`PortfolioMarketDataHelper`**  
Ruta: `services/portfolio-service/src/main/java/com/tychewealth/service/helper/marketdata/PortfolioMarketDataHelper.java`

Calcula actualmente el inicio efectivo, expande checkpoints de símbolos a activos y filtra mediante `checkpointAppliesToAsset` usando el `createdAt` del activo. Mantendrá esas responsabilidades y aplicará el mismo filtro a los puntos muestreados. La respuesta usa el `capturedAt` real del checkpoint seleccionado, no el límite del intervalo.

**`MarketDataClient`**  
Ruta: `clients/market-data-client/src/main/java/com/tychewealth/marketdataclient/MarketDataClient.java`

Llama actualmente al endpoint interno paginado `/internal/prices/history` con filtros temporales inclusivos y `offset`/`limit`. Extender la solicitud interna solo si hace falta para transmitir la resolución seleccionada. Mantener disponible la paginación cruda para otros consumidores; no trasladar sus semánticas al endpoint público de Portfolio.

**`MarketPriceApi` y `MarketPriceApiController`**  
Rutas: `services/market-data-service/src/main/java/com/tychewealth/controller/MarketPriceApi.java`, `services/market-data-service/src/main/java/com/tychewealth/controller/impl/MarketPriceApiController.java`

Definen y devuelven actualmente lecturas paginadas de checkpoints de seis horas, incluidos encabezados de paginación. Añadir o enrutar una modalidad interna con resolución para Portfolio, conservando la lectura paginada cruda existente. El controlador seguirá informando el total del resultado consultado para que Portfolio detecte respuestas incompletas; los metadatos no deben describir resultados muestreados como páginas de checkpoints crudos.

**`MarketPriceReadServiceImpl` y `MarketPriceReadHelper`**  
Rutas: `services/market-data-service/src/main/java/com/tychewealth/service/impl/MarketPriceReadServiceImpl.java`, `services/market-data-service/src/main/java/com/tychewealth/service/helper/MarketPriceReadHelper.java`

El servicio ejecuta actualmente una consulta paginada al repositorio y mapea las entidades. Añadir la selección en tiempo de consulta del último checkpoint por intervalo para la modalidad de gráficas, preservando validación de filtros, selección de símbolos, ordenación y lectura paginada cruda. El muestreo debe ocurrir antes de limitar la página: agrupar/ordenar los checkpoints coincidentes por símbolo e intervalo UTC, conservar el último checkpoint real de cada grupo y después paginar e informar el total muestreado. Muestrear solo la primera página cruda seguiría omitiendo datos y Portfolio la rechazaría. El muestreo debe ser determinista en UTC y coherente con los buckets UTC persistidos.

**`PortfolioMarketDataValidationHelper`**  
Ruta: `services/portfolio-service/src/main/java/com/tychewealth/service/helper/marketdata/PortfolioMarketDataValidationHelper.java`

Conservar la validación de respuesta MDS completa y el máximo de `app.market-data.max-results` para las filas expandidas. No convertir ninguna de estas condiciones en una respuesta parcial exitosa. Ajustar pruebas o detalles de error solo si el muestreo modifica el significado de `totalCount`.

#### Clases nuevas

El diseño elegido no requiere crear clases ni interfaces. La selección de resolución y el muestreo deberían permanecer inicialmente en el flujo de lectura/orquestación existente. Solo se justificaría una abstracción dedicada si la implementación demuestra que varios consumidores distintos comparten el comportamiento o que resulta difícil probarlo en su ubicación actual.

#### Interacción y flujo de componentes

1. `PortfolioMarketDataApi` recibe la selección de activos/carteras del usuario y límites temporales opcionales; no se añade paginación pública.
2. `PortfolioMarketDataServiceImpl` valida la solicitud y resuelve solo activos vigentes propiedad del usuario autenticado.
3. Portfolio calcula el inicio efectivo, limitado por la fecha de creación más antigua de los activos seleccionados, y elige la resolución según la duración.
4. `MarketDataClient` llama la ruta interna de historial MDS con símbolos, fechas, resolución, parámetros de paginación interna y autenticación existentes.
5. `MarketPriceReadServiceImpl` y `MarketPriceReadHelper` seleccionan el último checkpoint real de cada intervalo en la consulta de gráficas. La lectura paginada cruda de MDS continúa disponible para consumidores que necesitan checkpoints sin muestreo.
6. Portfolio verifica que la respuesta MDS esté completa, aplica las fronteras `createdAt` por activo y `from` solicitado, y expande cada checkpoint de símbolo a filas para los activos correspondientes.
7. Portfolio devuelve la lista completa si está dentro de `app.market-data.max-results`; de lo contrario devuelve el error tipado `422` existente. Nunca devuelve una lista truncada como exitosa.

#### Esquema de implementación

1. Definir selector de resolución y semántica exacta de intervalos/buckets UTC, incluidos `to` abierto y «máximo».
2. Extender el contrato interno MDS/`MarketDataClient` para consultas con resolución sin eliminar la paginación cruda.
3. Implementar selección determinista del último checkpoint real por intervalo en la lectura MDS existente.
4. Mantener propiedad, inicio efectivo, filtro individual por creación, validación del conteo expandido y DTO de respuesta de Portfolio, salvo que las pruebas exijan un ajuste.
5. Añadir pruebas de servicio/cliente/controlador e integración Portfolio para cada resolución, límites de rangos, activos con mismo símbolo y distinta creación, checkpoints anteriores a creación, historial vacío, páginas MDS incompletas y error de 5.000 filas.
6. Actualizar la documentación API generada si cambia el contrato público; no se prevé migración de persistencia.

## Preguntas abiertas

No queda ninguna decisión de producto pendiente: la respuesta será completa y no paginada, los períodos largos se muestrean, el máximo historial no tiene fecha límite, y se conservan las fronteras de creación y los errores explícitos por límite. La implementación debe concretar el anclaje exacto de buckets UTC y la representación de la resolución en la solicitud interna MDS; son detalles del contrato de implementación, no decisiones arquitectónicas abiertas.
