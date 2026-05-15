package com.tychewealth.testdata;

import static com.tychewealth.constants.CommonConstants.DELETE;
import static com.tychewealth.constants.CommonConstants.GET;
import static com.tychewealth.constants.CommonConstants.PATCH;
import static com.tychewealth.constants.CommonConstants.POST;
import static com.tychewealth.testhelper.IdempotencyTestHelper.asset;
import static com.tychewealth.testhelper.IdempotencyTestHelper.portfolio;

import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.ratelimit.RateLimitKey;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

public final class EndpointTestData {

  private EndpointTestData() {}

  public static Stream<Arguments> rateLimitedEndpoints() throws NoSuchMethodException {
    return Stream.of(
        Arguments.of(
            RateLimitKey.PORTFOLIO_CREATE,
            new MockHttpServletRequest(POST, "/tyche-wealth/portfolio-service/v1/portfolio"),
            portfolio("create", Long.class, PortfolioCreateRequestDto.class)),
        Arguments.of(
            RateLimitKey.PORTFOLIO_UPDATE,
            new MockHttpServletRequest(PATCH, "/tyche-wealth/portfolio-service/v1/portfolio/me/7"),
            portfolio("update", Long.class, Long.class, PortfolioUpdateRequestDto.class)),
        Arguments.of(
            RateLimitKey.PORTFOLIO_LIST,
            new MockHttpServletRequest(GET, "/tyche-wealth/portfolio-service/v1/portfolio/me"),
            portfolio("listPortfolios", Long.class)),
        Arguments.of(
            RateLimitKey.PORTFOLIO_RETRIEVE,
            new MockHttpServletRequest(GET, "/tyche-wealth/portfolio-service/v1/portfolio/me/7"),
            portfolio("retrieve", Long.class, Long.class)),
        Arguments.of(
            RateLimitKey.PORTFOLIO_DELETE,
            new MockHttpServletRequest(DELETE, "/tyche-wealth/portfolio-service/v1/portfolio/me/7"),
            portfolio("delete", Long.class, Long.class)),
        Arguments.of(
            RateLimitKey.ASSET_IMPORT,
            new MockHttpServletRequest(POST, "/tyche-wealth/portfolio-service/v1/assets/import"),
            asset("importAssets", Long.class, MultipartFile.class)),
        Arguments.of(
            RateLimitKey.ASSET_IMPORT_RETRIEVE,
            new MockHttpServletRequest(
                GET, "/tyche-wealth/portfolio-service/v1/assets/import/import-123"),
            asset("retrieveImportedAssets", Long.class, String.class)));
  }
}
