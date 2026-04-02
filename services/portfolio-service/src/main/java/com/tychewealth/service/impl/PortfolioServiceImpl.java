package com.tychewealth.service.impl;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.mapper.portfolio.PortfolioMapper;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.PortfolioService;
import com.tychewealth.service.helper.portfolio.PortfolioValidationHelper;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

  private final PortfolioRepository portfolioRepository;
  private final PortfolioMapper portfolioMapper;
  private final PortfolioValidationHelper portfolioValidationHelper;

  @Override
  @Transactional(readOnly = true)
  public List<PortfolioResponseDto> listPortfolios(Long userId) {
    return portfolioRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
        .map(portfolioMapper::toDto)
        .toList();
  }

  @Override
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public PortfolioResponseDto create(Long userId, PortfolioCreateRequestDto createRequest) {
    portfolioValidationHelper.validateCreateRequest(userId, createRequest);

    PortfolioEntity portfolio = portfolioMapper.create(createRequest);
    portfolio.setUserId(userId);

    try {
      return portfolioMapper.toDto(portfolioRepository.saveAndFlush(portfolio));
    } catch (DataIntegrityViolationException ex) {
      throw portfolioValidationHelper.validateCreatePersistenceConflict(
          ex, createRequest.getName());
    }
  }
}
