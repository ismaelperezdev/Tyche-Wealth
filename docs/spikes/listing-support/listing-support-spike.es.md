# Spike: Biblioteca compartida de soporte para listados

English version: [listing-support-spike.en.md](listing-support-spike.en.md).

## Estado

Implementada en `libraries/listing-support`, con pruebas MVC y JPA/H2. Actualmente ningún servicio de producción depende del módulo ni utiliza sus anotaciones.

Este documento sustituye la propuesta inicial de AOP en tiempo de respuesta por el diseño implementado. La aplicación consumidora aplica filtros, ordenación y paginación antes de consultar resultados; solo la selección de campos de la respuesta se ejecuta después de que retorna el controlador.

## Investigación

Portfolio ya tiene endpoints de listado paginados por base de datos. Un procesador genérico que filtre una página ya obtenida operaría sobre un resultado incompleto e invalidaría los conteos y metadatos de navegación. El futuro endpoint compuesto de precios también necesita un contrato de listado consistente, pero eso no justifica reescribir respuestas de forma implícita.

La biblioteca implementada separa tres responsabilidades:

- Analizar y validar criterios HTTP de listado contra los metadatos de los DTO de respuesta.
- Adaptar esos criterios a especificaciones JPA y paginación de Spring Data.
- Proyectar opcionalmente respuestas JSON a los campos solicitados.

La autorización, ejecución de consultas, mapeo de resultados y construcción de respuestas siguen siendo responsabilidad de cada aplicación.

## Opciones consideradas

### Análisis manual en cada controlador

Es sencillo localmente, pero duplica validación, valores predeterminados de paginación y sintaxis de filtros entre endpoints. No se seleccionó como enfoque compartido.

### Filtrado AOP en tiempo de respuesta

El spike inicial proponía `ListableAspect` y procesadores de filtrado/ordenación en memoria. Sería conveniente para listas completas en memoria, pero es incorrecto para resultados ya paginados en la base de datos y oculta la ejecución detrás de la interceptación de respuesta.

La propuesta fue sustituida. La implementación no tiene `ListableAspect` ni un ejecutor genérico de listados en memoria. El POM del módulo aún declara el starter AOP, pero esa dependencia no implica que exista un aspecto.

### Resolución de argumentos MVC con adaptador explícito de consulta — seleccionada

Un handler opta por el comportamiento con `@Listable(mapping = ...)` y recibe un argumento `ListQuery`. El servicio aplica explícitamente filtros y paginación a su consulta. Las anotaciones del DTO definen campos públicos permitidos; un mapping de la aplicación crea por separado la lista blanca de rutas de persistencia.

Esto requiere integración explícita en el límite del servicio, a cambio de una ejecución de consultas visible y filtrado correcto en la base de datos antes de paginar.

## Solución elegida

### Módulo e integración con Spring

- Módulo Maven: `libraries/listing-support`.
- Artefacto: `com.ismaelperezdev:tyche-wealth-listing-support`.
- Biblioteca de ejecución compartida, no es una aplicación ejecutable; se omite el reempaquetado de Boot.
- `ListingSupportConfiguration` habilita `FilterProperties` y registra `ListQueryArgumentResolver` con MVC.
- Los consumidores deben hacer disponible la configuración y los componentes necesarios mediante escaneo/importaciones. La biblioteca no ofrece descubrimiento de auto-configuración de Boot.

### Responsabilidades de componentes

| Componente | Responsabilidad |
| --- | --- |
| `Listable` | Opt-in a nivel de método que exige declarar una clase `FilterFieldMapping`. |
| `ListQueryArgumentResolver` | Resuelve argumentos `ListQuery`, determina el tipo DTO de respuesta y adapta solicitudes heredadas con `page`. |
| `FilterParser` | Analiza filtros y paginación; valida nombres de filtros/ordenación contra metadatos DTO. |
| `Filterable`, `ExcludeFilter` | Declaran campos DTO permitidos y restricciones de operadores por campo. |
| `ListQuery`, `ApiFilter`, `PageCriteria`, `SortCriteria` | Transportan criterios analizados sin ejecutar consultas. |
| `FilterFieldMapping` | Lista blanca de campos API-a-persistencia definida por la aplicación y transformadores opcionales de valores. |
| `JpaListingSpecificationBuilder` | Convierte filtros en predicados JPA tipados. |
| `OffsetBasedPageRequest` | Adapta offsets arbitrarios por fila y criterios de orden a `Pageable` de Spring Data. |
| `SortBuilder`, `Range` | Analizan expresiones de orden y rangos inclusivos de elementos. |
| `PaginationHttpHeaders` | Construye encabezados `X-Total-Count` y enlaces de navegación cuando se invoca explícitamente. |
| `FieldFiltered`, `FieldFilterAdvice`, `NestedPropertyFilter` | Selección opt-in de campos JSON, incluidos objetos y arrays anidados. |
| `FilterException`, `ErrorDefinition` | Errores tipados y definiciones estables `TYCHE-91xx`. |
| `domain.Page` | Contenedor opcional de resultado/conteo; el resolver no lo construye automáticamente. |

### Flujo de ejecución

```text
Solicitud HTTP
  -> argumento ListQuery del handler anotado con @Listable
  -> resolver MVC + validación de metadatos DTO
  -> servicio de aplicación / restricciones de autorización
  -> especificación JPA + Pageable por offset
  -> filtrado, ordenación y paginación en base de datos
  -> mapeo DTO y construcción explícita de respuesta/encabezados
  -> proyección opcional de campos
```

El resolver desenvuelve tipos de retorno `ResponseEntity`, `Page` de Spring Data y colecciones, y maneja arrays para encontrar el DTO de respuesta. Un tipo de respuesta indeterminado es un error de configuración del endpoint.

**Importante:** `@Listable.mapping()` es metadato, no instancia automáticamente un mapping ni ejecuta consultas. El servicio consumidor debe proporcionar el `FilterFieldMapping` previsto tanto al constructor de especificaciones como al constructor de paginación con mapping. La anotación por sí sola no filtra una lista ni añade encabezados de paginación.

## Contrato de solicitud implementado

### Campos filtrables

- `@Filterable` a nivel de tipo permite sus campos con todos los operadores.
- `@Filterable(operators = ...)` a nivel de campo reemplaza ese valor; su operador predeterminado es `EQ`.
- `@ExcludeFilter` elimina un campo de los nombres permitidos para filtrar/ordenar e impide recorrerlo como propiedad anidada.
- Los metadatos se descubren en campos Java, incluidos los heredados. No se resuelven alias JSON.
- Se admiten rutas anidadas como `details.category` cuando el DTO anidado es filtrable a nivel de tipo.
- El parser valida nombres públicos; el adaptador JPA exige de forma independiente mappings explícitos. Los nombres API no necesitan coincidir con propiedades de entidad.

### Filtrado y conversión

Ejemplos:

```http
GET /assets?symbol=AAPL&quantity.gte=2&sort=-symbol&offset=0&limit=10
GET /assets?symbol.in=AAPL,MSFT&details.category=Technology
GET /assets?acquiredOn=2023-01-10&status=ACTIVE
```

- Los nombres sin sufijo implican `EQ`; se admiten los sufijos `eq`, `neq`, `gt`, `gte`, `lt`, `lte`, `like` e `in`.
- El parser también reconoce operadores de comparación dentro del nombre (`>=`, `<=`, `>`, `<`, `*=`). Para ejemplos HTTP normales se recomienda la notación con sufijo y así evitar ambigüedades de codificación URL.
- Los valores se separan por comas o punto y coma y se recortan; se omiten valores de filtro vacíos.
- Filtros distintos se combinan con AND. Varios valores para `EQ` y `LIKE` se combinan con OR; `NEQ` exige que se cumplan todas las desigualdades. `IN` usa pertenencia.
- Las comparaciones ordenadas requieren exactamente un valor comparable. `LIKE` requiere un campo string y realiza búsqueda literal de subcadena sin distinguir mayúsculas; escapa barras inversas, porcentajes y guiones bajos antes de construir el patrón SQL LIKE.
- El resolver usa el primer valor de parámetros de consulta repetidos. Para alternativas, usar valores separados por delimitador; las claves repetidas no se reúnen como condiciones OR.
- Los transformadores de valores del mapping se ejecutan antes de convertir al tipo del campo de entidad mediante `DefaultFormattingConversionService`. Hay pruebas para números, `LocalDate` ISO, enums y valores tipados inválidos.
- No existe un token nulo reservado globalmente. Un mapping puede convertir explícitamente una cadena como `"null"` a null Java. `EQ`/`NEQ` generan entonces `IS NULL`/`IS NOT NULL`; la desigualdad SQL ordinaria no incluye filas con valores null.
- Los filtros sin mapping, valores tipados inválidos, operadores no permitidos y claves no vacías desconocidas se rechazan, no se convierten silenciosamente en resultados vacíos. Las consultas válidas sin coincidencias devuelven cero filas.

### Ordenación y paginación

- `sort=symbol,-name,+status` conserva el orden solicitado. Campos sin prefijo o con `+` son ascendentes; `-` indica descendente. Se ignoran tokens vacíos.
- Las claves de orden deben estar permitidas por los metadatos DTO. El constructor pageable con mapping también valida la lista blanca de persistencia.
- El constructor pageable sin mapping usa directamente las claves de orden; para traducir API a entidad se debe usar el constructor con mapping.
- El offset predeterminado es cero. Offsets negativos, límites inferiores a uno y valores de paginación no numéricos generan `FilterException`; los valores por encima de los máximos configurados se limitan.
- `page` es una entrada de compatibilidad basada en cero: sin `offset` explícito se convierte en `max(0, page) * limit`, limitado al offset máximo. Si hay `offset` explícito, `page` se ignora.
- `Range: items=20-29` define offset 20 y límite 10 solo si no se indicaron offset ni límite y no está activo el modo `page` heredado. La sintaxis Range inválida se registra en logs y usa valores predeterminados.
- `Range.withTotalElements(...)` y `toContentRangeHeader()` permiten construir explícitamente una respuesta. Ni `Range` ni los encabezados de paginación se escriben automáticamente desde el resolver.

Propiedades predeterminadas `filter.*`:

| Propiedad | Valor predeterminado |
| --- | ---: |
| `max-offset` | 100000 |
| `max-limit` | 5000 |
| `default-limit` | 20 |
| `max-filters` | 20 |
| `max-nested-depth` | 2 |
| `max-sorts` | 10 |

### Selección de campos de respuesta

`fields=symbol,details.category` está reservado por el parser y se procesa por separado en `FieldFilterAdvice` para DTO anotados con `@FieldFiltered`.

El advice proyecta el JSON serializado y conserva recursivamente rutas solicitadas en objetos y arrays. Solicitar una propiedad completa conserva su valor íntegro. Sin selección no vacía o sin un tipo de respuesta marcado, el body no cambia. Esto es proyección de respuesta, no filtrado de consultas, selección de columnas ni autorización.

### Límite de errores

La biblioteca proporciona `FilterException` con `ErrorDefinition`, datos descriptivos y un estado HTTP previsto. El servicio consumidor debe traducirla mediante su propio manejador de errores. La fixture de integración verifica solicitudes incorrectas representativas, pero la biblioteca no instala un manejador global de producción.

## Ejemplo de integración

La forma del endpoint es:

```java
@Listable(mapping = AssetFilterMapping.class)
@GetMapping("/assets")
ResponseEntity<List<AssetResponse>> listAssets(ListQuery query) {
    return assetService.listAssets(query);
}
```

El servicio combina las restricciones de autorización con los criterios de listado antes de consultar filas. Las llamadas principales al adaptador son:

```java
Specification<Asset> listing = specificationBuilder.fromFilters(query.filter(), mapping);
Pageable pageable = new OffsetBasedPageRequest(query.pageCriteria(), mapping);
```

La aplicación ejecuta la consulta, mapea entidades, calcula los totales coincidentes cuando se requieren y construye la respuesta. Estos ejemplos ilustran la integración; este spike no migró ningún servicio de producción.

## Validación y CI

Las pruebas cubren análisis, validación DTO/operadores, compatibilidad de paginación y Range, proyección de campos, mappings y adaptación offset-based de Spring Data. Las pruebas JPA usan H2; una fixture de integración MVC + JPA ejercita filtros anidados, fechas/enums tipados, solicitudes inválidas y comportamiento de null definido por mapping.

Las pruebas y compilación están integradas en `.github/workflows/shared-changes.yml`, que se activa con cambios en `libraries/**` en pull requests y pushes a main. No hay un workflow dedicado a esta biblioteca. `.coderabbit.yaml` incluye `libraries/listing-support/**` y excluye su salida de compilación; las revisiones de CodeRabbit se ejecutan mediante su integración, no desde un workflow.

Comprobaciones locales desde la raíz del repositorio (Java 21):

```powershell
.\mvnw.cmd -pl libraries/listing-support -am spotless:check
.\mvnw.cmd -pl libraries/listing-support -am -DskipTests package
.\mvnw.cmd -pl libraries/listing-support -am test
```

## Compromisos aceptados y trabajo restante

- Las anotaciones DTO acoplan la elegibilidad del listado público a los modelos de respuesta, reemplazando deliberadamente la propuesta original de DTO sin anotaciones.
- Los mappings de campos añaden configuración explícita por aplicación a cambio de mantener las rutas de entidad fuera del contrato público.
- La ejecución de consulta y los metadatos de respuesta son explícitos, no están ocultos detrás de un aspect.
- La adopción por servicios de producción, integración de manejadores de errores y convenciones de metadatos de respuesta son trabajo de integración independiente.
- No se implementan parámetros ignorados específicos por endpoint; los parámetros adicionales no reservados se procesan como filtros.
- No existe un ejecutor genérico en memoria para respuestas compuestas de precios. Ese endpoint debe aplicar explícitamente sus criterios o añadir un adaptador si lo necesita.
- Las rutas JPA anidadas usan recorrido de propiedades; no se ofrece como framework general soporte para joins arbitrarios ni semántica de filtros sobre colecciones.
- Los metadatos DTO se almacenan en caché por instancia del parser solo para DTO raíz. Los anidados se calculan a su profundidad real de recursión; se excluyen campos estáticos/sintéticos y los marcados con `@ExcludeFilter`.
- La descripción del POM y la dependencia AOP aún reflejan la propuesta inicial y pueden limpiarse independientemente; esta actualización documental no cambia dependencias.
