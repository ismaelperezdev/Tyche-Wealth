# Spike: Valoración de mercado de carteras

English version: [portfolio-market-valuation-spike.en.md](portfolio-market-valuation-spike.en.md).

## Investigación

Este spike registra la dirección acordada para calcular la evolución histórica de la ganancia o pérdida no realizada de una cartera. No representa el valor bruto de mercado de las posiciones. Para cada punto se usan la cantidad y el `averagePrice` vigentes en ese instante junto con el precio de mercado; los importes se expresan en la moneda base de la cartera. `AssetEntity.averagePrice` sigue representando el coste medio de adquisición, denominado en `AssetEntity.currency`, y no se modifica. Las decisiones de alcance, cálculo, monedas, datos ausentes y ruta quedan cerradas en este spike; los detalles restantes son de implementación.

La API pública actual de datos de mercado es `PortfolioMarketDataApi`, en `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioMarketDataApi.java`. Expone lecturas de últimas cotizaciones e historial. `PortfolioMarketDataServiceImpl` valida la selección, resuelve mediante `PortfolioRepository` y `AssetRepository` los activos vigentes propiedad del usuario autenticado, deduplica símbolos y consulta MDS a través de `MarketDataClient`.

Portfolio es el lugar adecuado para componer la valoración: es propietario de la cartera, las cantidades, las comprobaciones de propiedad y `PortfolioEntity.baseCurrency`. MDS es propietario de las observaciones de mercado. El flujo síncrono Portfolio → MDS y la autenticación entre servicios existentes ofrecen una vía natural para obtenerlas. MDS no debe exponerse directamente al frontend.

Actualmente, MDS guarda cotizaciones recientes en Redis mediante `MarketQuoteStore`, con un TTL de diez minutos, y persiste checkpoints periódicos de seis horas. `MarketQuoteService` captura y almacena cotizaciones; `MarketPriceReadServiceImpl` lee cotizaciones recientes de Redis e historial de checkpoints persistidos. Por acuerdo, los precios de mercado y checkpoints de MDS se interpretan siempre en EUR. Esta unidad forma parte del contrato semántico y no requiere un campo de moneda en sus DTO.

Si no hay una cotización reciente, la valoración debe usar el checkpoint persistido más reciente y conservar su fecha real. Si para un punto en que la cartera tenía una posición no existe ni cotización ni checkpoint, falla toda la petición. `AssetEntity.averagePrice` no se usa para sustituir un precio de mercado ausente.

Una cartera puede contener activos en distintas monedas. Los precios de mercado se convierten desde EUR a `PortfolioEntity.baseCurrency`. Frankfurter es la fuente FX y cubre todas las monedas disponibles en el producto. MDS consulta tasas diarias en lote, las persiste y ofrece una lectura interna en lote; Portfolio no mantiene una segunda caché FX. En toda la serie histórica se usa la tasa FX más reciente disponible al calcular la respuesta, igual para todos los puntos, no una tasa histórica distinta por fecha. Si no hay una tasa disponible para algún par requerido, la petición falla por completo; no se inventa una tasa 1:1 ni se devuelve una gráfica parcial. Se conserva la fecha efectiva de la tasa aplicada.

El alcance es una serie histórica de la ganancia o pérdida no realizada desde una fecha de inicio hasta la actualidad. Portfolio reconstruye cantidad y coste medio para cada fecha con los eventos de `AssetVariationEntity` y combina esos datos con los precios de los endpoints `historic` y `latest` de MDS: historial para puntos pasados y latest para el punto actual. Para cada punto, la ganancia/pérdida por activo es `cantidad × (precio de mercado convertido a moneda base − averagePrice convertido a moneda base)`. Portfolio devuelve por punto el importe total de ganancia/pérdida no realizada y el coste de adquisición total (`suma de cantidad × averagePrice`, convertido a moneda base), ambos en moneda base; el frontend deriva el porcentaje como ganancia/pérdida dividida por coste de adquisición. Si el coste total es cero, no se puede calcular el porcentaje. El porcentaje no se persiste ni lo calcula Portfolio. El valor bruto de mercado (`cantidad × precio`) no es la métrica de esta gráfica. Para fechas y muestreo, se sigue el estilo de Portfolio `/history`: parámetros `from` y `to` opcionales de tipo `Instant`, inicio efectivo no anterior a la creación del activo y resolución/muestreo existente de MDS. Todos los puntos usan las tasas FX más recientes disponibles al generar la serie, tanto para el precio como para el coste medio. Las variaciones se aplican según su `occurredAt`: en cada punto rige el último estado ocurrido en ese instante o antes. Esta decisión no selecciona Twelve Data como proveedor de precios.

Para reconstruir el rango, Portfolio necesita el último estado de cada activo ocurrido en o antes de `from` como estado inicial, y las variaciones posteriores hasta `to`. Solo se incluyen activos que tuvieron una posición dentro del rango, incluidos los que se borraron después; quedan fuera los creados después de `to` y los borrados antes de `from`. La lectura de variaciones no filtra por `AssetEntity.deletedAt`, pero siempre queda acotada al `portfolioId` cuya propiedad ya validó Portfolio.

## Problemas y restricciones

- **Autorización:** La valoración debe utilizar la frontera autenticada de Portfolio y validar que `{portfolioId}` pertenece al usuario. La consulta histórica incluye activos actualmente borrados que tuvieron posición en el intervalo; no debe restringirse al selector de activos vigentes usado por las lecturas de mercado actuales. Las llamadas a MDS mantienen la autenticación existente entre servicios.
- **Monedas distintas:** Los precios de mercado de MDS están denominados en EUR y se convierten a `baseCurrency` de la cartera correspondiente.
- **El coste base no es precio de mercado:** `averagePrice` es el coste medio usado para calcular ganancia/pérdida no realizada; no se presenta como cotización ni sustituye un precio de mercado ausente.
- **Expiración de Redis:** La ausencia de una cotización Redis no demuestra que nunca haya existido un dato de mercado. MDS debe consultar los checkpoints persistidos; si tampoco existe un precio para una posición en ese punto, falla toda la petición.
- **Observaciones obsoletas:** El fallback a checkpoint es una observación real, aunque potencialmente atrasada. Se conserva su fecha de captura y se distingue de una cotización reciente.
- **Datos requeridos ausentes:** Si, tras aplicar los fallbacks permitidos, falta un precio o una tasa FX requerida, la petición de valoración falla por completo. No se sustituye por cero ni por una tasa inventada, y no se devuelve una gráfica parcial.
- **Fechas FX:** Las tasas de Frankfurter son diarias. Para toda la serie se aplica la tasa más reciente disponible al generar la respuesta, igual para todos los puntos, y se conserva su fecha efectiva. No se reconstruye FX histórico por fecha de punto ni se implica precisión intradía.
- **Unidad de precios MDS:** Las cotizaciones y checkpoints MDS se consideran EUR por contrato; no es necesario incluir la moneda en esos DTO. Los datos FX requieren persistencia y una migración Liquibase en MDS.
- **Lotes:** Solicitar símbolos y pares de divisas distintos en lote. Evitar llamadas a MDS o FX por cada activo.
- **Sin efectos de escritura:** La valoración no modifica activos, `averagePrice` ni estado de la cartera.
- **Sin exposición pública de MDS:** El frontend sigue llamando a Portfolio, no a MDS ni a la API de Frankfurter.

## Opciones posibles

### Opción: Utilizar únicamente cotizaciones recientes

#### Enfoque

Calcular el valor de un activo solo cuando haya una cotización reciente de MDS. Convertirla a la moneda de la cartera y marcar la valoración como incompleta si falta una cotización o conversión requerida.

#### Cambios esperados

Ampliar la operación y los DTO de valoración de Portfolio, y añadir las lecturas por lote necesarias en `MarketDataClient`. Los precios de mercado MDS se interpretan en EUR por contrato; no se necesita añadir un campo de moneda ni cambiar la persistencia de activos.

#### Ventajas

- Los valores se basan únicamente en observaciones de mercado.
- Mantiene separado el coste de adquisición del precio de mercado.

#### Desventajas

- Si aún no hay cotización ni checkpoint para una posición, la valoración falla hasta que MDS disponga de una observación.
- No utiliza un checkpoint persistido cuando la cotización reciente de Redis no está disponible.

### Opción: Utilizar el coste de adquisición cuando falten datos de mercado

#### Enfoque

Usar `quantity × averagePrice` como fallback general cuando no haya cotización.

#### Cambios esperados

Portfolio podría calcular un fallback sin consultar los checkpoints de MDS, pero la respuesta tendría que identificar el origen del valor.

#### Ventajas

- Proporciona valor para más activos cuando faltan datos de mercado.

#### Desventajas

- Trata el coste de adquisición como sustituto de los datos de mercado actuales y puede ocultar datos obsoletos o fallos del proveedor.
- Confunde el coste de adquisición con el valor de mercado y puede generar una gráfica engañosa.

### Opción: Priorizar cotización reciente y luego checkpoint; fallar si falta el precio — seleccionada

#### Enfoque

Para cada punto, reconstruir cantidad y `averagePrice` aplicando las variaciones ocurridas hasta ese instante. Usar precios MDS de `historic` para puntos pasados y `latest` para el actual; conservar la fecha real y usar checkpoint según corresponda. Calcular la ganancia/pérdida no realizada como `cantidad × (precio de mercado − averagePrice)`, convirtiendo ambos precios a moneda base con FX actual. Si falta un precio de mercado o una tasa requerida, falla toda la petición; `averagePrice` no sustituye al precio.

#### Cambios esperados

Ampliar las lecturas MDS para admitir el fallback a checkpoint. Los precios y checkpoints se interpretan en EUR por contrato, sin añadir un campo de moneda. Añadir ingestión, persistencia y lecturas internas en lote de tasas FX en MDS. Ampliar `MarketDataClient`, la orquestación de valoración de Portfolio y sus DTO de respuesta. No cambiar el significado ni el comportamiento de actualización de `averagePrice`.

#### Ventajas

- Distingue cotizaciones recientes de observaciones atrasadas sin mezclar el coste de adquisición con el precio de mercado.
- Mantiene a MDS responsable de observaciones y tasas, y a Portfolio responsable de autorización y composición de la valoración.

#### Desventajas

- Si MDS aún no dispone de una observación para una posición histórica, la gráfica no puede generarse hasta que exista.
- Requiere persistir tasas FX y coordinar cambios en contratos internos.
- La petición falla si falta un dato de mercado o FX requerido después de aplicar los fallbacks acordados; no se devuelve una gráfica parcial.

### Opción: Consultar Frankfurter directamente desde Portfolio o mantener una segunda caché FX

#### Enfoque

Portfolio podría llamar a Frankfurter durante la valoración o mantener una caché FX local junto a las tasas persistidas por MDS.

#### Cambios esperados

Las llamadas directas añadirían configuración del proveedor, manejo de red y preocupaciones de disponibilidad a Portfolio. Una segunda caché duplicaría el almacenamiento FX y su ciclo de vida.

#### Ventajas

- Las llamadas directas podrían eliminar la dependencia de MDS para lecturas FX.
- Una caché local podría reducir la dependencia de Portfolio respecto a la disponibilidad de lecturas FX de MDS.

#### Desventajas

- Las llamadas directas acoplan Portfolio a un proveedor externo y al comportamiento de sus respuestas.
- Las cachés duplicadas crean semánticas potencialmente divergentes de frescura y fecha efectiva, y duplican la responsabilidad de ingestión.

## Solución elegida

### Decisión

Adoptar el flujo cotización → checkpoint, sin estimación basada en el coste, con MDS como propietario de las observaciones de mercado y las tasas FX de referencia. Portfolio sigue siendo la frontera autenticada que compone la valoración y solicita en lote las observaciones y tasas necesarias.

Reconstruir cantidad y `averagePrice` vigentes en cada punto desde `AssetVariationEntity`, aplicando las variaciones con `occurredAt` menor o igual al timestamp del punto. Obtener precios pasados de `historic` y el actual de `latest`, conservando sus timestamps y usando el checkpoint aplicable según el comportamiento de MDS. Calcular por activo `cantidad × (precio de mercado − averagePrice)`, convirtiendo ambos importes a `PortfolioEntity.baseCurrency` mediante la tasa FX más reciente al generar la serie. Si falta precio de mercado o una tasa FX requerida, fallar toda la petición; no devolver una serie parcial ni sustituir el precio por `averagePrice`.

Si falta un dato de mercado o FX requerido después de aplicar los fallbacks acordados, la petición falla y no devuelve una valoración parcial. No se inventan tasas ni se modifica `AssetEntity.averagePrice`.

Esta decisión encaja con el flujo existente Portfolio → MDS mediante cliente y autenticación entre servicios, mantiene la propiedad de datos alineada con las fronteras actuales y evita exponer detalles del proveedor o mantener una segunda caché FX. Si faltan datos, se prefiere un error explícito a una gráfica parcial que pueda interpretarse como válida.

### Diseño detallado

#### Clases existentes que cambiarían

**`PortfolioApi`**  
Ruta: `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioApi.java`

Añadir aquí la operación autenticada `GET /api/v1/portfolio/me/{portfolioId}/valuation`, con `from` y `to` como únicos parámetros de consulta opcionales de tipo `Instant`. Portfolio resuelve automáticamente los activos y variaciones de esa cartera y valida que pertenece al usuario autenticado; el cliente no envía IDs de activos. No exponer directamente un endpoint MDS.

**`PortfolioApiController`**  
Ruta: `services/portfolio-service/src/main/java/com/tychewealth/controller/impl/PortfolioApiController.java`

Implementar aquí la entrada HTTP de valoración y delegar su ejecución al servicio de Portfolio correspondiente, manteniendo la convención de controladores delgados. Las rutas existentes de `PortfolioMarketDataApi` no cambian.

**`PortfolioMarketDataService` y `PortfolioMarketDataServiceImpl`**  
Rutas: `services/portfolio-service/src/main/java/com/tychewealth/service/PortfolioMarketDataService.java`, `services/portfolio-service/src/main/java/com/tychewealth/service/impl/PortfolioMarketDataServiceImpl.java`

La lógica de valoración recupera la cartera ya validada por `PortfolioApi`, carga las variaciones históricas —incluidas las de activos borrados— y consulta a MDS las observaciones `historic` y `latest` y los pares FX necesarios en lotes. Calcula ganancia/pérdida no realizada y coste de adquisición por punto, y falla con un error tipado si falta un dato requerido. Mantener el flujo del token de servicio y no escribir datos de activos.

**`PortfolioMarketDataHelper` y DTO de respuesta de valoración**  
Rutas: `services/portfolio-service/src/main/java/com/tychewealth/service/helper/marketdata/PortfolioMarketDataHelper.java`, `services/portfolio-service/src/main/java/com/tychewealth/dto/marketdata/`

El helper adapta actualmente resultados de precios MDS y los expande a activos. Ampliarlo o añadir un helper acotado si hace falta para mapear y calcular la valoración. La respuesta debe distinguir cotización de checkpoint y conservar las fechas de observación y vigencia FX. Si falta un valor después de aplicar los fallbacks, el servicio lanza un error tipado; el contrato de ese error queda por cerrar.

**`MarketDataClient`**  
Ruta: `clients/market-data-client/src/main/java/com/tychewealth/marketdataclient/MarketDataClient.java`

Este wrapper concreto de `RestClient` lee actualmente últimas cotizaciones e historial. Añadir lecturas internas autenticadas en lote de observaciones de mercado y tasas FX, usando el patrón existente de bearer token de servicio. Mantener el contrato público de Portfolio independiente de las rutas MDS y los formatos del proveedor.

**Modelo y lectura de cotizaciones MDS**  
Las rutas relevantes incluyen:

- `services/market-data-service/src/main/java/com/tychewealth/model/MarketQuote.java`
- `services/market-data-service/src/main/java/com/tychewealth/dto/marketprice/LatestPriceResponseDto.java`
- `services/market-data-service/src/main/java/com/tychewealth/service/MarketQuoteService.java`
- `services/market-data-service/src/main/java/com/tychewealth/store/MarketQuoteStore.java`
- `services/market-data-service/src/main/java/com/tychewealth/service/impl/MarketPriceReadServiceImpl.java`

Las cotizaciones y checkpoints contienen precio y fecha de captura; su unidad es EUR por contrato, no por un campo del DTO. Las cotizaciones recientes se leen de Redis; los checkpoints se persisten en PostgreSQL. Proporcionar la observación persistida más reciente cuando no haya una cotización Redis reciente. Conservar su fecha real y distinguirla de una cotización reciente. No alterar el comportamiento establecido del historial/checkpoints de seis horas.

**Ingestión, persistencia y lectura interna FX de MDS**  
Las rutas relevantes incluyen `services/market-data-service/src/main/java/com/tychewealth/` y `services/market-data-service/src/main/resources/db.changelog/changelog-master.xml`.

Añadir recuperación diaria en lote desde Frankfurter, persistencia de tasas con sus monedas y fecha efectiva, y lectura interna autenticada en lote. Añadir el modelo, repositorio, componentes de proveedor/servicio, configuración, changeset Liquibase y pruebas necesarios siguiendo las convenciones MDS. MDS es propietario de la recuperación y lectura de tasas persistidas; Portfolio no persiste ni mantiene una segunda copia en caché.

**`AssetEntity` y `PortfolioEntity`**  
Rutas: `services/portfolio-service/src/main/java/com/tychewealth/entity/AssetEntity.java`, `services/portfolio-service/src/main/java/com/tychewealth/entity/PortfolioEntity.java`

No se añade responsabilidad de escritura por la valoración. `AssetEntity.currency` sigue siendo la moneda de origen de `averagePrice`; `PortfolioEntity.baseCurrency` sigue siendo la moneda de destino. Las consultas existentes de activos vigentes y propiedad siguen siendo autoritativas.

#### Clases nuevas que se crearían

Esta decisión no requiere una estrategia, fábrica o módulo de valoración genérico. Añadir únicamente los componentes MDS concretos necesarios para recuperar, persistir y leer FX, siguiendo los patrones existentes de servicios, repositorios y Liquibase.

Un componente de valoración separado en Portfolio es opcional si la implementación demuestra que incorporar el cálculo a la frontera actual de orquestación/helper perjudica materialmente la claridad. No se requiere de antemano.

#### Interacción y flujo de componentes

1. El usuario autenticado llama a la operación de Portfolio para obtener la serie de ganancia/pérdida no realizada usando el modelo existente de selección de carteras/activos.
2. Portfolio valida la solicitud y resuelve únicamente activos vigentes pertenecientes al usuario, incluida la `baseCurrency` de cada cartera correspondiente.
3. Portfolio reconstruye cantidad y `averagePrice` de cada activo en cada fecha aplicando cronológicamente `AssetVariationEntity`. Para puntos pasados solicita precios a MDS `historic` y usa `latest` para el punto actual; conserva timestamps reales.
4. Para cada punto y activo con posición, calcula `cantidad × (precio de mercado − averagePrice)`. Si falta el precio de mercado, falla toda la petición; el coste medio no es un fallback.
5. Portfolio reúne en lote los pares FX necesarios para convertir los precios EUR y el coste medio en `AssetEntity.currency` a la moneda base. Usa la tasa más reciente al generar la serie para todos los puntos; la conversión a la misma moneda no requiere tasa externa.
6. Portfolio suma la ganancia/pérdida no realizada por activo en moneda base. Si falta una tasa requerida, falla toda la petición. No devuelve una serie parcial ni actualiza activos.
7. Portfolio devuelve la serie mediante su API autenticada, con fechas de punto, origen/timestamp de precio y fecha efectiva FX. Los datos requeridos ausentes se exponen mediante un error tipado.

#### Esquema de implementación

1. Implementar `GET /api/v1/portfolio/me/{portfolioId}/valuation?from=...&to=...`, el esquema de respuesta y el error tipado por datos requeridos ausentes, siguiendo las convenciones API y de errores existentes.
2. Definir cómo MDS informa una cotización reciente frente al checkpoint aplicable más reciente para cada punto histórico.
3. Aplicar la convención de que los precios y checkpoints MDS están denominados en EUR; añadir en MDS la ingestión diaria Frankfurter en lote, persistencia, lectura interna en lote, migración Liquibase y pruebas.
4. Ampliar `MarketDataClient` con lecturas autenticadas en lote de observaciones y tasas.
5. Añadir la orquestación de valoración en Portfolio y la conversión de monedas en lote, gestionando explícitamente los errores por datos requeridos ausentes y sin modificar `averagePrice`.
6. Probar propiedad y filtrado de activos vigentes; varias monedas; cotización, checkpoint y ausencia de datos; fechas efectivas FX y tasas faltantes; conversión a la misma moneda; y error sin respuesta parcial. Actualizar la documentación API generada si cambia el contrato público.

## Estado del spike

Las decisiones de diseño están cerradas. Durante la implementación se concretarán los nombres exactos de los campos JSON y el mapeo HTTP del error tipado, reutilizando el contrato de errores y `ErrorHandler` existentes; no son bloqueos para cerrar este spike.
