package com.tychewealth.service;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import java.util.List;

public interface PortfolioService {

  List<PortfolioResponseDto> listPortfolios(Long userId);

  PortfolioResponseDto create(Long userId, PortfolioCreateRequestDto createRequest);
}
