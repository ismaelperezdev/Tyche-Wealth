package com.tychewealth.service.impl;

import static com.tychewealth.constants.ApiConstants.MAX_PORTFOLIO_LIST_LIMIT;
import static com.tychewealth.constants.CommonConstants.ID;
import static com.tychewealth.constants.LogConstants.RETRIEVE_ACTION;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.mapper.portfolio.PortfolioMapper;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.PortfolioService;
import com.tychewealth.service.helper.CommonValidationHelper;
import com.tychewealth.service.helper.portfolio.PortfolioValidationHelper;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

  private final PortfolioRepository portfolioRepository;
  private final PortfolioMapper portfolioMapper;
  private final CommonValidationHelper commonValidationHelper;
  private final PortfolioValidationHelper portfolioValidationHelper;

  @Override
  @Transactional(readOnly = true)
  public Page<PortfolioResponseDto> listPortfolios(Long userId, int page, int limit) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    int safePage = Math.max(page, 0);
    int safeLimit = Math.clamp(limit, 1, MAX_PORTFOLIO_LIST_LIMIT);
    return portfolioRepository
        .findByUserId(userId, PageRequest.of(safePage, safeLimit, Sort.by(ID)))
        .map(portfolioMapper::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public PortfolioResponseDto retrieve(Long userId, Long portfolioId) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    PortfolioEntity portfolio =
        commonValidationHelper.validateOwnedPortfolio(userId, portfolioId, RETRIEVE_ACTION);
    return portfolioMapper.toDto(portfolio);
  }

  @Override
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public PortfolioResponseDto create(Long userId, PortfolioCreateRequestDto createRequest) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    portfolioValidationHelper.validateCreateRequest(userId, createRequest);

    PortfolioEntity portfolio = portfolioMapper.create(createRequest);
    portfolio.setUserId(userId);

    try {
      return portfolioMapper.toDto(portfolioRepository.saveAndFlush(portfolio));
    } catch (DataIntegrityViolationException ex) {
      throw portfolioValidationHelper.translateNamePersistenceConflict(ex, createRequest.getName());
    }
  }

  @Override
  @Transactional()
  public PortfolioResponseDto update(
      Long userId, Long portfolioId, PortfolioUpdateRequestDto updateRequest) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    PortfolioEntity portfolio =
        portfolioValidationHelper.validateUpdateRequest(userId, portfolioId, updateRequest);
    portfolioMapper.update(updateRequest, portfolio);

    try {
      return portfolioMapper.toDto(portfolioRepository.saveAndFlush(portfolio));
    } catch (DataIntegrityViolationException ex) {
      throw portfolioValidationHelper.translateNamePersistenceConflict(ex, updateRequest.getName());
    }
  }

  @Override
  @Transactional
  public void delete(Long userId, Long portfolioId) {
    commonValidationHelper.validateAuthenticatedUser(userId);
    portfolioRepository
        .findByIdAndUserId(portfolioId, userId)
        .ifPresent(portfolioRepository::delete);
  }
}
