# Spike: Shared Listing Support Library

## Investigation

Tyche Wealth has existing list endpoints but no common filtering, sorting, and pagination contract.

`portfolio-service` exposes paginated portfolio and asset lists through:

- `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioApi.java`
- `services/portfolio-service/src/main/java/com/tychewealth/controller/AssetApi.java`

Their implementations use `page` and `limit`, return `ResponseEntity<List<...>>`, and include pagination metadata in headers. `PortfolioServiceImpl.listPortfolios` and `AssetServiceImpl.listAssets` already apply `PageRequest` and `Sort.by(ID)` at database level.

The future price-composition endpoint discussed in the market-data spike will return a composed in-memory list from Portfolio/MDS data. It needs frontend-friendly filtering, sorting, offset, and limit without adding that logic to each controller.

The desired API shape is:

```http
GET /prices?symbol=AAPL&sort=-price&offset=0&limit=10
```

Agreed rules:

- Query parameters other than `sort`, `offset`, and `limit` are filters over response fields.
- Filters use exact equality initially.
- Multiple filters use AND semantics.
- A non-existent filter field or a filter with no matches returns `[]`.
- A non-existent or non-sortable sort field is ignored.
- `sort=field` means ascending; `sort=-field` means descending.
- Processing order is: filter → sort → offset → limit.
- Controllers opt in through one `@Listable` annotation.
- DTOs do not need listing annotations.

## Problems and Constraints

- Existing Portfolio lists are already paginated in the repository through `PageRequest`; a response-time processor only sees one page, not the full result set.
- `PortfolioApiController` and `AssetApiController` expose `X-Total-Count`, `X-Page`, `X-Limit`, and `X-Has-Next` headers. Applying in-memory filtering after those headers are calculated would make metadata incorrect.
- Existing endpoints use `page`, while the proposed common contract uses `offset`. They must not be mixed silently.
- The parent `pom.xml` has no AOP starter. A controller annotation that transforms responses requires `spring-boot-starter-aop`.
- The only existing shared runtime module is `libraries/redis-support`; the proposed listing library needs its own reactor module.
- Generic filtering must initially support only top-level scalar response properties: strings, numbers, booleans, enums, UUIDs, and ISO dates/timestamps.
- Request parameters can collide with endpoint-specific parameters. The annotation needs a way to exclude such parameters from generic filtering.
- This capability must not replace authorization. Portfolio must resolve authorized assets/symbols before a list reaches the listing processor.

## Possible Options

### Option: Manual filtering and sorting in every controller

#### Approach

Each controller parses request parameters and delegates sorting/filtering to its own service.

#### Expected Changes

Every list endpoint would add request parameters, parsing, validation, and list transformation logic.

#### Advantages

- No new library or AOP dependency.
- Database-backed endpoints can translate criteria directly into repository queries.

#### Disadvantages

- Repeated parsing and inconsistent semantics across services.
- Controllers become responsible for list-processing logic.
- Composed responses would duplicate implementation.

### Option: Annotate DTO fields

#### Approach

Mark response DTO fields as sortable or filterable.

#### Expected Changes

Every response DTO would carry listing-specific annotations.

#### Advantages

- Explicit per-field control.

#### Disadvantages

- Couples DTOs to the generic listing mechanism.
- Requires modifying every DTO.
- Does not match the agreed controller-level ergonomics.

### Option: `@Listable` controller annotation with response-time processing

#### Approach

A Spring AOP aspect intercepts annotated controller methods, reads query parameters, and transforms direct list responses.

#### Expected Changes

Add a shared library with `@Listable`, parameter parsing, field resolution, filters, sort, pagination, and an aspect.

#### Advantages

- One annotation activates the agreed behavior.
- Controllers and DTOs remain simple.
- Fits composed/in-memory responses such as the future Portfolio price view.
- Keeps filter, sort, offset, and limit semantics consistent.

#### Disadvantages

- Requires `spring-boot-starter-aop`.
- Reflection is needed to read record components or JavaBean getters.
- Cannot correctly replace database pagination after a `PageRequest` has already been applied.

## Chosen Solution

### Decision

Create a new shared reactor module, initially for **in-memory and composed list responses**, with one `@Listable` annotation on a controller class or method.

The library processes request parameters in this order:

```text
authorized list → filters → sort → offset → limit
```

The initial consumer is expected to be the future Portfolio price-composition endpoint. Existing database-backed Portfolio list endpoints remain unchanged until the library gains a repository/JPA adapter that applies filters, sorting, and pagination before fetching records.

This keeps the initial scope compatible with the existing architecture: controllers remain thin, services preserve authorization and orchestration, and the listing concern is centralized without rewriting current JPA pagination.

### Detailed Design

#### Existing Classes to Modify

**Parent Maven reactor**  
Path: `pom.xml`

Add `libraries/listing-support` as a reactor module.

**Portfolio service POM**  
Path: `services/portfolio-service/pom.xml`

Add the listing-support dependency when Portfolio becomes the first consumer.

**Future price controller in Portfolio**

The future authenticated price endpoint will be annotated with `@Listable`. It must first resolve the user’s authorized active symbols from Portfolio data; the library only transforms the already-authorized result.

**`PortfolioApiController` and `AssetApiController`**  
Paths:

- `services/portfolio-service/src/main/java/com/tychewealth/controller/impl/PortfolioApiController.java`
- `services/portfolio-service/src/main/java/com/tychewealth/controller/impl/AssetApiController.java`

These are not initial consumers. Their services currently use database pagination and their response headers represent `Page` metadata.

#### New Classes to Create

**`Listable`**  
Suggested path: `libraries/listing-support/src/main/java/com/tychewealth/listing/Listable.java`

Controller or method annotation enabling listing processing.

```java
public @interface Listable {
    String[] ignoredParameters() default {};
}
```

**`ListingParameters`**  
Suggested path: `.../listing/ListingParameters.java`

Parses `sort`, `offset`, `limit`, and generic filter parameters.

**`SortExpression`**  
Suggested path: `.../listing/SortExpression.java`

Represents a field and `ASC`/`DESC`; a leading `-` means descending.

**`ResponseFieldResolver`**  
Suggested path: `.../listing/ResponseFieldResolver.java`

Resolves top-level record components or JavaBean getters by response-field name.

**`FilterProcessor`**  
Suggested path: `.../listing/FilterProcessor.java`

Applies exact scalar filters. Missing fields, invalid scalar conversion, or no matches produce an empty list.

**`SortProcessor`**  
Suggested path: `.../listing/SortProcessor.java`

Sorts a list by a resolved comparable field. Unknown or non-comparable sort fields leave the list unchanged.

**`PaginationProcessor`**  
Suggested path: `.../listing/PaginationProcessor.java`

Applies non-negative offset and optional limit after sorting.

**`ListableAspect`**  
Suggested path: `.../listing/ListableAspect.java`

Spring AOP component that intercepts `@Listable` controllers and handles `List<?>` and `ResponseEntity<List<?>>` responses.

#### Component Interaction and Flow

1. An authenticated Portfolio controller invokes its normal service flow.
2. The service applies domain authorization and returns an authorized list.
3. `ListableAspect` reads the query parameter map.
4. `ListingParameters` reserves `sort`, `offset`, and `limit`, excluding annotation-defined endpoint parameters.
5. `FilterProcessor` applies exact filters.
6. `SortProcessor` applies requested sort when the field exists and is comparable.
7. `PaginationProcessor` applies offset and limit.
8. The aspect returns the transformed list, preserving the original `ResponseEntity` status and headers.

#### Implementation Outline

1. Add the `listing-support` module, skipping Spring Boot repackaging like `redis-support`.
2. Add Spring AOP support to the module/consumers.
3. Implement and test parsing, field resolution, filtering, sorting, and pagination independently.
4. Add `ListableAspect`.
5. Add tests for records and JavaBean DTOs.
6. Use the library first from the future Portfolio price endpoint.
7. Design a separate JPA adapter before applying the annotation to existing Portfolio/Asset paged endpoints.

## Open Questions

- What maximum `limit` should the library enforce, and should it have a default?
- Should malformed `offset`/`limit` be ignored, clamped, or returned as `400 Bad Request`?
- Should repeated filters such as `?symbol=AAPL&symbol=MSFT` use OR semantics?
- Should JSON aliases such as `@JsonProperty` be supported, or only Java response-property names?
- What response envelope and metadata should offset-based endpoints expose?
- How should a future JPA adapter map allowed response fields to repository/query fields without exposing arbitrary database sorting?

## Reference API Shapes

```java
@Listable
@GetMapping("/prices")
ResponseEntity<List<PriceResponse>> prices() {
    return ResponseEntity.ok(priceService.getPrices());
}
```

```java
public record SortExpression(String field, Direction direction) {
    enum Direction { ASC, DESC }
}
```

```java
public record ListingParameters(
    Map<String, String> filters,
    SortExpression sort,
    int offset,
    Integer limit) {}
```

```java
List<?> filtered = filterProcessor.apply(items, parameters.filters());
List<?> sorted = sortProcessor.apply(filtered, parameters.sort());
List<?> page = paginationProcessor.apply(sorted, parameters.offset(), parameters.limit());
```
