package com.tychewealth.listing.domain;

import com.tychewealth.listing.enums.SortDirection;

public record SortCriteria(String field, SortDirection direction) {}
