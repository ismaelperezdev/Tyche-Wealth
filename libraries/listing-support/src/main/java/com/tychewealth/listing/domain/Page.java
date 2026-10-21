package com.tychewealth.listing.domain;

import java.util.List;

public record Page<T>(long totalElements, List<T> elements) {}
