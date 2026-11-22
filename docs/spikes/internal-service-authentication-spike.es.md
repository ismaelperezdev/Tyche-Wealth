# Spike: Autenticación interna con OAuth2 Client Credentials en user-service

Versión en inglés: [internal-service-authentication-spike.en.md](internal-service-authentication-spike.en.md).

## Investigación

### Objetivo y decisión de alcance

Centralizar la emisión de tokens de servicio en `user-service` mediante Spring Authorization Server, sin migrar ni sustituir la autenticación actual de usuarios. El alcance inicial es Portfolio → Market Data Service (MDS). MDS debe rechazar tokens de usuario en sus endpoints internos.

Este documento concreta la seguridad pendiente en `docs/spikes/market-data-price-history-spike.md`. No reabre paginación, batching, límites de resultados ni composición de precios.

La investigación se basa en inspección del código, dependencias gestionadas y documentación de Spring. No se ha implementado ni compilado una prueba de concepto de Authorization Server.

### Implementación existente

- `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioMarketDataApi.java` y `controller/impl/PortfolioMarketDataApiController.java` reciben el principal de usuario y el encabezado `Authorization`.
- `services/portfolio-service/src/main/java/com/tychewealth/service/PortfolioMarketDataService.java` y `service/impl/PortfolioMarketDataServiceImpl.java` propagan ese encabezado. La implementación valida la selección y propiedad de activos, resuelve símbolos y reenvía el token del usuario mediante su método privado `accessToken`.
- `clients/market-data-client/src/main/java/com/tychewealth/marketdataclient/MarketDataClient.java` ya acepta un access token en `getLatestPrices` y `getPriceHistory`, y lo envía a `/internal/prices/latest` y `/internal/prices/history`.
- `services/market-data-service/src/main/java/com/tychewealth/security/config/ApplicationSecurityConfig.java` utiliza `JwtAuthenticationFilter` y exige autenticación, pero no identidad técnica ni scopes.
- En MDS, `security/token/AccessTokenSupport.java`, `TokenValidator.java` y `TokenStateSupport.java` verifican tokens humanos HMAC, sujetos numéricos y revocación en Redis.
- En `user-service`, `config/security/PrometheusSecurityConfig.java` tiene orden 1 y `ApplicationSecurityConfig.java` orden 2. Las APIs actuales de usuarios utilizan un filtro JWT propio y conservan su contrato de autenticación.

El prefijo `/internal` no constituye una barrera de autenticación. Actualmente un token humano válido puede autenticar una consulta de precios en MDS.

### Compatibilidad y contexto futuro

El `pom.xml` raíz establece Java 21 y Spring Boot 3.5.14. Su BOM gestiona Spring Security 6.5.10 y Spring Authorization Server 1.5.7. El starter puede incorporarse únicamente al POM de `user-service`, sin fijar otra versión ni actualizar Boot. Las versiones deberán confirmarse al implementar si el proyecto cambia.

Se prevé un servicio RAG que investigue en Internet, genere y almacene informes autónomamente, y los exponga a consultas internas de Portfolio. La producción es asíncrona; consultar un informe disponible puede ser HTTP síncrono. No requiere callbacks a Portfolio ni endpoints públicos. También se considera un servicio de transacciones, cuyo flujo y alcance no están definidos. Ninguno se implementa en este spike.

## Problemas y restricciones

1. **Compatibilidad humana:** preservar login, refresh, logout, revocación y principales numéricos actuales. Un cliente técnico no es una cuenta de `UserEntity`.
2. **Cadenas aisladas:** el Authorization Server necesita una cadena anterior a las existentes y limitada mediante su matcher de endpoints. Los endpoints OAuth no heredan el prefijo MVC de las APIs actuales.
3. **Registro del filtro:** `services/user-service/src/main/java/com/tychewealth/config/security/SecurityCommonConfig.java` expone `JwtAuthenticationFilter` como bean sin desactivar su registro automático como filtro servlet. Debe limitarse a la cadena humana. MDS ya muestra el patrón con `FilterRegistrationBean` en su `security/config/SecurityCommonConfig.java`.
4. **Confianza separada:** no reutilizar `JWT_SECRET`. Los tokens técnicos tendrán firma asimétrica y política explícita de emisor, audiencia, identidad y scopes. Una firma válida por sí sola no basta.
5. **Persistencia:** PostgreSQL y Liquibase gestionan el esquema. `services/user-service/src/main/resources/db.changelog/changelog-master.xml` incluye usuarios, refresh tokens y dispositivos, no clientes OAuth. No reutilizar esas tablas para identidades técnicas.
6. **Claves y secretos:** claves duraderas fuera de Git, publicación exclusiva de claves públicas y rotación con `kid`. No generar nuevas claves de producción en cada arranque. Proteger también las credenciales OAuth en tránsito mediante TLS.
7. **Disponibilidad:** `user-service` ya es crítico; ahora también será necesario para adquirir y renovar tokens internos. MDS validará localmente tokens vigentes. La disponibilidad de JWKS, su caché y los cambios de clave condicionan esa independencia. No habrá fallback al token humano.
8. **Errores:** conservar los errores OAuth en el endpoint de tokens y el contrato JSON de MDS para sus APIs. No presentar una caída del emisor como necesidad de volver a iniciar sesión.
9. **Protección frente a abuso:** `services/user-service/src/main/java/com/tychewealth/config/AuthRateLimitConfig.java` registra interceptores MVC. No asumir que cubren `/oauth2/token`, procesado por filtros de seguridad.
10. **Pruebas reales:** parte de las pruebas de usuarios usa configuración de seguridad específica de test. Añadir pruebas con cadenas de producción y servidor embebido para detectar registro servlet del filtro.
11. **Despliegue coordinado:** Portfolio actual envía tokens humanos; MDS propuesto los rechaza. Historias separadas no implican activaciones independientes compatibles.
12. **Límite de autorización:** Portfolio mantiene la propiedad y selección de activos. La identidad de servicio no otorga acceso implícito a datos privados de cualquier usuario. Los futuros informes privados y transacciones requieren políticas propias.
13. **Red:** autenticación no sustituye restricciones de exposición. Un RAG con conexiones salientes a Internet no necesita una API pública. Sus credenciales internas no deben enviarse a páginas externas ni incluirse en prompts.

## Opciones posibles

### Opción: Token opaco estático por consumidor

#### Enfoque

Portfolio presenta una credencial aleatoria propia y MDS la valida.

#### Cambios esperados

Configuración de secretos y autenticación técnica en ambos servicios, retirando el reenvío del token humano.

#### Ventajas

Menor implementación inicial, sin emisor ni renovación; válido para pocas relaciones.

#### Desventajas

Credencial reutilizable hasta rotación, distribución coordinada y ausencia del modelo estándar de emisión centralizada seleccionado.

### Opción: JWT corto firmado por Portfolio

#### Enfoque

Portfolio firma con su clave privada y MDS verifica con una clave pública.

#### Cambios esperados

Generación de tokens en Portfolio y validación asimétrica en MDS.

#### Ventajas

Sin nuevo emisor desplegable y validación local; el receptor no puede firmar con la clave pública.

#### Desventajas

Distribuye emisión entre servicios de negocio y exige un modelo propio de confianza. No encaja con centralizar autenticación en `user-service`. La expiración corta limita un token robado, pero no resuelve el robo de la clave privada.

### Opción: Authorization Server estándar en user-service

#### Enfoque

Incorporar Spring Authorization Server para clientes técnicos manteniendo la autenticación humana existente.

#### Cambios esperados

Cadena OAuth, clientes en memoria desde configuración, firma asimétrica y JWKS; Portfolio como OAuth2 Client y MDS como Resource Server.

#### Ventajas

Centraliza identidad donde ya existe autenticación, utiliza un protocolo estándar y evita otro despliegue. No obliga a migrar usuarios.

#### Desventajas

Comparte impacto operativo y de seguridad, y exige mantener claves, credenciales, permisos, persistencia y pruebas de aislamiento. El framework no constituye por sí solo una consola completa de gestión de identidad.

### Opción: Proveedor separado como Keycloak

#### Enfoque

Un proveedor externo o autogestionado emite tokens técnicos; se conserva inicialmente el login actual.

#### Cambios esperados

Despliegue y almacenamiento del proveedor, registro de clientes y recursos, e integración OAuth de consumidores y receptores.

#### Ventajas

Administración incorporada, federación y SSO si se requieren; aislamiento de despliegue.

#### Desventajas

Otro componente operativo y dos sistemas de identidad mientras no se migren usuarios. Sus capacidades adicionales no son requisitos actuales.

### Opción: Nuevo microservicio de identidad

#### Enfoque

Crear un servicio dedicado con un Authorization Server estándar.

#### Cambios esperados

Nuevo módulo, despliegue, almacenamiento y supervisión, además de las integraciones OAuth.

#### Ventajas

Aislamiento operativo y evolución independiente.

#### Desventajas

Más infraestructura sin una necesidad actual de aislamiento. Separar también el login humano sería otro alcance.

### Opción: Endpoint propio de emisión en user-service

#### Enfoque

Validar credenciales y emitir JWT con código propio.

#### Cambios esperados

Implementación de autenticación técnica, emisión, permisos, errores y rotación.

#### Ventajas

Puede reducir integración inicial para un caso mínimo.

#### Desventajas

No equivale automáticamente a OAuth2 Client Credentials y duplica responsabilidades del framework. No se selecciona para el objetivo de reutilización.

### Opción: Librerías compartidas de seguridad interna

#### Enfoque

Complementar la solución con una posible `internal-security-support` para validadores comunes y una posible `internal-client-support` para adquisición OAuth compartida.

#### Cambios esperados

Extraer únicamente código repetido y estable. Mantener cadenas, rutas, audiencias esperadas, permisos y errores en cada servicio. No instalar automáticamente una cadena global ni envolver todo Spring Security.

#### Ventajas

Evita divergencias del contrato técnico y facilita reutilización entre proyectos.

#### Desventajas

Añade APIs y compatibilidad compartidas sin duplicación confirmada todavía. Dos destinos dentro de Portfolio no equivalen a dos consumidores independientes.

Se conserva como evolución opcional: evaluar validación compartida al incorporar un segundo receptor y adquisición compartida al aparecer un segundo micro consumidor. No es requisito de la primera implementación ni reduce su estimación inicial.

### Opción: mTLS o identidad de workloads

#### Enfoque

Autenticar servicios mediante certificados, sustituyendo o complementando tokens.

#### Cambios esperados

Emisión, distribución y renovación de certificados, además de autorización por operación cuando proceda.

#### Ventajas

Identidad en la conexión y protección del transporte.

#### Desventajas

Complejidad operativa si la plataforma no lo proporciona. No se introduce una service mesh solo para esta integración.

## Solución elegida

### Decisión

Usar **Spring Authorization Server dentro de user-service para Client Credentials**, preservando la autenticación humana. Portfolio solicita tokens; no emite ni concede permisos. MDS autentica servicios y autoriza sus operaciones.

Se acepta compartir despliegue y criticidad en `user-service` a cambio de no añadir otro componente. Se reconsiderará separación por requisitos concretos de aislamiento, no por el número de micros. La reutilización futura es un beneficio, no una razón para crear ahora un framework genérico.

### Diseño detallado

#### Clases existentes a modificar

**`ApplicationSecurityConfig` — user-service**  
Ruta: `services/user-service/src/main/java/com/tychewealth/config/security/ApplicationSecurityConfig.java`

Mantener la cadena humana detrás de la cadena OAuth. No cambiar principales ni convertir las APIs de usuarios en receptoras de tokens técnicos.

**`SecurityCommonConfig` — user-service**  
Ruta: `services/user-service/src/main/java/com/tychewealth/config/security/SecurityCommonConfig.java`

Desactivar el registro servlet del filtro JWT humano y mantener su inserción explícita. Integrar el encoder de secretos OAuth sin modificar el formato de contraseñas humanas ni introducir incompatibilidad con el BCrypt existente.

**Configuración y persistencia — user-service**  
Rutas: `services/user-service/pom.xml`, `src/main/resources/application.properties` y `src/main/resources/db.changelog/changelog-master.xml`, relativos al módulo.

Añadir el starter solo al módulo. Incorporar repositorio JDBC estándar de clientes y migraciones basadas en el esquema de la versión utilizada, adaptadas y verificadas para PostgreSQL. No añadir consentimiento para un flujo exclusivamente Client Credentials sin necesidad real.

**`PortfolioMarketDataApi`, `PortfolioMarketDataApiController`, `PortfolioMarketDataService`, `PortfolioMarketDataServiceImpl`**  
Rutas relativas a `services/portfolio-service/src/main/java/com/tychewealth/`: `controller/PortfolioMarketDataApi.java`, `controller/impl/PortfolioMarketDataApiController.java`, `service/PortfolioMarketDataService.java`, `service/impl/PortfolioMarketDataServiceImpl.java`.

Retirar `authorizationHeader` de las firmas destinadas al reenvío y el método privado `accessToken`. El filtro de entrada sigue exigiendo el JWT humano. Preservar rutas, DTOs, validación de propiedad, filtrado temporal y límites. Obtener el token técnico después de validar y resolver activos y de aplicar salidas tempranas.

**`MarketDataClient` — sin cambios previstos**  
Ruta: `clients/market-data-client/src/main/java/com/tychewealth/marketdataclient/MarketDataClient.java`.

Reutilizar el parámetro de token existente. No incorporar OAuth a esta librería. No crear `UserServiceClient`: Spring OAuth2 Client ya implementa la solicitud de tokens. Ese cliente de negocio solo tendría sentido para APIs de usuarios distintas de OAuth.

**`ApplicationSecurityConfig` y `SecurityCommonConfig` — MDS**  
Rutas: `services/market-data-service/src/main/java/com/tychewealth/security/config/ApplicationSecurityConfig.java` y `SecurityCommonConfig.java` en el mismo paquete.

Usar OAuth2 Resource Server para las rutas de precios. Conectar los handlers JSON existentes también al procesamiento bearer. Mantener Prometheus y health, y definir explícitamente la política de rutas no contempladas.

Retirar `security/JwtAuthenticationFilter.java` y los componentes de `security/token/` de autenticación humana cuando no tengan consumidores. No dejar beans obsoletos que sigan exigiendo `JWT_SECRET` o blacklist Redis. Esto no elimina Redis del almacenamiento de precios.

**Configuración de Portfolio y MDS**

Añadir los starters `spring-boot-starter-oauth2-client` y `spring-boot-starter-oauth2-resource-server` únicamente a sus respectivos POM. Configurar registro OAuth, token URI, credenciales, issuer y JWKS según el entorno, sin alterar la autenticación humana entrante de Portfolio.

#### Nuevas clases a crear

Nombres orientativos; no requieren interfaces de una sola implementación.

**`AuthorizationServerConfig`**  
Ruta sugerida: `services/user-service/src/main/java/com/tychewealth/config/security/AuthorizationServerConfig.java`.

Define la cadena OAuth con orden anterior a las existentes, limitada al matcher del servidor, y los componentes estándar. No habilita login de navegador, OIDC, registro dinámico ni grants humanos para este alcance.

**`ServiceTokenClaimsCustomizer`**  
Ruta sugerida: `services/user-service/src/main/java/com/tychewealth/service/token/ServiceTokenClaimsCustomizer.java`.

Extensión estándar para asignar audiencia e identidad técnica a Client Credentials. Restringe permisos a los configurados para el cliente y no copia claims arbitrarios de la solicitud. El cliente no elige libremente su destinatario. El personalizador permanece como bean en `InternalServiceTokenServerConfig`: lee la lista no vacía de audiencias del cliente registrado en memoria y la contrasta con la lista permitida de configuración. Una configuración de cliente ausente o inválida rechaza la emisión con `server_error`. Los parámetros de solicitud no sustituyen esta política.

**Registros de clientes desde configuración**
Configurados bajo `app.oauth2.authorization-server.clients` en `services/user-service/src/main/resources/application.properties` o en la configuración externa del despliegue.

Al arrancar, `InternalServiceTokenServerConfig` construye un `InMemoryRegisteredClientRepository` desde la configuración. Clientes, scopes, audiencias y duración se reconstruyen en cada reinicio; no hay tabla OAuth ni administración dinámica en base de datos. Los secretos se reciben desde variables de entorno o un gestor de secretos, se codifican con el `PasswordEncoder` del servicio y solo se conservan como hashes en los registros en memoria. No incluir secretos productivos en Git.

**`InternalAccessTokenProvider`**  
Integración local en Portfolio sobre el componente común de cliente de tokens.

Configura `AuthorizedClientServiceOAuth2AuthorizedClientManager`, la identidad estable del servicio y el registro destinado a MDS. Spring gestiona reutilización y readquisición al expirar; no implementar protocolo HTTP ni caché propios por defecto.

**`InternalTokenValidators`**, si se necesitan validadores adicionales  
Componente común de validación, configurable por cada resource server.

Centraliza únicamente las reglas compartidas del contrato de tokens internos que no cubra el Resource Server estándar. La política de rutas y permisos propios de cada servicio permanece en ese microservicio.

**Librería compartida de autenticación entre servicios**

Debe ofrecer capacidades independientes para clientes OAuth2 (adquisición y reutilización de tokens) y resource servers (validación de firma/JWKS y reglas comunes del contrato). Cada microservicio incorpora solo el lado que necesita y aporta su configuración de issuer, audience y credenciales. La caché JWKS es local al consumidor y no requiere una tabla compartida ni Redis. La librería nunca contiene claves privadas ni decide los `kid`; `user-service` conserva la propiedad de las claves de firma y su rotación.

Contrato de identidad firmado: `token_type=service` se emite solo para access tokens Client Credentials y es obligatorio en la librería. Es distinto del campo `token_type=Bearer` de la respuesta OAuth. Esta decisión cierra el nombre y valor del claim antes pendientes; el issuer y la autorización por cliente siguen siendo responsabilidades del despliegue y de cada servicio.

#### Interacción y flujo

1. El usuario se autentica con el mecanismo actual.
2. Portfolio valida selección, propiedad y estado activo; aplica salidas tempranas.
3. El proveedor local obtiene un token vigente o solicita otro a `/oauth2/token`.
4. `user-service` autentica el cliente, verifica grant y scopes y firma un token técnico.
5. `MarketDataClient` presenta ese token en la llamada de precios.
6. MDS valida firma, emisor, timestamps, expiración obligatoria, audiencia e identidad técnica; exige scope y política del llamador.
7. El flujo de precios y composición continúa sin cambiar reglas de negocio.

Contrato inicial propuesto:

| Elemento | Regla |
|---|---|
| Cliente inicial | `portfolio-service` |
| Grant | Solo `client_credentials` |
| Autenticación | `client_secret_basic`, sobre TLS |
| Audiencia | `market-data-service` |
| Scope | `market-prices:read` |
| Identidad | Claim explícito y controlado por el emisor para clientes técnicos |
| Firma | Asimétrica, separada de los tokens humanos |
| Duración | 15 minutos por defecto, configurable |
| Refresh token | No; solicitar otro access token |
| Claves públicas | JWKS con `kid` |

Los literales del claim técnico y del issuer deben cerrarse antes de implementar consumidores. MDS rechazará tokens humanos aunque sean válidos para Portfolio. Token inválido: 401; token técnico válido sin permisos: 403. No asumir que Client Credentials introduce automáticamente un claim `grant_type`.

Los JWT emitidos no se invalidan inmediatamente por cambiar el secreto del cliente o deshabilitarlo: normalmente siguen válidos hasta expirar. No se incluye revocación por petición en MDS.

#### Plan de implementación

Cada historia tiene un ámbito de implementación acotado e incluye pruebas y documentación. Los nombres son propuestas, no tickets creados.

| ID | Historia | Ámbito | SP | Criterios principales |
|---|---|---|---:|---|
| US-1 | Emisión Client Credentials y aislamiento | user-service | 5 | Cadena OAuth, contrato de claims, credenciales/scopes/grants inválidos rechazados, sin refresh, regresiones humanas y Prometheus. Cliente y claves de prueba; no habilitar aún en producción. |
| US-2 | Clientes desde configuración | user-service | 5 | Clientes, scopes, audiencias y TTL cargados en repositorio en memoria; secretos externos y codificados; configuración inválida rechazada; reinicio reconstruye registros. Sin esquema OAuth ni provisionamiento en BBDD. |
| US-3 | Claves duraderas, JWKS y rotación | user-service | 3 | Claves externas, issuer estable, solo claves públicas, transición por `kid`, fallo claro ante configuración crítica ausente. |
| AUTH-LIB-1 | Capacidades comunes de cliente OAuth2 y Resource Server | librería compartida | Por estimar | Componentes independientes para adquirir/reutilizar tokens y validar tokens/JWKS; configuración por consumidor; pruebas de ambos roles; sin claves privadas, `kid` ni política de permisos específica de un microservicio. |
| PF-1 | Adquisición y reutilización para MDS | portfolio-service | 5 | OAuth2 Client, identidad estable, renovación, eliminación del reenvío humano, fallo de dependencia sin fallback; selección inválida no adquiere tokens. |
| MDS-1 | Consultas internas solo para servicios | market-data-service | 5 | Resource Server, contrato completo y scope, rechazo de humanos, limpieza de autenticación anterior, errores y Prometheus preservados. |
| OPS-1 | Despliegue integrado | Infraestructura | 3 | Configuración Compose, URLs, secretos y TLS, restricciones de red, activación coordinada y verificación extremo a extremo; sin cambiar código de micros. |
| US-4 | Protección del endpoint de tokens | user-service | 3 | Limitar abuso antes del filtro de tokens, pruebas y logs seguros; necesaria antes de exposición compartida salvo protección equivalente de infraestructura. |

Base de las historias de microservicios listadas: **26 SP**, más AUTH-LIB-1, pendiente de estimación. Protección adicional del endpoint: **3 SP**, total **29 SP + AUTH-LIB-1** si se implementa en user-service. Son estimaciones relativas que deben calibrarse con el equipo. La estimación inicial de 8–13 SP para toda la integración se revisa: 13 SP encaja con el bloque emisor, no con el conjunto.

Orden: US-1 → US-2 → US-3 → AUTH-LIB-1 → PF-1 y MDS-1 (estos dos pueden desarrollarse en paralelo usando la librería); OPS-1 verifica la integración. US-4 o protección equivalente debe estar lista antes de exponer el endpoint.

Pruebas mínimas:

- Cadenas de producción y servidor servlet real para el aislamiento de filtros; login, refresh, logout, revocación y Prometheus sin regresiones.
- Clientes cargados desde configuración, restricciones de grant/scope/audiencia, ausencia de secretos en claro en los registros y comportamiento tras reinicio.
- En Portfolio, reutilización y expiración, token técnico en ambas lecturas, cero adquisición para selecciones inválidas y fallo del emisor sin fallback ni error de login humano.
- En MDS, JWT/JWKS firmados de test: firma, emisor, audiencia y expiración inválidos; ausencia de expiración; identidad humana incluso bajo el mismo emisor; permisos insuficientes; Basic no autoriza precios y bearer técnico no sustituye Prometheus.
- Reinicio y caída del emisor, disponibilidad de claves, expiración, comportamiento de errores y activación coordinada en el flujo completo.

## Preguntas abiertas

1. **Issuer y URLs por entorno:** fijar un `iss` estable y token/JWKS accesibles desde host y contenedores. Se resuelve definiendo topología y configuración, sin derivar identidad del hostname de cada petición.
2. **Claim técnico y política de clientes:** concretar nombre, valor y relación con identidad del cliente, además de cómo se asignan audiencias al crecer los destinos. Se requiere contrato emisor/receptor y pruebas negativas antes de PF-1 y MDS-1.
3. **Persistencia de autorizaciones:** los registros de cliente se cargan desde configuración y viven en memoria. Decidir por separado si se necesitan autorizaciones persistentes para introspección o revocación; persistirlas no hace que MDS revoque JWT localmente. No añadir tablas de consentimiento innecesarias.
4. **Provisionamiento y secretos:** definir valores por entorno y entrega/rotación segura de secretos; mantener credenciales productivas fuera de Git.
5. **Protección OAuth:** confirmar si rate limiting se implementa en user-service o infraestructura, con cobertura efectiva antes de exposición compartida.
6. **Parámetros operativos:** confirmar tolerancia de reloj, timeouts y límites antes del despliegue; la duración predeterminada del token es de 15 minutos y sigue siendo configurable.
