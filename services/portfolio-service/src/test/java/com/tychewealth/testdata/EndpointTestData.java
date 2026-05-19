package com.tychewealth.testdata;

import static com.tychewealth.constants.ApiConstants.ASSET_IMPORT_BY_ID_URL;
import static com.tychewealth.constants.ApiConstants.ASSET_IMPORT_URL;
import static com.tychewealth.constants.ApiConstants.PORTFOLIO_ASSET_BASE_URL;
import static com.tychewealth.constants.ApiConstants.PORTFOLIO_BASE_URL;
import static com.tychewealth.constants.CommonConstants.DELETE;
import static com.tychewealth.constants.CommonConstants.GET;
import static com.tychewealth.constants.CommonConstants.PATCH;
import static com.tychewealth.constants.CommonConstants.POST;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_ID;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_IMPORT_ID;
import static com.tychewealth.constants.TestConstants.TEST_ME_PATH_SEGMENT;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID_TEMPLATE;
import static com.tychewealth.testhelper.IdempotencyTestHelper.asset;
import static com.tychewealth.testhelper.IdempotencyTestHelper.portfolio;

import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
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
            new MockHttpServletRequest(POST, PORTFOLIO_BASE_URL),
            portfolio("create", Long.class, PortfolioCreateRequestDto.class)),
        Arguments.of(
            RateLimitKey.PORTFOLIO_UPDATE,
            new MockHttpServletRequest(
                PATCH, PORTFOLIO_BASE_URL + TEST_ME_PATH_SEGMENT + TEST_PORTFOLIO_ID),
            portfolio("update", Long.class, Long.class, PortfolioUpdateRequestDto.class)),
        Arguments.of(
            RateLimitKey.PORTFOLIO_LIST,
            new MockHttpServletRequest(GET, PORTFOLIO_BASE_URL + "/me"),
            portfolio("listPortfolios", Long.class)),
        Arguments.of(
            RateLimitKey.PORTFOLIO_RETRIEVE,
            new MockHttpServletRequest(
                GET, PORTFOLIO_BASE_URL + TEST_ME_PATH_SEGMENT + TEST_PORTFOLIO_ID),
            portfolio("retrieve", Long.class, Long.class)),
        Arguments.of(
            RateLimitKey.PORTFOLIO_DELETE,
            new MockHttpServletRequest(
                DELETE, PORTFOLIO_BASE_URL + TEST_ME_PATH_SEGMENT + TEST_PORTFOLIO_ID),
            portfolio("delete", Long.class, Long.class)),
        Arguments.of(
            RateLimitKey.ASSET_CREATE,
            new MockHttpServletRequest(
                POST,
                PORTFOLIO_ASSET_BASE_URL.replace(
                    TEST_PORTFOLIO_ID_TEMPLATE, String.valueOf(TEST_PORTFOLIO_ID))),
            asset("create", Long.class, Long.class, AssetCreateRequestDto.class)),
        Arguments.of(
            RateLimitKey.ASSET_RETRIEVE,
            new MockHttpServletRequest(
                GET,
                PORTFOLIO_ASSET_BASE_URL.replace(
                        TEST_PORTFOLIO_ID_TEMPLATE, String.valueOf(TEST_PORTFOLIO_ID))
                    + "/"
                    + TEST_ASSET_ID),
            asset("retrieve", Long.class, Long.class, Long.class)),
        Arguments.of(
            RateLimitKey.ASSET_IMPORT,
            new MockHttpServletRequest(POST, ASSET_IMPORT_URL),
            asset("importAssets", Long.class, MultipartFile.class)),
        Arguments.of(
            RateLimitKey.ASSET_IMPORT_RETRIEVE,
            new MockHttpServletRequest(
                GET, ASSET_IMPORT_BY_ID_URL.replace("{importId}", TEST_ASSET_IMPORT_ID)),
            asset("retrieveImportedAssets", Long.class, String.class)));
  }
}
