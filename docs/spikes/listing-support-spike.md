# Spike: Shared Listing Support Library

## Status

Implemented in `libraries/listing-support`, with MVC and JPA/H2 tests. No production service currently depends on the module or uses its annotations.

This document replaces the initial response-time AOP proposal with the design actually implemented. Filtering, sorting, and pagination are applied by the consuming application before fetching results; only response field selection runs after the controller returns.

## Investigation

Portfolio already has database-paginated list endpoints. A generic processor that filters a returned page would operate on an incomplete result set and invalidate counts and navigation metadata. The future composed price endpoint also needs a consistent listing contract, but that does not justify silently rewriting every response.

The implemented library separates three responsibilities:

- Parse and validate HTTP listing criteria against response DTO metadata.
- Adapt those criteria to JPA specifications and Spring Data pagination.
- Optionally project JSON responses onto requested fields.

Authorization, query execution, result mapping, and response construction remain application responsibilities.

## Options Considered

### Manual parsing in each controller

Simple locally, but duplicates validation, pagination defaults, and filter syntax across endpoints. Not selected as the shared approach.

### Response-time AOP filtering

The initial spike proposed `ListableAspect` and in-memory filter/sort processors. This would be convenient for complete in-memory lists, but is incorrect for already-paginated database results and hides execution behind response interception.

This proposal was superseded. There is no `ListableAspect` or generic in-memory listing executor in the implementation. The module POM still declares the AOP starter, but that dependency does not imply that an aspect exists.

### MVC argument resolution with an explicit query adapter — selected

A handler opts in with `@Listable(mapping = ...)` and receives a `ListQuery` argument. The service explicitly applies filters and paging to its query. DTO annotations define allowed public fields; an application mapping separately whitelists persistence paths.

This adds explicit integration work at the service boundary, in exchange for visible query execution and correct database filtering before pagination.

## Chosen Solution

### Module and Spring Integration

- Maven module: `libraries/listing-support`.
- Artifact: `com.ismaelperezdev:tyche-wealth-listing-support`.
- Shared runtime library, not a bootable application; Boot repackaging is skipped.
- `ListingSupportConfiguration` enables `FilterProperties` and registers `ListQueryArgumentResolver` with MVC.
- Consumers must make the configuration and required components available through scanning/imports. The library does not provide Boot auto-configuration discovery.

### Component Responsibilities

| Component | Responsibility |
| --- | --- |
| `Listable` | Method-level opt-in with a required `FilterFieldMapping` class declaration. |
| `ListQueryArgumentResolver` | Resolves `ListQuery` arguments, determines response DTO type, and adapts legacy `page` requests. |
| `FilterParser` | Parses filters and pagination; validates filter/sort names against DTO metadata. |
| `Filterable`, `ExcludeFilter` | Declare permitted DTO fields and per-field operator restrictions. |
| `ListQuery`, `ApiFilter`, `PageCriteria`, `SortCriteria` | Carry parsed criteria without executing a query. |
| `FilterFieldMapping` | Application-defined API-to-persistence field whitelist and optional value transformers. |
| `JpaListingSpecificationBuilder` | Converts filters to typed JPA predicates. |
| `OffsetBasedPageRequest` | Adapts arbitrary row offsets and sort criteria to Spring Data `Pageable`. |
| `SortBuilder`, `Range` | Parse sort expressions and inclusive item ranges. |
| `PaginationHttpHeaders` | Builds `X-Total-Count` and navigation `Link` headers when explicitly called. |
| `FieldFiltered`, `FieldFilterAdvice`, `NestedPropertyFilter` | Opt-in JSON field selection, including nested objects and arrays. |
| `FilterException`, `ErrorDefinition` | Typed failures and stable `TYCHE-91xx` error definitions. |
| `domain.Page` | Optional result/count carrier; not automatically constructed by the resolver. |

### Execution Flow

```text
HTTP request
  -> @Listable handler's ListQuery argument
  -> MVC resolver + DTO metadata validation
  -> application service / authorization constraints
  -> JPA specification + offset-based Pageable
  -> database filtering, ordering, and pagination
  -> DTO mapping and explicit response/header construction
  -> optional fields projection
```

The resolver unwraps `ResponseEntity`, Spring Data `Page`, and collection return types, and handles arrays, to find the response DTO. An indeterminate response type is an endpoint configuration error.

**Important:** `@Listable.mapping()` is metadata, not automatic mapping instantiation or query execution. The consuming service must supply the intended `FilterFieldMapping` to both the specification builder and mapped pageable constructor. The annotation alone does not filter a result list or add pagination headers.

## Implemented Request Contract

### Filterable Fields

- Type-level `@Filterable` permits its fields with all operators.
- Field-level `@Filterable(operators = ...)` overrides that default; its default operator is `EQ`.
- `@ExcludeFilter` removes a field from the allowed filter/sort names and prevents nested traversal through it.
- Metadata is discovered from Java fields, including inherited fields. JSON aliases are not resolved.
- Nested paths such as `details.category` are supported when the nested DTO is type-level filterable.
- The parser validates public names; the JPA adapter independently requires explicit mapping entries. API names need not match entity property names.

### Filtering and Conversion

Examples:

```http
GET /assets?symbol=AAPL&quantity.gte=2&sort=-symbol&offset=0&limit=10
GET /assets?symbol.in=AAPL,MSFT&details.category=Technology
GET /assets?acquiredOn=2023-01-10&status=ACTIVE
```

- Bare names imply `EQ`; suffixes support `eq`, `neq`, `gt`, `gte`, `lt`, `lte`, `like`, and `in`.
- The parser also recognizes comparison tokens embedded in parameter names (`>=`, `<=`, `>`, `<`, `*=`). Use suffix notation for ordinary HTTP examples to avoid URL encoding ambiguity.
- Values split on commas or semicolons and are trimmed; blank filter values are skipped.
- Different filters combine with AND. Multiple values for `EQ` and `LIKE` combine with OR; `NEQ` requires every inequality. `IN` uses membership.
- Ordered comparisons require exactly one comparable value. `LIKE` requires a string field and performs case-insensitive literal substring matching; backslashes, percent signs, and underscores are escaped before constructing the SQL LIKE pattern.
- The resolver uses the first value of repeated query parameters. Use delimiter-separated values for alternatives; repeated keys are not collected as OR conditions.
- Mapping value transformers run before conversion to the entity field type through `DefaultFormattingConversionService`. Tests cover numeric, ISO `LocalDate`, and enum conversion and invalid typed values.
- There is no globally reserved null token. A mapping may explicitly transform a string such as `"null"` into Java null. `EQ`/`NEQ` then generate `IS NULL`/`IS NOT NULL`; ordinary SQL inequality does not include rows with null values.
- Unmapped filters, invalid typed values, disallowed operators, and unknown nonblank filter keys are rejected, not silently turned into empty results. Valid queries with no matches return no rows.

### Sorting and Pagination

- `sort=symbol,-name,+status` preserves the requested ordering sequence. Bare/`+` fields ascend; `-` fields descend. Blank tokens are ignored.
- Sort keys must be allowed by DTO metadata. The mapped pageable constructor also checks the persistence whitelist.
- The unmapped pageable constructor uses sort keys directly; use the mapped constructor for public API-to-entity translation.
- Default offset is zero. Negative offsets, limits below one, and nonnumeric paging values raise `FilterException`; values above configured maxima are capped.
- `page` is zero-based compatibility input: without an explicit offset, it becomes `max(0, page) * limit`, capped at the maximum offset. With an explicit offset, `page` is ignored.
- `Range: items=20-29` supplies offset 20 and limit 10 only when neither offset nor limit is present and legacy page mode is inactive. Invalid Range syntax is logged and falls back to defaults.
- `Range.withTotalElements(...)` and `toContentRangeHeader()` support explicit response construction. Neither Range nor pagination headers are automatically written by the resolver.

Default `filter.*` properties:

| Property | Default |
| --- | ---: |
| `max-offset` | 100000 |
| `max-limit` | 5000 |
| `default-limit` | 20 |
| `max-filters` | 20 |
| `max-nested-depth` | 2 |
| `max-sorts` | 10 |

### Response Field Selection

`fields=symbol,details.category` is reserved by the parser and handled separately by `FieldFilterAdvice` for DTOs marked `@FieldFiltered`.

The advice projects serialized JSON, recursively retaining requested paths in objects and arrays. Requesting a complete property preserves its full value. Without a nonempty selection or a marked response type, the body is unchanged. This is response projection, not query filtering, database column selection, or authorization.

### Error Boundary

The library supplies `FilterException` with `ErrorDefinition`, description data, and an intended HTTP status. The consuming service must translate it with its own error handler. The integration fixture verifies representative bad requests, but the library does not install a production-wide error handler.

## Integration Example

The endpoint shape is now:

```java
@Listable(mapping = AssetFilterMapping.class)
@GetMapping("/assets")
ResponseEntity<List<AssetResponse>> listAssets(ListQuery query) {
    return assetService.listAssets(query);
}
```

The service combines authorization constraints with listing criteria before fetching rows. The core adapter calls are:

```java
Specification<Asset> listing = specificationBuilder.fromFilters(query.filter(), mapping);
Pageable pageable = new OffsetBasedPageRequest(query.pageCriteria(), mapping);
```

The application then executes the query, maps entities, calculates matching totals where needed, and builds the response. These snippets illustrate integration; no production service has been migrated as part of this spike.

## Validation and CI

Tests cover parsing, DTO/operator validation, pagination and Range compatibility, field projection, field mappings, and offset-based Spring Data adaptation. JPA tests use H2; an MVC + JPA integration fixture exercises nested filters, typed dates/enums, invalid requests, and mapping-defined null behavior.

Build and tests are integrated into the existing `.github/workflows/shared-changes.yml`, triggered by `libraries/**` changes on pull requests and main pushes. No dedicated library workflow is used. `.coderabbit.yaml` includes `libraries/listing-support/**` and excludes its build output; CodeRabbit reviews run through its integration, not a workflow invocation.

Local checks from the repository root (Java 21):

```powershell
.\mvnw.cmd -pl libraries/listing-support -am spotless:check
.\mvnw.cmd -pl libraries/listing-support -am -DskipTests package
.\mvnw.cmd -pl libraries/listing-support -am test
```

## Accepted Trade-offs and Remaining Work

- DTO annotations couple public listing eligibility to response models, deliberately replacing the original annotation-free DTO proposal.
- Field mappings add explicit per-application configuration in exchange for keeping entity paths out of the public contract.
- Query execution and response metadata are explicit, rather than hidden behind an aspect.
- Production service adoption, error-handler wiring, and response metadata conventions remain separate integration work.
- Endpoint-specific ignored parameters are not implemented; additional nonreserved parameters are processed as filters.
- There is no generic in-memory executor for composed price responses. Such an endpoint must explicitly apply its criteria or add an adapter if required.
- Nested JPA paths use property traversal; arbitrary joins and collection-filter semantics are not provided as a general framework.
- DTO metadata is cached per parser instance for root DTOs only. Nested metadata is computed at its actual recursion depth; static and synthetic fields are excluded along with fields marked `@ExcludeFilter`.
- The POM description/AOP dependency still reflect the initial proposal and can be cleaned up independently; this documentation update does not change dependencies.
