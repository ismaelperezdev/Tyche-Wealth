# Spike: Historial de precios de mercado

English version: [market-data-price-history-spike.en.md](market-data-price-history-spike.en.md).

## Investigación

El repositorio es un reactor Maven con `portfolio-service` y `market-data-service` (MDS). MDS tiene habilitados scheduling y carga de propiedades; Portfolio conserva carteras/activos y consulta sincrónicamente a MDS. `AssetEntity.averagePrice` representa el coste medio de adquisición, no una cotización, por lo que las lecturas de mercado no deben modificarlo.

`ActiveSymbolService.synchronizeSymbols` compara el conjunto actual de símbolos con una instantánea Redis y publica sus diferencias mediante `ActiveSymbolChangesEventPublisher`. El consumidor MDS de `active-symbol-changes` inicialmente solo registra el evento. El productor envía con `kafkaTemplate.send(topic, event)` sin clave; un identificador de evento no garantiza orden.

La dirección elegida para la capacidad de mercado fue que MDS sea propietario del catálogo de símbolos activos, cotizaciones recientes y checkpoints históricos; Portfolio conserva la propiedad de carteras, activos y su historial de cambios. El frontend entra por Portfolio: este resuelve los símbolos de los activos autorizados, los deduplica y consulta endpoints internos de MDS. MDS no se expone directamente al frontend.

MDS ya dispone de cliente externo Twelve Data, Redis, Kafka y configuración de scheduling, pero la fuente inicial acordada fue un proveedor de mercado mock dedicado. Las clases y configuración `Vehicle*` son superficie de migración heredada y no deben reutilizarse para esta capacidad.

## Problemas y restricciones

- Redis por sí solo no ofrece historial durable tras expirar la caché; hace falta persistir checkpoints.
- Un contador de seis horas en memoria se reinicia al desplegar. La frontera debe derivarse del tiempo y ser idempotente.
- El scheduler debe poder solicitar una cotización si Redis no está precalentado.
- Los checkpoints se conservan indefinidamente y deben ser únicos por símbolo y bucket.
- MDS debe validar defensivamente el mismo límite efectivo de símbolos que impone Portfolio, sin crear un valor numérico distinto.
- Los eventos Kafka delta pueden duplicarse o llegar desordenados; retries y DLT no sustituyen idempotencia ni orden.
- La eliminación física actual de activos/carteras impide conservar vínculos para reconstrucción histórica. Portfolio usa cascada y `orphanRemoval`; la dirección seleccionada fue soft delete explícito.
- El historial de cambios de activos debe registrarse automáticamente en la transacción de mutación. No es un ledger ni un servicio de transacciones.
- Las rutas internas y DTO concretos de lectura no deben exponer Redis ni tablas como API.
- La paginación, el muestreo y el tratamiento de checkpoints del endpoint de historial se concretan en el spike complementario [Estrategia de consulta de checkpoints de precios de mercado](../market-price-checkpoint-query/market-price-checkpoint-query-spike.es.md).

## Opciones posibles

### Opción: Redis como único almacén de precios

Guardar únicamente la última cotización JSON por símbolo en Redis y servirla directamente.

**Ventajas:** coste inicial menor, lecturas rápidas y sin migraciones.

**Desventajas:** la expiración elimina la historia y no permite gráficas ni cálculos históricos durables.

### Opción: Caché Redis más almacenamiento histórico en MDS — seleccionada

Mantener la última cotización en Redis y guardar en PostgreSQL checkpoints por símbolo y bucket UTC de seis horas. Añadir modelo, repositorios, Liquibase, consumo de deltas, scheduler y lecturas internas.

**Ventajas:** separa lecturas de baja latencia de la retención, soporta gráficas sin alterar el coste de adquisición y permite capturas idempotentes.

**Desventajas:** incorpora persistencia y operación de esquema a MDS, además de evolucionar contrato Kafka y tratamiento de datos.

### Opción: Contador de seis horas o dos schedulers

Un contador en memoria o un scheduler de cotización más otro scheduler de checkpoints.

**Ventajas:** la intención temporal parece explícita.

**Desventajas:** el contador se pierde al reiniciar; dos schedulers elevan la coordinación y no garantizan idempotencia.

### Opción: Scheduler en Portfolio o composición desde el frontend

Portfolio consultaría y persistiría cotizaciones o el frontend llamaría directamente a MDS.

**Ventajas:** Portfolio está cerca de los activos y la llamada directa eliminaría un salto HTTP.

**Desventajas:** mezcla propiedad de mercado y cartera, duplica símbolos entre usuarios o rompe límites de autorización/composición al exponer MDS.

### Opción: Sobrescribir `averagePrice` con la cotización

Actualizar el coste medio de adquisición cada vez que se lee el precio de mercado.

**Desventajas:** destruye el significado del campo y convierte una lectura en una escritura con efectos secundarios. No seleccionada.

### Opción: CRUD externo de auditoría o registros inmutables automáticos

Crear registros de cambios desde endpoints genéricos o automáticamente junto con las mutaciones de activos.

Se seleccionaron entradas inmutables automáticas en la misma transacción: CRUD permitiría reescribir/borrar historia y no asegura que acompañe la mutación.

### Opción: Eliminación física o soft delete

Se seleccionó `deleted_at` en carteras y activos, con filtros explícitos. La eliminación física rompe los vínculos históricos; el soft delete requiere migraciones y revisar consultas, listados, validaciones, conteos y unicidad.

### Opción: Twelve Data directo o mock dedicado

Se seleccionó un proveedor mock dedicado para desarrollar y probar el flujo sin cuota, credenciales ni disponibilidad externa. Twelve Data queda como evolución; no reutilizar clases ni configuración `Vehicle*`.

## Solución elegida

### Decisión

Adoptar caché Redis más almacenamiento histórico persistente en MDS, con proveedor mock dedicado inicial. MDS posee el catálogo de símbolos activos, cotizaciones recientes y checkpoints; Portfolio posee carteras, activos e historial de cambios. Portfolio compone la respuesta autenticada mediante un `MarketDataClient` concreto basado en Spring `RestClient`, sin introducir una interfaz solo para esa llamada.

El scheduler MDS se ejecuta cada cinco minutos. Para cada símbolo activo obtiene una cotización, la guarda como JSON en Redis con TTL de diez minutos y persiste si el bucket UTC actual de seis horas todavía no tiene checkpoint. El bucket se calcula desde el tiempo, no con un contador. Si Redis no contiene cotización, el scheduler consulta al proveedor.

El contrato provisional `active-symbol-changes` añade `eventId` y `occurredAt`; MDS consume idempotentemente. El topic conserva tres particiones. Mientras el contrato sea delta, debe definirse un mecanismo de orden para evitar estado obsoleto; `eventId` por sí solo no ordena. La autenticación servicio-a-servicio se resolvió por separado en [Autenticación interna con OAuth2 Client Credentials](../internal-service-authentication/internal-service-authentication-spike.es.md).

### Diseño detallado

MDS persiste `market_symbols` con `id`, símbolo normalizado único, `active`, `created_at`, `updated_at` y `deactivated_at`. `market_price_checkpoints` contiene `id`, FK a símbolo, `price`, `captured_at` y `bucket_start`, con unicidad `(market_symbol_id, bucket_start)`. Los checkpoints no expiran.

El proveedor mock dedicado alimenta el polling. `TwelveDataApiClient` sigue siendo un cliente externo existente y posible evolución. Ninguna clase ni configuración MDS `Vehicle*` participa en este diseño.

Portfolio añade un endpoint para precios/gráficas de símbolos autorizados. Obtiene símbolos de activos vigentes del usuario, los deduplica y consulta endpoints internos MDS para precio actual e historial. Rutas y DTO no exponen Redis ni tablas. MDS valida defensivamente el mismo límite efectivo de símbolos de Portfolio. La integración no escribe `AssetEntity.averagePrice`.

Portfolio añade `asset_changes`: `id`, `asset_id` como única FK, `change_type` (`CREATED`, `MANUAL_UPDATE`, `DELETED`), cantidades anterior/nueva, coste medio anterior/nuevo y `occurred_at`. Los cuatro valores pueden ser null según el tipo de cambio. No incluye `portfolio_id`, no ofrece CRUD externo y no es un ledger. Un PATCH crea una entrada inmutable en la misma transacción solo cuando cambian campos pertinentes.

Portfolio usa soft delete mediante `deleted_at` para carteras y activos. Borrar cartera marca también sus activos y registra los cambios de borrado. La restricción `uq_asset_portfolio_id_symbol` se migra a un índice único parcial PostgreSQL `(portfolio_id, symbol) WHERE deleted_at IS NULL`, permitiendo recrear un símbolo eliminado. Las consultas deben excluir explícitamente registros eliminados; no se asume un filtro global JPA.

### Clases existentes que cambiarían

- `TycheWealthMarketDataServiceApplication`: conservar scheduling y escaneo de propiedades para registrar nuevos componentes.
- `ActiveSymbolChangesEventConsumer`, `ActiveSymbolChanges`, `KafkaConfig` de MDS: contrato evolucionado, proyección idempotente y consumo con estrategia de orden; conservar retry y DLT.
- `ActiveSymbolService`, `ActiveSymbolChanges` y `ActiveSymbolChangesEventPublisher` de Portfolio: emitir `eventId` y `occurredAt`; el productor actual no usa clave.
- `AssetEntity`, `PortfolioEntity`, repositorios, validadores y servicios: `deleted_at`, filtros explícitos y soft delete. `AssetRepository.findDistinctSymbolsByUserIds` excluye activos/carteras eliminados.
- `AssetServiceImpl`: registrar cambios inmutables en PATCH y borrado sin alterar `averagePrice` por lecturas de mercado.
- `PortfolioServiceImpl`: reemplazar borrado físico y marcar activos al borrar cartera.
- Changelogs Liquibase de ambos servicios: añadir esquemas e índices de sus dominios.

### Clases nuevas

- En MDS: entidades, repositorios y servicios para `market_symbols` y `market_price_checkpoints`; proveedor mock, scheduler de cinco minutos y DTO/controladores internos para precio actual e historial.
- En Portfolio: `MarketDataClient` concreto basado en Spring `RestClient`, servicio/controlador de composición, entidad/repositorio `asset_changes` y tipo `change_type`.

Los nombres finales de rutas, DTO y orquestación se fijan en las historias de implementación; el spike define responsabilidades y límites, no firmas públicas finales.

### Flujo de componentes

1. User service publica usuarios activos; Portfolio los consume y `ActiveSymbolService.synchronizeSymbols` compara la instantánea Redis con símbolos de carteras vigentes.
2. Portfolio publica diferencias `addedSymbols`/`removedSymbols` en `active-symbol-changes` con `eventId` y `occurredAt`; el topic conserva tres particiones.
3. MDS consume idempotentemente y activa/desactiva filas `market_symbols`; los fallos siguen retry y DLT.
4. Cada cinco minutos el scheduler consulta el mock para cada símbolo activo, actualiza caché Redis con TTL de diez minutos y persiste solo si el bucket UTC de seis horas es nuevo.
5. El frontend solicita precios/historial a Portfolio. Portfolio resuelve símbolos de activos vigentes del usuario, los deduplica y consulta MDS.
6. Portfolio devuelve la respuesta sin escribir `AssetEntity.averagePrice`.
7. Crear, cambiar manualmente o borrar un activo registra automáticamente su cambio en `asset_changes`.

### Esquema de implementación

Dividir entregas por límites de responsabilidad: primero modelo/migraciones MDS, proyección idempotente de símbolos y mock; después caché, scheduler y checkpoints; después lecturas internas y composición desde Portfolio. En paralelo o antes, implementar soft delete y `asset_changes` en Portfolio. Cada cambio de persistencia incorpora entidad, repositorio, changelog y pruebas del servicio en la misma entrega.

No se incluyen un microservicio de transacciones, valoración histórica completa de carteras, Twelve Data como proveedor inicial ni un contrato de snapshots versionado. La autenticación servicio-a-servicio se resolvió por separado en [Autenticación interna con OAuth2 Client Credentials](../internal-service-authentication/internal-service-authentication-spike.es.md). Las demás evoluciones dependen de las necesidades de producción, escala y orden.

## Preguntas abiertas

- ¿Qué normalización de símbolos y mercado/divisa representará inicialmente el mock?
- ¿Cómo se gestionarán límites de uso, errores y sustitución gradual del mock por Twelve Data?
- ¿Qué estrategia de orden/snapshot se usará con tres particiones y eventos delta para garantizar recuperación?
- ¿Qué cambios de activos, además de cantidad y coste medio, se necesitarán para reconstruir valoraciones futuras?
- ¿Cómo se migrarán y verificarán todas las consultas para excluir explícitamente entidades con `deleted_at` no nulo?

Las decisiones sobre paginación, muestreo y tratamiento seguro de checkpoints históricos están documentadas en el spike complementario [Estrategia de consulta de checkpoints de precios de mercado](../market-price-checkpoint-query/market-price-checkpoint-query-spike.es.md); no son preguntas abiertas de este spike.

## Historias derivadas del spike

### Evolucionar el contrato Kafka de símbolos activos

**Objetivo:** evolucionar `ActiveSymbolChanges` para incluir `eventId` y `occurredAt`, conservando los conjuntos de símbolos añadidos y eliminados, y operar `active-symbol-changes` con tres particiones.

**Criterios de aceptación:**

- El productor de Portfolio publica cada delta con `eventId` y `occurredAt` no nulos.
- El contrato documenta que los mensajes son deltas y que `eventId` por sí solo no garantiza orden.
- La configuración operativa del topic conserva tres particiones y el consumidor usa la estrategia de orden definida para el contrato delta.
- Se conserva la política existente de reintentos y DLT de MDS para el contrato evolucionado.
- Se documenta la futura necesidad de un contrato de snapshots versionado si aumentan los requisitos de escala o recuperación/orden.

### Persistir símbolos de mercado y consumir deltas idempotentemente en MDS

**Objetivo:** añadir el catálogo persistido `market_symbols` a MDS y hacer que el consumidor proyecte idempotentemente los eventos de símbolos activos.

**Criterios de aceptación:**

- Liquibase crea `market_symbols` con `id`, símbolo normalizado único, `active`, `created_at`, `updated_at` y `deactivated_at`.
- Los eventos repetidos no producen estados inconsistentes ni filas duplicadas.
- Un símbolo añadido queda activo y uno eliminado queda inactivo con su fecha correspondiente.
- El consumidor deja de limitarse a registrar el evento y conserva la configuración de reintentos y DLT.
- La entrega incluye entidad, repositorio, changelog y pruebas MDS.

### Capturar cotizaciones mock, caché Redis y checkpoints persistidos

**Objetivo:** obtener cotizaciones mediante un mock dedicado de mercado, actualizar el precio más reciente y conservar checkpoints de seis horas.

**Criterios de aceptación:**

- Un scheduler MDS se ejecuta cada cinco minutos y procesa solo símbolos activos.
- El mock de mercado es una capacidad nueva y no reutiliza `VehiclePollingScheduler`, `VehiclePollingService`, ninguna otra clase `Vehicle*` ni configuración `Vehicle*`; son deuda de migración/eliminación heredada que se espera retirar.
- Cada cotización se guarda como JSON en Redis con una clave por símbolo y TTL de diez minutos.
- Si la caché no contiene cotización, el scheduler consulta al proveedor antes de continuar.
- Liquibase crea `market_price_checkpoints` con FK a `market_symbols`, `price`, `captured_at`, `bucket_start` y unicidad `(market_symbol_id, bucket_start)`.
- El bucket se calcula desde UTC, sin contador en memoria; se conserva como máximo un checkpoint por símbolo y bucket.
- Los checkpoints no tienen expiración ni tarea de borrado asociada.

### Exponer lecturas internas MDS y componer precios desde Portfolio

**Objetivo:** permitir que Portfolio entregue al frontend precios recientes e historial de sus símbolos autorizados sin exponer MDS directamente.

**Criterios de aceptación:**

- MDS ofrece endpoints internos de última cotización e historial; las rutas y DTO se definen en la historia sin exponer Redis ni tablas como API.
- Portfolio añade un endpoint autenticado orientado al frontend y resuelve desde su base de datos los símbolos de los activos vigentes del usuario.
- Portfolio deduplica símbolos y consulta MDS sincrónicamente mediante un `MarketDataClient` concreto basado en Spring `RestClient`.
- Portfolio y MDS aplican el mismo límite efectivo de símbolos de Portfolio; MDS lo valida defensivamente sin introducir otro límite numérico.
- La composición de la respuesta no escribe ni modifica `AssetEntity.averagePrice`.
- MDS sigue siendo un servicio interno. Las llamadas Portfolio → MDS usan la decisión OAuth2 Client Credentials documentada en [Autenticación interna con OAuth2 Client Credentials](../internal-service-authentication/internal-service-authentication-spike.es.md); MDS rechaza tokens de usuario en sus endpoints internos.

### Registrar auditoría inmutable de cambios de activos

**Objetivo:** conservar cambios relevantes de posiciones en `asset_changes` sin introducir CRUD de auditoría ni un ledger de transacciones.

**Criterios de aceptación:**

- Liquibase crea `asset_changes` con `id`, `asset_id` como única FK, `change_type`, cantidades anterior/nueva y costes medios anterior/nuevo nullable, y `occurred_at`.
- `change_type` admite exactamente `CREATED`, `MANUAL_UPDATE` y `DELETED`.
- La creación, actualización manual relevante y eliminación de un activo generan automáticamente una entrada en la misma transacción que la mutación.
- PATCH registra cantidad y coste medio anteriores y nuevos solo cuando corresponde.
- No existen endpoints externos para crear, actualizar o eliminar entradas de auditoría.
- La tabla no incluye `portfolio_id` ni se presenta como tabla de transacciones o ledger.

### Aplicar borrado lógico y migrar unicidad y consultas de Portfolio

**Objetivo:** sustituir el borrado físico de carteras y activos por borrado lógico, preservando la relación necesaria para el historial y la recreación de símbolos.

**Criterios de aceptación:**

- Liquibase añade `deleted_at` a `portfolios` y `assets`.
- El borrado de un activo marca `deleted_at` y crea su entrada de auditoría `DELETED`; no elimina físicamente la fila.
- El borrado de una cartera marca la cartera y sus activos asociados y crea las entradas de borrado correspondientes.
- La restricción `uq_asset_portfolio_id_symbol` se reemplaza por el índice único parcial PostgreSQL `(portfolio_id, symbol) WHERE deleted_at IS NULL`.
- Se puede recrear un símbolo eliminado en la misma cartera y se siguen rechazando símbolos activos duplicados.
- Repositorios, validaciones, listados, recuperaciones, conteos y comprobaciones de conflicto excluyen explícitamente las entidades eliminadas.
- `AssetRepository.findDistinctSymbolsByUserIds` excluye activos y carteras eliminados para no publicar símbolos inactivos.
- La implementación no presupone un filtro global automático: sigue el precedente de filtros explícitos usado para el borrado lógico de usuarios.

## Historia futura: preservar el orden de deltas de símbolos activos con tres particiones Kafka

El contrato actual de deltas `active-symbol-changes` depende del orden de mensajes, mientras que el topic está previsto con tres particiones Kafka. El productor actual publica eventos sin clave, por lo que deltas consecutivos pueden distribuirse en distintas particiones y consumirse fuera de orden. Esto debe resolverse en una historia separada, no mediante cambios en la persistencia MDS.

La misma restricción de orden se aplica al snapshot `active-users` que consume Portfolio. Por tanto, ambos topics usan tres particiones y una clave de enrutamiento estable por flujo, manteniendo cada secuencia en una partición ordenada sin cambiar los payloads.

La historia futura debe definir una estrategia compatible con tres particiones, como una clave de enrutamiento estable o un contrato de snapshot/delta versionado. Una clave estable preserva el orden, pero puede concentrar tráfico; la versión permitiría usar las tres particiones y que MDS rechace eventos obsoletos. La migración debe considerar los topics y registros existentes.

### Criterios de aceptación candidatos

- `active-symbol-changes` conserva tres particiones.
- `active-users` conserva tres particiones.
- La estrategia elegida garantiza que deltas obsoletos no dejen a MDS con un estado incorrecto de símbolos activos.
- User service, Portfolio y MDS usan el mismo contrato de orden para sus respectivos topics.
- Los productores publican cada flujo con una clave de enrutamiento estable y no nula.
- Pruebas cubren eventos fuera de orden, duplicados, reintentados y migrados.
- Se documenta el procedimiento de migración para un topic existente.

### Procedimiento de migración

Para un topic existente, detener el productor correspondiente antes de cambiar el número de particiones y drenar el consumer group hasta vaciar el backlog actual. Iniciar el servicio que declara el bean `NewTopic` para ampliar el topic a tres particiones; después desplegar el productor con la clave de enrutamiento estable y reanudar el consumo normal. Así se evita intercalar registros antiguos sin clave con nuevos registros con clave durante el corte; añadir particiones no redistribuye los registros ya almacenados.
