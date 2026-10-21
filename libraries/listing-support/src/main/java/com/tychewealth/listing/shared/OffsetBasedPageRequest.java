package com.tychewealth.listing.shared;

import com.tychewealth.listing.domain.PageCriteria;
import com.tychewealth.listing.domain.SortCriteria;
import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.exception.FilterException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

/**
 * Spring Data {@link Pageable} implementation for offsets that are not page-size multiples.
 *
 * <p>Unlike the standard page-number model, {@link #getOffset()} retains the requested row offset.
 * Constructors reject negative offsets and limits below one; an optional field mapping translates
 * API sort keys to persistence property names.
 */
public class OffsetBasedPageRequest implements Pageable {

  private final int limit;
  private final long offset;
  private final Sort sort;

  public OffsetBasedPageRequest(long offset, int limit) {
    this(offset, limit, Sort.unsorted());
  }

  private OffsetBasedPageRequest(long offset, int limit, Sort sort) {
    if (offset < 0) {
      throw new FilterException(ErrorDefinition.PAGINATION_INVALID_OFFSET, HttpStatus.BAD_REQUEST);
    }

    if (limit < 1) {
      throw new FilterException(ErrorDefinition.PAGINATION_INVALID_LIMIT, HttpStatus.BAD_REQUEST);
    }

    this.offset = offset;
    this.limit = limit;
    this.sort = sort;
  }

  public OffsetBasedPageRequest(PageCriteria criteria) {
    this(criteria.offset(), criteria.limit(), toSort(criteria));
  }

  public OffsetBasedPageRequest(PageCriteria criteria, FilterFieldMapping fieldMapping) {
    this(criteria.offset(), criteria.limit(), toSort(criteria, fieldMapping));
  }

  private static Sort toSort(PageCriteria criteria) {
    Objects.requireNonNull(criteria, "criteria must not be null");
    if (criteria.sorts() == null || criteria.sorts().isEmpty()) {
      return Sort.unsorted();
    }
    return Sort.by(
        criteria.sorts().stream()
            .map(
                sort ->
                    new Sort.Order(
                        sort.direction() == com.tychewealth.listing.enums.SortDirection.DESC
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC,
                        sort.field()))
            .toList());
  }

  private static Sort toSort(PageCriteria criteria, FilterFieldMapping fieldMapping) {
    Objects.requireNonNull(criteria, "criteria must not be null");
    Objects.requireNonNull(fieldMapping, "fieldMapping must not be null");
    if (criteria.sorts() == null || criteria.sorts().isEmpty()) {
      return Sort.unsorted();
    }
    return Sort.by(criteria.sorts().stream().map(sort -> toOrder(sort, fieldMapping)).toList());
  }

  private static Sort.Order toOrder(SortCriteria sort, FilterFieldMapping fieldMapping) {
    if (!fieldMapping.hasFieldMapping(sort.field())) {
      throw new FilterException(
          ErrorDefinition.FILTER_FIELD_NOT_FOUND,
          new Object[] {sort.field()},
          HttpStatus.BAD_REQUEST);
    }
    Sort.Direction direction =
        sort.direction() == com.tychewealth.listing.enums.SortDirection.DESC
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
    return new Sort.Order(direction, fieldMapping.resolveFieldName(sort.field()));
  }

  @Override
  public boolean isPaged() {
    return Pageable.super.isPaged();
  }

  @Override
  public boolean isUnpaged() {
    return Pageable.super.isUnpaged();
  }

  @Override
  public int getPageNumber() {
    return Math.toIntExact(offset / limit);
  }

  @Override
  public int getPageSize() {
    return limit;
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  public Sort getSort() {
    return sort;
  }

  @Override
  public Sort getSortOr(Sort sort) {
    return Pageable.super.getSortOr(sort);
  }

  @Override
  public Pageable next() {
    return new OffsetBasedPageRequest(Math.addExact(offset, limit), limit, sort);
  }

  @Override
  public Pageable previousOrFirst() {
    return hasPrevious()
        ? new OffsetBasedPageRequest(Math.max(0, offset - limit), limit, sort)
        : first();
  }

  @Override
  public Pageable first() {
    return new OffsetBasedPageRequest(0, limit, sort);
  }

  @Override
  public Pageable withPage(int pageNumber) {
    if (pageNumber < 0) {
      throw new IllegalArgumentException("Page index must not be less than zero");
    }
    return new OffsetBasedPageRequest(Math.multiplyExact((long) pageNumber, limit), limit, sort);
  }

  @Override
  public boolean hasPrevious() {
    return offset > 0;
  }

  @Override
  public Optional<Pageable> toOptional() {
    return Pageable.super.toOptional();
  }

  @Override
  public Limit toLimit() {
    return Pageable.super.toLimit();
  }

  @Override
  public OffsetScrollPosition toScrollPosition() {
    return Pageable.super.toScrollPosition();
  }
}
