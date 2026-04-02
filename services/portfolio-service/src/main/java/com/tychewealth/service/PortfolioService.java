package com.tychewealth.service;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;

public interface PortfolioService {

  PortfolioResponseDto create(Long userId, PortfolioCreateRequestDto createRequest);
}
