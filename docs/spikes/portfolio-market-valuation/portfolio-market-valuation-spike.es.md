# Spike: Valoración de mercado de carteras

English version: [portfolio-market-valuation-spike.en.md](portfolio-market-valuation-spike.en.md).

## Investigación

Este spike registra la dirección acordada para calcular la valoración de mercado actual de una cartera de un usuario. El cálculo utiliza activos vigentes y sus cantidades, expresa los importes en la moneda base de la cartera y no modifica `AssetEntity.averagePrice`. Ese campo sigue representando el coste medio de adquisición del activo, denominado en `AssetEntity.currency`.

La API pública actual de datos de mercado es `PortfolioMarketDataApi`, en `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioMarketDataApi.java`. Expone lecturas de últimas cotizaciones e historial. `PortfolioMarketDataServiceImpl` valida la selección, resuelve mediante `PortfolioRepository` y `AssetRepository` los activos vigentes propiedad del usuario autenticado, deduplica símbolos y consulta MDS a través de `MarketDataClient`.

Portfolio es el lugar adecuado para componer la valoración: es propietario de la cartera, las cantidades, las comprobaciones de propiedad y `PortfolioEntity.baseCurrency`. MDS es propietario de las observaciones de mercado. El flujo síncrono Portfolio → MDS y la autenticación entre servicios existentes ofrecen una vía natural para obtenerlas. MDS no debe exponerse directamente al frontend.

Actualmente, MDS guarda cotizaciones recientes en Redis mediante `MarketQuoteStore`, con un TTL de diez minutos, y persiste checkpoints periódicos de seis horas. `MarketQuoteService` captura y almacena cotizaciones; `MarketPriceReadServiceImpl` lee cotizaciones recientes de Redis e historial de checkpoints persistidos. `MarketQuote` y el DTO actual de última cotización contienen símbolo, precio y fecha de captura, pero no divisa. Para este flujo se asume inicialmente que MDS informa precios en EUR; la divisa de cotización debe explicitarse en el contrato interno.

Si no hay una cotización reciente, la valoración debe usar el checkpoint persistido más reciente y conservar su fecha real. Solo durante el arranque inicial de los datos de mercado, si no existe ni cotización ni checkpoint, se permite `quantity × averagePrice` como estimación identificada explícitamente, no como cotización. En los demás casos sin precio, el valor del activo queda ausente.

Una cartera puede contener activos en distintas monedas. Tanto los precios de mercado como las estimaciones basadas en el coste de adquisición se convierten desde sus monedas de origen a `PortfolioEntity.baseCurrency`. Se propone Frankfurter como fuente FX: MDS consulta tasas diarias en lote, las persiste y ofrece una lectura interna en lote. Portfolio solicita a MDS las tasas que necesita al calcular la valoración y no mantiene una segunda caché FX. Se utiliza la fecha efectiva devuelta con cada tasa. No se inventa una tasa 1:1 ni se devuelve un total aparentemente completo si falta un precio o una conversión necesaria.

Este spike trata la valoración actual, no la reconstrucción histórica de carteras. El comportamiento del historial de precios y el muestreo de checkpoints siguen regidos por los spikes de historial existentes. Esta decisión no selecciona Twelve Data como proveedor de precios, y la propuesta Frankfurter es independiente de esa elección.

## Problemas y restricciones

- **Autorización:** La valoración debe utilizar la frontera autenticada de Portfolio y resolver únicamente activos vigentes propiedad del usuario. No debe eludir las comprobaciones de propiedad de `PortfolioMarketDataServiceImpl` ni la autenticación existente entre servicios para llamar a MDS.
- **Monedas distintas:** El precio de mercado de cada activo está denominado en la moneda de cotización; la estimación de coste está denominada en `AssetEntity.currency`. Ambos se convierten a `baseCurrency` de la cartera correspondiente.
- **El coste base no es precio de mercado:** No se debe sobrescribir ni presentar `averagePrice` como cotización. Su uso como estimación se limita al arranque inicial de datos de mercado.
- **Expiración de Redis:** La ausencia de una cotización Redis no demuestra que nunca haya existido un dato de mercado. MDS debe consultar los checkpoints persistidos antes de permitir la estimación de bootstrap.
- **Observaciones obsoletas:** El fallback a checkpoint es una observación real, aunque potencialmente atrasada. Se conserva su fecha de captura y se distingue de una cotización reciente.
- **Resultados incompletos:** La ausencia de precios o tasas FX debe representarse y reflejarse en un indicador global de completitud. No se sustituye por cero ni por una tasa inventada, ni se presenta una suma parcial como valoración completa.
- **Fechas FX:** Las tasas de Frankfurter son diarias; su fecha efectiva puede diferir de la fecha de valoración. La respuesta conserva la fecha y no implica precisión intradía de FX.
- **Contratos actuales:** Los DTO de cotizaciones y checkpoints MDS no incluyen divisa. La hipótesis inicial de precios en EUR debe explicitarse en el contrato relacionado con la valoración. Los datos FX requieren persistencia y una migración Liquibase en MDS.
- **Lotes:** Solicitar símbolos y pares de divisas distintos en lote. Evitar llamadas a MDS o FX por cada activo.
- **Sin efectos de escritura:** La valoración no modifica activos, `averagePrice` ni estado de la cartera.
- **Sin exposición pública de MDS:** El frontend sigue llamando a Portfolio, no a MDS ni a la API de Frankfurter.

## Opciones posibles

### Opción: Utilizar únicamente cotizaciones recientes

#### Enfoque

Calcular el valor de un activo solo cuando haya una cotización reciente de MDS. Convertirla a la moneda de la cartera y marcar la valoración como incompleta si falta una cotización o conversión requerida.

#### Cambios esperados

Ampliar la operación y los DTO de valoración de Portfolio. Ampliar los contratos de MDS y `MarketDataClient` para incluir la divisa de cotización y lecturas por lote. No se necesita cambiar la persistencia de activos.

#### Ventajas

- Los valores se basan únicamente en observaciones de mercado.
- Mantiene separado el coste de adquisición del precio de mercado.

#### Desventajas

- Durante el bootstrap inicial de MDS, un activo podría no tener valor hasta recibir su primera cotización.
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
- Contradice la restricción acordada de permitir esta estimación únicamente durante el bootstrap inicial.

### Opción: Priorizar cotización reciente, luego checkpoint y finalmente estimación solo durante bootstrap — seleccionada

#### Enfoque

Para cada símbolo, utilizar una cotización reciente MDS/Redis si existe; de lo contrario, usar el checkpoint persistido más reciente con su fecha de captura. Solo si no existe ninguno durante el bootstrap inicial puede Portfolio usar `quantity × averagePrice` como estimación identificada explícitamente. Fuera de esa condición, dejar ausente el valor del activo. Convertir los valores disponibles a la moneda base de la cartera y marcar la respuesta incompleta si faltan datos requeridos.

#### Cambios esperados

Ampliar los contratos y lecturas de observaciones MDS para admitir la divisa de cotización y el fallback a checkpoint. Añadir ingestión, persistencia y lecturas internas en lote de tasas FX en MDS. Ampliar `MarketDataClient`, la orquestación de valoración de Portfolio y sus DTO de respuesta. No cambiar el significado ni el comportamiento de actualización de `averagePrice`.

#### Ventajas

- Distingue cotizaciones recientes, observaciones atrasadas y estimaciones basadas en el coste.
- Permite responder durante el bootstrap sin presentar una estimación como cotización de mercado.
- Mantiene a MDS responsable de observaciones y tasas, y a Portfolio responsable de autorización y composición de la valoración.

#### Desventajas

- Requiere definir de forma fiable el bootstrap y añadir metadatos de respuesta para cada origen del valor.
- Requiere persistir tasas FX y coordinar cambios en contratos internos.
- La valoración puede quedar incompleta si faltan datos de mercado o FX; los clientes deben tratarlo explícitamente.

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

Adoptar el flujo cotización → checkpoint → estimación solo durante bootstrap, con MDS como propietario de las observaciones de mercado y las tasas FX de referencia. Portfolio sigue siendo la frontera autenticada que compone la valoración y solicita en lote las observaciones y tasas necesarias.

Priorizar una cotización reciente y después el checkpoint persistido más reciente con su fecha real de captura. Usar `quantity × averagePrice` solo cuando no exista observación de mercado durante el bootstrap inicial, identificándolo como estimación. En otro caso, dejar ausente el valor del activo. Convertir precios de mercado y costes de adquisición desde sus respectivas monedas de origen a `PortfolioEntity.baseCurrency`, usando tasas Frankfurter persistidas y sus fechas efectivas.

La falta de datos de mercado o FX requeridos hace que la valoración sea incompleta. No se inventan tasas ni se presenta un total incompleto como completo. No se modifica `AssetEntity.averagePrice`.

Esta decisión encaja con el flujo existente Portfolio → MDS mediante cliente y autenticación entre servicios, mantiene la propiedad de datos alineada con las fronteras actuales y evita exponer detalles del proveedor o mantener una segunda caché FX. El compromiso aceptado es una respuesta más rica y explícitamente incompleta cuando corresponda, en lugar de un total engañoso.

### Diseño detallado

#### Clases existentes que cambiarían

**`PortfolioMarketDataApi`**  
Ruta: `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioMarketDataApi.java`

Actualmente define operaciones autenticadas de últimas cotizaciones e historial. Añadir una operación pública de valoración en la frontera de Portfolio, conservando el usuario autenticado y el modelo de selección existente. La ruta exacta y el contrato JSON quedan pendientes. No exponer directamente un endpoint MDS.

**`PortfolioMarketDataApiController`**  
Ruta: `services/portfolio-service/src/main/java/com/tychewealth/controller/impl/PortfolioMarketDataApiController.java`

Actualmente implementa la entrada HTTP para las lecturas de datos de mercado de Portfolio. Delegar la operación nueva al servicio, manteniendo la convención de controladores delgados.

**`PortfolioMarketDataService` y `PortfolioMarketDataServiceImpl`**  
Rutas: `services/portfolio-service/src/main/java/com/tychewealth/service/PortfolioMarketDataService.java`, `services/portfolio-service/src/main/java/com/tychewealth/service/impl/PortfolioMarketDataServiceImpl.java`

La implementación actual valida selecciones, resuelve activos vigentes propiedad del usuario, deduplica símbolos y consulta a MDS por últimas cotizaciones o historial. Ampliar esta orquestación para obtener la `baseCurrency` de cada cartera seleccionada, recuperar observaciones y pares FX distintos requeridos en lotes, calcular los valores por activo y el total, y determinar la completitud. Conservar las comprobaciones de propiedad y el flujo del token de servicio. No escribir datos de activos.

**`PortfolioMarketDataHelper` y DTO de respuesta de valoración**  
Rutas: `services/portfolio-service/src/main/java/com/tychewealth/service/helper/marketdata/PortfolioMarketDataHelper.java`, `services/portfolio-service/src/main/java/com/tychewealth/dto/marketdata/`

El helper adapta actualmente resultados de precios MDS y los expande a activos. Ampliarlo o añadir un helper acotado si hace falta para mapear y calcular la valoración. La respuesta debe distinguir cotización reciente, fallback a checkpoint, estimación de bootstrap y valor ausente; conservar las fechas de observación y vigencia FX; e indicar si la valoración está completa. Los nombres exactos de los campos DTO y la representación del total incompleto son detalles de contrato por cerrar.

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

Las cotizaciones y checkpoints actuales contienen precio y fecha de captura, pero no divisa. Las cotizaciones recientes se leen de Redis; los checkpoints se persisten en PostgreSQL. Hacer explícita la moneda EUR inicial en la respuesta interna y proporcionar la observación persistida más reciente cuando no haya una cotización Redis reciente. Conservar su fecha real y distinguirla de una cotización reciente. No alterar el comportamiento establecido del historial/checkpoints de seis horas.

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

1. El usuario autenticado llama a la operación de valoración de Portfolio utilizando el modelo existente de selección de carteras/activos.
2. Portfolio valida la solicitud y resuelve únicamente activos vigentes pertenecientes al usuario, incluida la `baseCurrency` de cada cartera correspondiente.
3. Portfolio solicita en un lote las observaciones de mercado más recientes para los símbolos seleccionados. Cuando no haya una cotización Redis reciente, MDS devuelve el último checkpoint persistido si existe, con su fecha real de captura.
4. Si no hay cotización ni checkpoint, Portfolio usa `quantity × averagePrice` únicamente cuando se cumple la condición acordada de bootstrap e identifica el resultado como estimación. En otro caso, el valor de mercado del activo queda ausente.
5. Portfolio reúne los pares distintos de moneda de origen/destino para cotizaciones y estimaciones disponibles, y solicita las tasas FX necesarias a MDS en un lote. La conversión a la misma moneda no requiere una tasa externa. Las demás conversiones usan la tasa devuelta y su fecha efectiva; nunca se trata una tasa ausente como 1:1.
6. Portfolio calcula valores por activo y el total en moneda base. La ausencia de precios o tasas requeridos hace incompleta la valoración; la respuesta no debe presentar una suma parcial como total completo. No se actualiza ningún activo.
7. Portfolio devuelve el resultado mediante su API autenticada, con información suficiente de origen, fechas, vigencia FX y completitud para distinguir cotizaciones, checkpoints, estimaciones y datos ausentes.

#### Esquema de implementación

1. Cerrar la ruta pública de valoración y el esquema de respuesta, incluida la representación de origen y completitud por activo.
2. Definir cómo MDS informa una cotización reciente frente al último checkpoint persistido y cómo se determina la condición de bootstrap inicial.
3. Hacer explícita la moneda EUR inicial y añadir en MDS la ingestión diaria Frankfurter en lote, persistencia, lectura interna en lote, migración Liquibase y pruebas.
4. Ampliar `MarketDataClient` con lecturas autenticadas en lote de observaciones y tasas.
5. Añadir la orquestación de valoración en Portfolio y la conversión de monedas en lote, gestionando explícitamente resultados incompletos y sin modificar `averagePrice`.
6. Probar propiedad y filtrado de activos vigentes; varias monedas; cotización, checkpoint, estimación y datos ausentes; fechas efectivas FX y tasas faltantes; conversión a la misma moneda; y comportamiento del total/completitud. Actualizar la documentación API generada si cambia el contrato público.

## Preguntas abiertas

1. **Ruta y contrato JSON exactos:** ¿Qué ruta pública de valoración y campos estables de respuesta se usarán? ¿Cómo se representan valores ausentes y una respuesta incompleta debe incluir un subtotal parcial? Afecta la compatibilidad API y el comportamiento del frontend; debe definirse antes de implementar.
2. **Identificación del bootstrap:** ¿Cómo determinará MDS que no existe ninguna observación porque la captura inicial aún no ha ocurrido, en vez de porque Redis expiró o el proveedor está inactivo o fallando? Es necesario para evitar que las estimaciones oculten fallos operativos.
3. **Cobertura y modo de proveedor Frankfurter:** ¿El modo/fuente Frankfurter seleccionado cubre todos los valores de `CurrencyCodeEnum` admitidos por el producto? Verificar la cobertura, elegir la política de proveedor y definir qué hacer con monedas no soportadas antes de implementar. Mientras tanto, una tasa faltante implica valoración incompleta; nunca se usa un fallback 1:1.
