package com.tychewealth.security;

import static com.tychewealth.constants.ApiConstants.USER_ME_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.SecurityConstants.ACTUATOR_PROMETHEUS_PATH;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_USERNAME;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.testhelper.AuthTestHelper.login;
import static com.tychewealth.testhelper.AuthTestHelper.logout;
import static com.tychewealth.testhelper.AuthTestHelper.refresh;
import static com.tychewealth.testhelper.AuthTestHelper.seedTrustedDevice;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import com.tychewealth.config.AuthRateLimitConfig;
import com.tychewealth.config.security.InternalServiceTokenServerConfig;
import com.tychewealth.config.security.SecurityIntegrationInternalTestConfig;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.TrustedDeviceRepository;
import com.tychewealth.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest(
    classes = SecurityIntegrationInternalTestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = SecurityIntegrationInternalTestConfig.Initializer.class)
@AutoConfigureMockMvc
class InternalServiceTokenServerIntegrationTest {

  private static final String TOKEN_ENDPOINT = "/oauth2/token";
  private static final String CLIENT_ID = "portfolio-service-integration-test";
  private static final String CLIENT_SECRET = "integration-test-secret";
  private static final String CLIENT_CREDENTIALS_GRANT = "client_credentials";
  private static final String MARKET_PRICES_READ_SCOPE = "market-prices:read";

  @Autowired private MockMvc mockMvc;
  @Autowired private RegisteredClientRepository registeredClientRepository;
  @Autowired private OAuth2AuthorizationService authorizationService;
  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private TrustedDeviceRepository trustedDeviceRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AuthRateLimitConfig rateLimitConfig;

  @BeforeEach
  void resetUserState() {
    refreshTokenRepository.deleteAll();
    trustedDeviceRepository.deleteAll();
    userRepository.deleteAll();
    rateLimitConfig.resetAll();
  }

  @Test
  void issuesTokenThroughRealServletServerWithoutHumanFilterInterference() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth(CLIENT_ID, CLIENT_SECRET);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    var form = new LinkedMultiValueMap<String, String>();
    form.add("grant_type", CLIENT_CREDENTIALS_GRANT);
    form.add("scope", MARKET_PRICES_READ_SCOPE);
    var response =
        restTemplate.postForEntity(TOKEN_ENDPOINT, new HttpEntity<>(form, headers), String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var body = objectMapper.readTree(response.getBody());
    assertNotNull(body.get("access_token"));
    assertEquals(
        CLIENT_ID,
        SignedJWT.parse(body.get("access_token").asText()).getJWTClaimsSet().getSubject());
  }

  @Test
  void humanFilterDoesNotRejectMalformedBearerOnPublicJwksEndpoint() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("not-a-human-jwt");
    var response =
        restTemplate.exchange(
            "/oauth2/jwks", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    var jwks = objectMapper.readTree(response.getBody());
    assertEquals("integration-test-key", jwks.get("keys").get(0).get("kid").asText());
    assertFalse(jwks.get("keys").get(0).has("d"));
  }

  @Test
  void preservesHumanLoginAccessRefreshLogoutAndRevocation() throws Exception {
    UserEntity user = new UserEntity();
    user.setEmail(TEST_EMAIL_LAURA);
    user.setUsername(TEST_USERNAME_LAURA);
    user.setPassword(passwordEncoder.encode(TEST_PASSWORD_VALID));
    user.setVerified(true);
    user = userRepository.save(user);
    var trustedDevice = seedTrustedDevice(trustedDeviceRepository, user);
    var loggedIn =
        login(
            mockMvc,
            objectMapper,
            new LoginRequestDto(TEST_EMAIL_LAURA, TEST_PASSWORD_VALID),
            trustedDevice);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(loggedIn.getAccessToken());
    assertEquals(
        HttpStatus.OK,
        restTemplate
            .exchange(USER_ME_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class)
            .getStatusCode());
    var refreshedResult =
        refresh(mockMvc, objectMapper, loggedIn.getRefreshToken())
            .andExpect(status().isOk())
            .andReturn();
    var refreshed =
        objectMapper.readValue(
            refreshedResult.getResponse().getContentAsString(), RefreshTokenResponseDto.class);
    refresh(mockMvc, objectMapper, loggedIn.getRefreshToken()).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            get(USER_ME_URL).header(AUTHORIZATION_HEADER, "Bearer " + refreshed.getAccessToken()))
        .andExpect(status().isOk());
    logout(mockMvc, objectMapper, refreshed.getAccessToken(), refreshed.getRefreshToken())
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            get(USER_ME_URL).header(AUTHORIZATION_HEADER, "Bearer " + refreshed.getAccessToken()))
        .andExpect(status().isUnauthorized());
    refresh(mockMvc, objectMapper, refreshed.getRefreshToken())
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsSupportedGrantNotAuthorizedForClient() throws Exception {
    var client = registeredClientRepository.findByClientId(CLIENT_ID);
    Instant now = Instant.now();
    // Seed a known token so the provider reaches the client grant check, not token lookup failure.
    var authorization =
        OAuth2Authorization.withRegisteredClient(client)
            .principalName(CLIENT_ID)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .refreshToken(new OAuth2RefreshToken("test-refresh-token", now, now.plusSeconds(300)))
            .build();
    authorizationService.save(authorization);
    try {
      mockMvc
          .perform(
              post(TOKEN_ENDPOINT)
                  .secure(true)
                  .header(AUTHORIZATION_HEADER, basicAuth(CLIENT_ID, CLIENT_SECRET))
                  .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                  .param("grant_type", "refresh_token")
                  .param("refresh_token", "test-refresh-token"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("unauthorized_client"));
    } finally {
      authorizationService.remove(authorization);
    }
  }

  @Test
  void issuesServiceAccessTokenWithExpectedClaimsAndNoRefreshToken() throws Exception {
    MvcResult result =
        requestToken(CLIENT_ID, CLIENT_SECRET, CLIENT_CREDENTIALS_GRANT, MARKET_PRICES_READ_SCOPE)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.scope").value(MARKET_PRICES_READ_SCOPE))
            .andExpect(jsonPath("$.refresh_token").doesNotExist())
            .andReturn();

    int expiresIn =
        com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.expires_in");
    assertTrue(expiresIn > 0 && expiresIn <= 300);

    String accessToken =
        com.jayway.jsonpath.JsonPath.read(
            result.getResponse().getContentAsString(), "$.access_token");
    var claims = SignedJWT.parse(accessToken).getJWTClaimsSet();

    assertEquals(
        300,
        Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant())
            .getSeconds());
    assertEquals(CLIENT_ID, claims.getSubject());
    assertEquals(List.of("market-data-service"), claims.getAudience());
  }

  @Test
  void rejectsIncorrectClientSecret() throws Exception {
    requestToken(CLIENT_ID, "incorrect-secret", CLIENT_CREDENTIALS_GRANT, MARKET_PRICES_READ_SCOPE)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("invalid_client"));
  }

  @Test
  void rejectsUnknownClient() throws Exception {
    requestToken(
            "unknown-service", CLIENT_SECRET, CLIENT_CREDENTIALS_GRANT, MARKET_PRICES_READ_SCOPE)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("invalid_client"));
  }

  @Test
  void rejectsUnsupportedGrantType() throws Exception {
    requestToken(CLIENT_ID, CLIENT_SECRET, "password", MARKET_PRICES_READ_SCOPE)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
  }

  @Test
  void rejectsUnregisteredScope() throws Exception {
    requestToken(CLIENT_ID, CLIENT_SECRET, CLIENT_CREDENTIALS_GRANT, "unregistered-scope")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_scope"));
  }

  @Test
  void keepsUserAndPrometheusEndpointsBehindTheirExistingAuthentication() throws Exception {
    mockMvc.perform(get(USER_ME_URL).secure(true)).andExpect(status().isUnauthorized());

    mockMvc
        .perform(get(ACTUATOR_PROMETHEUS_PATH).secure(true))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            get(ACTUATOR_PROMETHEUS_PATH)
                .secure(true)
                .header(
                    AUTHORIZATION_HEADER,
                    basicAuth(TEST_PROMETHEUS_USERNAME, TEST_PROMETHEUS_PASSWORD)))
        .andExpect(status().isOk());
  }

  @Test
  void usesOnlyAudiencesAuthorizedInClientSettings() throws Exception {
    var template = registeredClientRepository.findByClientId(CLIENT_ID);
    String clientId = "other-client";
    var client =
        RegisteredClient.from(template)
            .id(UUID.randomUUID().toString())
            .clientId(clientId)
            .clientSecret(passwordEncoder.encode(CLIENT_SECRET))
            .clientSettings(
                ClientSettings.builder()
                    .setting(
                        InternalServiceTokenServerConfig.SERVICE_TOKEN_AUDIENCES_SETTING,
                        List.of("market-data-service"))
                    .build())
            .build();
    registeredClientRepository.save(client);

    // A requested destination must not override the server-managed client policy.
    var result =
        mockMvc
            .perform(
                post(TOKEN_ENDPOINT)
                    .secure(true)
                    .header(AUTHORIZATION_HEADER, basicAuth(clientId, CLIENT_SECRET))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("grant_type", CLIENT_CREDENTIALS_GRANT)
                    .param("scope", MARKET_PRICES_READ_SCOPE)
                    .param("audience", "user-service")
                    .param("resource", "https://unauthorized-service.test"))
            .andExpect(status().isOk())
            .andReturn();
    String token =
        objectMapper
            .readTree(result.getResponse().getContentAsString())
            .get("access_token")
            .asText();
    assertEquals(
        List.of("market-data-service"), SignedJWT.parse(token).getJWTClaimsSet().getAudience());
  }

  @ParameterizedTest
  @MethodSource("invalidAudienceSettings")
  void refusesIssuanceWithMissingOrInvalidAudienceSettings(Object audiences) throws Exception {
    var template = registeredClientRepository.findByClientId(CLIENT_ID);
    var settings = ClientSettings.builder();
    if (audiences != null) {
      settings.setting(InternalServiceTokenServerConfig.SERVICE_TOKEN_AUDIENCES_SETTING, audiences);
    }
    try {
      registeredClientRepository.save(
          RegisteredClient.from(template).clientSettings(settings.build()).build());

      requestToken(CLIENT_ID, CLIENT_SECRET, CLIENT_CREDENTIALS_GRANT, MARKET_PRICES_READ_SCOPE)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("server_error"))
          .andExpect(jsonPath("$.access_token").doesNotExist());
    } finally {
      registeredClientRepository.save(template);
    }
  }

  @Test
  void refusesIssuanceForClientNotInAllowedClientIds() throws Exception {
    var template = registeredClientRepository.findByClientId(CLIENT_ID);
    String clientId = "unlisted-client";
    registeredClientRepository.save(
        RegisteredClient.from(template)
            .id(UUID.randomUUID().toString())
            .clientId(clientId)
            .clientSecret(passwordEncoder.encode(CLIENT_SECRET))
            .build());

    requestToken(clientId, CLIENT_SECRET, CLIENT_CREDENTIALS_GRANT, MARKET_PRICES_READ_SCOPE)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("server_error"))
        .andExpect(jsonPath("$.access_token").doesNotExist());
  }

  private static Stream<Object> invalidAudienceSettings() {
    return Stream.of(
        null,
        List.of(),
        "not-a-list",
        List.of(""),
        List.of("valid-service", 123),
        List.of("not-allowed-service"));
  }

  private ResultActions requestToken(
      String clientId, String clientSecret, String grantType, String scope) throws Exception {
    return mockMvc.perform(
        post(TOKEN_ENDPOINT)
            .secure(true)
            .header(AUTHORIZATION_HEADER, basicAuth(clientId, clientSecret))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("grant_type", grantType)
            .param("scope", scope));
  }

  private String basicAuth(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}
