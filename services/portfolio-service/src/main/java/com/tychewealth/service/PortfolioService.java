package com.tychewealth.service;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import java.util.List;

public interface PortfolioService {

  List<PortfolioResponseDto> listPortfolios(Long userId);

  PortfolioResponseDto retrieve(Long userId, Long portfolioId);

  PortfolioResponseDto create(Long userId, PortfolioCreateRequestDto createRequest);

  PortfolioResponseDto update(
      Long userId, Long portfolioId, PortfolioUpdateRequestDto updateRequest);

  void delete(Long userId, Long portfolioId);
}
