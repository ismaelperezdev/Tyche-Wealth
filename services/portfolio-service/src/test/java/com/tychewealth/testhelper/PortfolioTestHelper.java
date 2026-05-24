package com.tychewealth.testhelper;

import static com.tychewealth.constants.ApiConstants.PORTFOLIO_BASE_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.CommonConstants.BASE_CURRENCY;
import static com.tychewealth.constants.CommonConstants.DESCRIPTION;
import static com.tychewealth.constants.CommonConstants.INVESTMENT_HORIZON;
import static com.tychewealth.constants.CommonConstants.MAX_RISK;
import static com.tychewealth.constants.CommonConstants.NAME;
import static com.tychewealth.constants.CommonConstants.RISK_PROFILE;
import static com.tychewealth.constants.CommonConstants.STRATEGY_TYPE;
import static com.tychewealth.testhelper.AuthTestHelper.createAuthorizationHeader;
import static com.tychewealth.utils.UtilsTest.putIfNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public final class PortfolioTestHelper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private PortfolioTestHelper() {}

  public static ResultActions createRequest(MockMvc mockMvc, String userId, String requestBody)
      throws Exception {
    MockHttpServletRequestBuilder builder =
        MockMvcRequestBuilders.post(PORTFOLIO_BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);
    if (userId != null) {
      builder.header(AUTHORIZATION_HEADER, createAuthorizationHeader(Long.parseLong(userId)));
    }
    return mockMvc.perform(builder);
  }

  public static ResultActions updateMeRequest(
      MockMvc mockMvc, String userId, Long portfolioId, String requestBody) throws Exception {
    MockHttpServletRequestBuilder builder =
        MockMvcRequestBuilders.patch(PORTFOLIO_BASE_URL + "/me/" + portfolioId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody);
    if (userId != null) {
      builder.header(AUTHORIZATION_HEADER, createAuthorizationHeader(Long.parseLong(userId)));
    }
    return mockMvc.perform(builder);
  }

  public static ResultActions retrieveMeRequest(MockMvc mockMvc, String userId) throws Exception {
    MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(PORTFOLIO_BASE_URL + "/me");
    if (userId != null) {
      builder.header(AUTHORIZATION_HEADER, createAuthorizationHeader(Long.parseLong(userId)));
    }
    return mockMvc.perform(builder);
  }

  public static ResultActions retrieveMeRequest(MockMvc mockMvc, String userId, int page, int limit)
      throws Exception {
    MockHttpServletRequestBuilder builder =
        MockMvcRequestBuilders.get(PORTFOLIO_BASE_URL + "/me")
            .queryParam("page", String.valueOf(page))
            .queryParam("limit", String.valueOf(limit));
    if (userId != null) {
      builder.header(AUTHORIZATION_HEADER, createAuthorizationHeader(Long.parseLong(userId)));
    }
    return mockMvc.perform(builder);
  }

  public static ResultActions retrieveMeByIdRequest(
      MockMvc mockMvc, String userId, Long portfolioId) throws Exception {
    MockHttpServletRequestBuilder builder =
        MockMvcRequestBuilders.get(PORTFOLIO_BASE_URL + "/me/" + portfolioId);
    if (userId != null) {
      builder.header(AUTHORIZATION_HEADER, createAuthorizationHeader(Long.parseLong(userId)));
    }
    return mockMvc.perform(builder);
  }

  public static ResultActions deleteMeRequest(MockMvc mockMvc, String userId, Long portfolioId)
      throws Exception {
    MockHttpServletRequestBuilder builder =
        MockMvcRequestBuilders.delete(PORTFOLIO_BASE_URL + "/me/" + portfolioId);
    if (userId != null) {
      builder.header(AUTHORIZATION_HEADER, createAuthorizationHeader(Long.parseLong(userId)));
    }
    return mockMvc.perform(builder);
  }

  public static String createRequestBody(
      String name,
      String description,
      String baseCurrency,
      String riskProfile,
      String investmentHorizon,
      String strategyType,
      BigDecimal maxRisk) {
    try {
      Map<String, Object> body = new LinkedHashMap<>();
      putIfNotNull(body, NAME, name);
      putIfNotNull(body, DESCRIPTION, description);
      putIfNotNull(body, BASE_CURRENCY, baseCurrency);
      putIfNotNull(body, RISK_PROFILE, riskProfile);
      putIfNotNull(body, INVESTMENT_HORIZON, investmentHorizon);
      putIfNotNull(body, STRATEGY_TYPE, strategyType);
      putIfNotNull(body, MAX_RISK, maxRisk);
      return OBJECT_MAPPER.writeValueAsString(body);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize portfolio create request body", ex);
    }
  }
}
