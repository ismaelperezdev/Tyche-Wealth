package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.PORTFOLIO_BASE_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.CommonConstants.BASE_CURRENCY;
import static com.tychewealth.constants.CommonConstants.DESCRIPTION;
import static com.tychewealth.constants.CommonConstants.INVESTMENT_HORIZON;
import static com.tychewealth.constants.CommonConstants.MAX_RISK;
import static com.tychewealth.constants.CommonConstants.NAME;
import static com.tychewealth.constants.CommonConstants.NAME_PLACEHOLDER;
import static com.tychewealth.constants.CommonConstants.RISK_PROFILE;
import static com.tychewealth.constants.CommonConstants.STRATEGY_TYPE;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;
import static com.tychewealth.constants.TestConstants.TEST_BASE_CURRENCY_EUR;
import static com.tychewealth.constants.TestConstants.TEST_BASE_CURRENCY_USD;
import static com.tychewealth.constants.TestConstants.TEST_INVESTMENT_HORIZON_LONG;
import static com.tychewealth.constants.TestConstants.TEST_INVESTMENT_HORIZON_MEDIUM;
import static com.tychewealth.constants.TestConstants.TEST_JSON_CODE_PATH;
import static com.tychewealth.constants.TestConstants.TEST_JSON_TYPE_PATH;
import static com.tychewealth.constants.TestConstants.TEST_MAX_PORTFOLIOS_PER_USER;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_ANOTHER;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_LONG_TERM;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_DESCRIPTION_OTHER_OWNER;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_CORE;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_OTHER_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_RISK_PROFILE_LOW;
import static com.tychewealth.constants.TestConstants.TEST_RISK_PROFILE_MEDIUM;
import static com.tychewealth.constants.TestConstants.TEST_STRATEGY_TYPE_BALANCED;
import static com.tychewealth.constants.TestConstants.TEST_STRATEGY_TYPE_INCOME;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildAsset;
import static com.tychewealth.testdata.EntityBuilder.buildPortfolio;
import static com.tychewealth.testdata.PortfolioTestData.invalidEnumCreateRequest;
import static com.tychewealth.testdata.PortfolioTestData.malformedCreateRequest;
import static com.tychewealth.testhelper.AuthTestHelper.createAuthorizationHeader;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequest;
import static com.tychewealth.testhelper.PortfolioTestHelper.createRequestBody;
import static com.tychewealth.testhelper.PortfolioTestHelper.deleteMeRequest;
import static com.tychewealth.testhelper.PortfolioTestHelper.retrieveMeByIdRequest;
import static com.tychewealth.testhelper.PortfolioTestHelper.retrieveMeRequest;
import static com.tychewealth.testhelper.PortfolioTestHelper.updateMeRequest;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.PRAGMA;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.PortfolioIntegrationTestConfig;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.repository.PortfolioRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(classes = PortfolioIntegrationTestConfig.class)
@ContextConfiguration(initializers = PortfolioIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class PortfolioApiControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PortfolioRepository portfolioRepository;
  @Autowired private AssetRepository assetRepository;

  @BeforeEach
  void setUp() {
    assetRepository.deleteAll();
    portfolioRepository.deleteAll();
  }

  @Test
  void retrieveReturnsOkWithAuthenticatedUserPortfoliosOnly() throws Exception {
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_CORE,
            CurrencyCodeEnum.USD,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.MEDIUM));
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_RETIREMENT,
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG));
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_OTHER_USER_ID,
            "Other User Portfolio",
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG));

    retrieveMeRequest(mockMvc, String.valueOf(TEST_USER_ID))
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(header().string("X-Total-Count", "2"))
        .andExpect(header().string("X-Page", "0"))
        .andExpect(header().string("X-Limit", "10"))
        .andExpect(header().string("X-Has-Next", "false"))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name").value(TEST_PORTFOLIO_NAME_CORE))
        .andExpect(jsonPath("$[1].name").value(TEST_PORTFOLIO_NAME_RETIREMENT));
  }

  @Test
  void retrieveReturnsEmptyListWhenAuthenticatedUserHasNoPortfolios() throws Exception {
    retrieveMeRequest(mockMvc, String.valueOf(TEST_USER_ID))
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(header().string("X-Total-Count", "0"))
        .andExpect(header().string("X-Page", "0"))
        .andExpect(header().string("X-Limit", "10"))
        .andExpect(header().string("X-Has-Next", "false"))
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void retrieveReturnsUnauthorizedWhenAuthenticatedUserIsMissing() throws Exception {
    retrieveMeRequest(mockMvc, null)
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION).value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void retrieveByIdReturnsOkWhenPortfolioBelongsToAuthenticatedUser() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    retrieveMeByIdRequest(mockMvc, String.valueOf(TEST_USER_ID), portfolio.getId())
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.id").value(portfolio.getId()))
        .andExpect(jsonPath("$." + NAME).value(TEST_PORTFOLIO_NAME_RETIREMENT))
        .andExpect(jsonPath("$." + BASE_CURRENCY).value(TEST_BASE_CURRENCY_EUR));
  }

  @Test
  void retrieveByIdReturnsUnauthorizedWhenAuthenticatedUserIsMissing() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    retrieveMeByIdRequest(mockMvc, null, portfolio.getId())
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.UNAUTHORIZED.getType()));
  }

  @Test
  void retrieveByIdReturnsNotFoundWhenPortfolioBelongsToAnotherUser() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_OTHER_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    retrieveMeByIdRequest(mockMvc, String.valueOf(TEST_USER_ID), portfolio.getId())
        .andExpect(status().isNotFound())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION)
                .value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getDescription()));
  }

  @Test
  void createReturnsCreatedWhenPayloadIsValid() throws Exception {
    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_LONG_TERM,
            TEST_BASE_CURRENCY_EUR,
            TEST_RISK_PROFILE_LOW,
            TEST_INVESTMENT_HORIZON_LONG,
            TEST_STRATEGY_TYPE_INCOME,
            TEST_PORTFOLIO_MAX_RISK);

    createRequest(mockMvc, String.valueOf(TEST_USER_ID), requestBody)
        .andExpect(status().isCreated())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$." + NAME).value(TEST_PORTFOLIO_NAME_RETIREMENT))
        .andExpect(jsonPath("$." + DESCRIPTION).value(TEST_PORTFOLIO_DESCRIPTION_LONG_TERM))
        .andExpect(jsonPath("$." + BASE_CURRENCY).value(TEST_BASE_CURRENCY_EUR))
        .andExpect(jsonPath("$." + RISK_PROFILE).value(TEST_RISK_PROFILE_LOW))
        .andExpect(jsonPath("$." + INVESTMENT_HORIZON).value(TEST_INVESTMENT_HORIZON_LONG))
        .andExpect(jsonPath("$." + STRATEGY_TYPE).value(TEST_STRATEGY_TYPE_INCOME))
        .andExpect(jsonPath("$." + MAX_RISK).value(0.40))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());

    List<PortfolioEntity> portfolios =
        portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_USER_ID);
    assertEquals(1, portfolios.size());
    assertEquals(TEST_PORTFOLIO_NAME_RETIREMENT, portfolios.getFirst().getName());
    assertNotNull(portfolios.getFirst().getCreatedAt());
  }

  @Test
  void createReturnsBadRequestWhenAuthenticatedUserIsMissing() throws Exception {
    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_LONG_TERM,
            TEST_BASE_CURRENCY_EUR,
            TEST_RISK_PROFILE_LOW,
            TEST_INVESTMENT_HORIZON_LONG,
            TEST_STRATEGY_TYPE_INCOME,
            TEST_PORTFOLIO_MAX_RISK);

    createRequest(mockMvc, null, requestBody)
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION).value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.PortfolioTestData#invalidCreateRequests")
  void createReturnsBadRequestForInvalidPayload(String requestBody, String expectedMessage)
      throws Exception {
    createRequest(mockMvc, String.valueOf(TEST_USER_ID), requestBody)
        .andExpect(status().isBadRequest())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString(expectedMessage)));
  }

  @Test
  void createReturnsBadRequestForMalformedJson() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(PORTFOLIO_BASE_URL)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedCreateRequest()))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.GENERIC_BAD_REQUEST.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.GENERIC_BAD_REQUEST.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("invalid")));
  }

  @Test
  void createReturnsBadRequestForInvalidEnumValue() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(PORTFOLIO_BASE_URL)
                .header(AUTHORIZATION_HEADER, createAuthorizationHeader(TEST_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidEnumCreateRequest()))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.GENERIC_BAD_REQUEST.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.GENERIC_BAD_REQUEST.getType()))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("field 'baseCurrency'")))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("option 'INVALID'")))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("Available options:")))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("EUR")))
        .andExpect(jsonPath("$." + DESCRIPTION).value(containsString("USD")));
  }

  @Test
  void createReturnsConflictWhenPortfolioNameAlreadyExistsForUser() throws Exception {
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_RETIREMENT,
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_ANOTHER,
            TEST_BASE_CURRENCY_EUR,
            TEST_RISK_PROFILE_LOW,
            TEST_INVESTMENT_HORIZON_LONG,
            TEST_STRATEGY_TYPE_INCOME,
            TEST_PORTFOLIO_MAX_RISK);

    createRequest(mockMvc, String.valueOf(TEST_USER_ID), requestBody)
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_NAME_CONFLICT.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_NAME_CONFLICT.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION)
                .value(
                    ErrorDefinition.PORTFOLIO_NAME_CONFLICT
                        .getDescription()
                        .replace(NAME_PLACEHOLDER, TEST_PORTFOLIO_NAME_RETIREMENT)));
  }

  @Test
  void createReturnsConflictWhenUserAlreadyHasMaximumPortfolios() throws Exception {
    for (int index = 0; index < TEST_MAX_PORTFOLIOS_PER_USER; index++) {
      portfolioRepository.saveAndFlush(
          buildPortfolio(
              TEST_USER_ID,
              "Portfolio " + index,
              CurrencyCodeEnum.EUR,
              RiskProfileEnum.LOW,
              StrategyTypeEnum.INCOME,
              InvestmentHorizonEnum.LONG));
    }

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_LONG_TERM,
            TEST_BASE_CURRENCY_EUR,
            TEST_RISK_PROFILE_LOW,
            TEST_INVESTMENT_HORIZON_LONG,
            TEST_STRATEGY_TYPE_INCOME,
            TEST_PORTFOLIO_MAX_RISK);

    createRequest(mockMvc, String.valueOf(TEST_USER_ID), requestBody)
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_LIMIT_REACHED.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_LIMIT_REACHED.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION)
                .value(ErrorDefinition.PORTFOLIO_LIMIT_REACHED.getDescription()));
  }

  @Test
  void createAllowsSamePortfolioNameForDifferentUsers() throws Exception {
    portfolioRepository.saveAndFlush(
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_RETIREMENT,
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_RETIREMENT,
            TEST_PORTFOLIO_DESCRIPTION_OTHER_OWNER,
            TEST_BASE_CURRENCY_USD,
            TEST_RISK_PROFILE_MEDIUM,
            TEST_INVESTMENT_HORIZON_MEDIUM,
            TEST_STRATEGY_TYPE_BALANCED,
            TEST_PORTFOLIO_OTHER_MAX_RISK);

    createRequest(mockMvc, String.valueOf(TEST_OTHER_USER_ID), requestBody)
        .andExpect(status().isCreated())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$." + NAME).value(TEST_PORTFOLIO_NAME_RETIREMENT));

    assertEquals(1, portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_USER_ID).size());
    assertEquals(1, portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_OTHER_USER_ID).size());
  }

  @Test
  void updateReturnsOkWhenPortfolioBelongsToAuthenticatedUser() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_CORE,
            TEST_PORTFOLIO_DESCRIPTION_ANOTHER,
            TEST_BASE_CURRENCY_USD,
            TEST_RISK_PROFILE_MEDIUM,
            TEST_INVESTMENT_HORIZON_MEDIUM,
            TEST_STRATEGY_TYPE_BALANCED,
            TEST_PORTFOLIO_OTHER_MAX_RISK);

    updateMeRequest(mockMvc, String.valueOf(TEST_USER_ID), portfolio.getId(), requestBody)
        .andExpect(status().isOk())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(jsonPath("$.id").value(portfolio.getId()))
        .andExpect(jsonPath("$." + NAME).value(TEST_PORTFOLIO_NAME_CORE))
        .andExpect(jsonPath("$." + DESCRIPTION).value(TEST_PORTFOLIO_DESCRIPTION_ANOTHER))
        .andExpect(jsonPath("$." + BASE_CURRENCY).value(TEST_BASE_CURRENCY_USD))
        .andExpect(jsonPath("$." + RISK_PROFILE).value(TEST_RISK_PROFILE_MEDIUM))
        .andExpect(jsonPath("$." + INVESTMENT_HORIZON).value(TEST_INVESTMENT_HORIZON_MEDIUM))
        .andExpect(jsonPath("$." + STRATEGY_TYPE).value(TEST_STRATEGY_TYPE_BALANCED))
        .andExpect(jsonPath("$." + MAX_RISK).value(0.3));

    PortfolioEntity updatedPortfolio =
        portfolioRepository.findById(portfolio.getId()).orElseThrow();
    assertEquals(TEST_PORTFOLIO_NAME_CORE, updatedPortfolio.getName());
    assertEquals(TEST_PORTFOLIO_DESCRIPTION_ANOTHER, updatedPortfolio.getDescription());
    assertEquals(CurrencyCodeEnum.USD, updatedPortfolio.getBaseCurrency());
  }

  @Test
  void updateReturnsUnauthorizedWhenAuthenticatedUserIsMissing() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_CORE,
            TEST_PORTFOLIO_DESCRIPTION_ANOTHER,
            TEST_BASE_CURRENCY_USD,
            TEST_RISK_PROFILE_MEDIUM,
            TEST_INVESTMENT_HORIZON_MEDIUM,
            TEST_STRATEGY_TYPE_BALANCED,
            TEST_PORTFOLIO_OTHER_MAX_RISK);

    updateMeRequest(mockMvc, null, portfolio.getId(), requestBody)
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE));

    assertEquals(
        TEST_PORTFOLIO_NAME_RETIREMENT,
        portfolioRepository.findById(portfolio.getId()).orElseThrow().getName());
  }

  @Test
  void updateReturnsNotFoundWhenPortfolioBelongsToAnotherUser() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_OTHER_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            TEST_PORTFOLIO_NAME_CORE,
            TEST_PORTFOLIO_DESCRIPTION_ANOTHER,
            TEST_BASE_CURRENCY_USD,
            TEST_RISK_PROFILE_MEDIUM,
            TEST_INVESTMENT_HORIZON_MEDIUM,
            TEST_STRATEGY_TYPE_BALANCED,
            TEST_PORTFOLIO_OTHER_MAX_RISK);

    updateMeRequest(mockMvc, String.valueOf(TEST_USER_ID), portfolio.getId(), requestBody)
        .andExpect(status().isNotFound())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getType()))
        .andExpect(
            jsonPath("$." + DESCRIPTION)
                .value(ErrorDefinition.PORTFOLIO_NOT_FOUND.getDescription()));
  }

  @Test
  void updateReturnsConflictWhenPortfolioNameAlreadyExistsForUser() throws Exception {
    PortfolioEntity existingPortfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_CORE,
                CurrencyCodeEnum.USD,
                RiskProfileEnum.MEDIUM,
                StrategyTypeEnum.BALANCED,
                InvestmentHorizonEnum.MEDIUM));
    PortfolioEntity targetPortfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    String requestBody =
        createRequestBody(
            existingPortfolio.getName(),
            TEST_PORTFOLIO_DESCRIPTION_ANOTHER,
            TEST_BASE_CURRENCY_USD,
            TEST_RISK_PROFILE_MEDIUM,
            TEST_INVESTMENT_HORIZON_MEDIUM,
            TEST_STRATEGY_TYPE_BALANCED,
            TEST_PORTFOLIO_OTHER_MAX_RISK);

    updateMeRequest(mockMvc, String.valueOf(TEST_USER_ID), targetPortfolio.getId(), requestBody)
        .andExpect(status().isConflict())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(
            jsonPath(TEST_JSON_CODE_PATH).value(ErrorDefinition.PORTFOLIO_NAME_CONFLICT.getCode()))
        .andExpect(
            jsonPath(TEST_JSON_TYPE_PATH).value(ErrorDefinition.PORTFOLIO_NAME_CONFLICT.getType()));
  }

  @Test
  void deleteReturnsNoContentAndRemovesOwnedPortfolioWithAssets() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));
    assetRepository.saveAndFlush(
        buildAsset(portfolio, "Apple Inc.", "AAPL", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

    deleteMeRequest(mockMvc, String.valueOf(TEST_USER_ID), portfolio.getId())
        .andExpect(status().isNoContent())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE));

    assertEquals(0, portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_USER_ID).size());
    assertEquals(0, assetRepository.findByPortfolioId(portfolio.getId()).size());
  }

  @Test
  void deleteReturnsNoContentWhenPortfolioBelongsToAnotherUser() throws Exception {
    PortfolioEntity otherUsersPortfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_OTHER_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    deleteMeRequest(mockMvc, String.valueOf(TEST_USER_ID), otherUsersPortfolio.getId())
        .andExpect(status().isNoContent())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE));

    assertEquals(1, portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_OTHER_USER_ID).size());
  }

  @Test
  void deleteReturnsUnauthorizedWhenAuthenticatedUserIsMissing() throws Exception {
    PortfolioEntity portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    deleteMeRequest(mockMvc, null, portfolio.getId())
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE));

    assertTrue(portfolioRepository.findById(portfolio.getId()).isPresent());
  }
}
