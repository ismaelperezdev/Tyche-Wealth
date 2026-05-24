package com.tychewealth.service;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import org.springframework.data.domain.Page;

public interface PortfolioService {

  Page<PortfolioResponseDto> listPortfolios(Long userId, int page, int limit);

  PortfolioResponseDto retrieve(Long userId, Long portfolioId);

  PortfolioResponseDto create(Long userId, PortfolioCreateRequestDto createRequest);

  PortfolioResponseDto update(
      Long userId, Long portfolioId, PortfolioUpdateRequestDto updateRequest);

  void delete(Long userId, Long portfolioId);
}
