package com.tychewealth.service.helper;

import static com.tychewealth.constants.CommonConstants.SYMBOL_QUERY_PARAM;
import static com.tychewealth.constants.MarketPriceFieldConstants.CAPTURED_AT;
import static com.tychewealth.constants.MarketPriceFieldConstants.ID;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.InvalidMarketPriceQueryException;
import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.domain.PageCriteria;
import com.tychewealth.listing.domain.SortCriteria;
import com.tychewealth.listing.enums.FilterOperator;
import com.tychewealth.listing.enums.SortDirection;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.listing.filter.ListingUtils;
import com.tychewealth.listing.jpa.JpaListingSpecificationBuilder;
import com.tychewealth.listing.shared.OffsetBasedPageRequest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** Builds the persistence query components for market-price history listings. */
@Component
@RequiredArgsConstructor
public class MarketPriceReadHelper {

  private final JpaListingSpecificationBuilder specificationBuilder;
  private final MarketPriceFilterFieldMapping fieldMapping;
  private final MarketPriceLatestFilterFieldMapping latestFieldMapping;
  private final MarketPriceQueryValidationHelper validationHelper;

  public Page<String> pageLatestSymbols(ListQuery query) {
    List<String> symbols = new ArrayList<>(requiredSymbols(query.filter()));
    OffsetBasedPageRequest pageable =
        ListingUtils.pageRequest(
            query.pageCriteria(),
            latestFieldMapping,
            new SortCriteria(SYMBOL_QUERY_PARAM, SortDirection.ASC));
    var symbolSort = pageable.getSort().getOrderFor(SYMBOL_QUERY_PARAM);
    Comparator<String> comparator = Comparator.naturalOrder();
    if (symbolSort != null && symbolSort.isDescending()) {
      comparator = comparator.reversed();
    }
    symbols.sort(comparator);

    int start = (int) Math.min(pageable.getOffset(), symbols.size());
    int end = (int) Math.min((long) start + pageable.getPageSize(), symbols.size());
    return new PageImpl<>(symbols.subList(start, end), pageable, symbols.size());
  }

  public void validateHistoryFilters(List<ApiFilter> filters) {
    requiredSymbols(filters);

    Instant from =
        ListingUtils.findFilters(
                filters, CAPTURED_AT, Set.of(FilterOperator.GT, FilterOperator.GTE))
            .stream()
            .map(this::parseTimeFilter)
            .max(Comparator.naturalOrder())
            .orElse(null);
    Instant to =
        ListingUtils.findFilters(
                filters, CAPTURED_AT, Set.of(FilterOperator.LT, FilterOperator.LTE))
            .stream()
            .map(this::parseTimeFilter)
            .min(Comparator.naturalOrder())
            .orElse(null);
    if (from != null && to != null) {
      validationHelper.validateTimeRange(from, to);
    }
  }

  private List<String> requiredSymbols(List<ApiFilter> filters) {
    List<ApiFilter> symbolFilters =
        filters.stream().filter(filter -> SYMBOL_QUERY_PARAM.equals(filter.field())).toList();
    if (symbolFilters.isEmpty()) {
      throw new InvalidMarketPriceQueryException(
          ErrorDefinition.MARKET_PRICE_SYMBOL_FILTER_REQUIRED);
    }

    Set<String> matchingSymbols = null;
    for (ApiFilter symbolFilter : symbolFilters) {
      Set<String> filterSymbols =
          new LinkedHashSet<>(validationHelper.normalizeSymbols(symbolFilter.values()));
      if (matchingSymbols == null) {
        matchingSymbols = filterSymbols;
      } else {
        matchingSymbols.retainAll(filterSymbols);
      }
    }
    return List.copyOf(matchingSymbols);
  }

  private Instant parseTimeFilter(ApiFilter filter) {
    if (filter == null) {
      return null;
    }
    try {
      return Instant.parse(filter.singleValue());
    } catch (DateTimeParseException exception) {
      throw new InvalidMarketPriceQueryException(ErrorDefinition.MARKET_PRICE_CAPTURED_AT_INVALID);
    }
  }

  public Specification<MarketPriceCheckpointEntity> specification(ListQuery query) {
    return specificationBuilder.fromFilters(query.filter(), fieldMapping);
  }

  public OffsetBasedPageRequest pageRequest(PageCriteria criteria) {
    return ListingUtils.pageRequest(
        criteria,
        fieldMapping,
        new SortCriteria(ID, SortDirection.ASC),
        new SortCriteria(ID, SortDirection.ASC));
  }
}
