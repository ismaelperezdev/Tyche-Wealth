# Listing Support

Shared support for parsing HTTP listing parameters into filtering, sorting, and pagination criteria.
It separates the API query format from each service's entities and persistence queries.

## Features

- `ListQueryArgumentResolver` creates a `ListQuery` for handler methods annotated with `@Listable`.
- `FilterParser` parses filters, sorting, `offset`/`limit` pagination, and the `Range` header.
- `JpaListingSpecificationBuilder` converts filters to JPA specifications. Queryable fields must be
  explicitly allowed with `FilterFieldMapping`.
- `filter.*` properties limit the size and complexity of client-provided queries.

## Integration

Add `tyche-wealth-listing-support` as a Maven dependency and import the required components in the
MVC application. For example:

```java
@Import({
  ListingSupportConfiguration.class,
  ListQueryArgumentResolver.class,
  FilterParser.class,
  JpaListingSpecificationBuilder.class
})
```

Annotate handlers that accept `ListQuery` with `@Listable(mapping = YourFilterMapping.class)`. Each
service defines its own mapping from public API fields to persistence fields and decides how to run
the query; fields that are not allowed are rejected.

This module provides reusable support, not endpoints or service-specific listing rules.
