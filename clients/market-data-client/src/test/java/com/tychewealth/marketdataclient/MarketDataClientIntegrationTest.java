package com.tychewealth.marketdataclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class MarketDataClientIntegrationTest {

  private MockRestServiceServer server;
  private MarketDataClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://market-data.test");
    server = MockRestServiceServer.bindTo(restClientBuilder).build();
    client = new MarketDataClient(restClientBuilder.build());
  }

  @Test
  void getsLatestPricesWithBearerTokenAndPagination() {
    server
        .expect(
            requestTo(
                "http://market-data.test/internal/prices/latest?symbol.in=AAPL,MSFT&offset=0&limit=2"))
        .andExpect(method(org.springframework.http.HttpMethod.GET))
        .andExpect(
            request ->
                assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Bearer user-token"))
        .andRespond(
            withSuccess(
                    """
                    [
                      {"symbol":"AAPL","price":201.25,"capturedAt":"2026-11-08T10:00:00Z"},
                      {"symbol":"MSFT","price":502.50,"capturedAt":"2026-11-08T10:00:00Z"}
                    ]
                    """,
                    MediaType.APPLICATION_JSON)
                .header("X-Total-Count", "2"));

    var result = client.getLatestPrices(List.of("AAPL", "MSFT"), 0, 2, "user-token");

    assertThat(result.totalCount()).isEqualTo(2);
    assertThat(result.content()).hasSize(2);
    assertThat(result.content().getFirst().symbol()).isEqualTo("AAPL");
    assertThat(result.content().getFirst().price()).isEqualByComparingTo(new BigDecimal("201.25"));
    server.verify();
  }

  @Test
  void retrievesPriceCheckpointAtOrBeforeRequestedInstant() {
    Instant capturedAt = Instant.parse("2026-11-08T00:00:00Z");
    server
        .expect(
            requestTo(
                "http://market-data.test/internal/prices/history?symbol.in=AAPL&capturedAt=2026-11-08T00:00:00Z"))
        .andExpect(method(org.springframework.http.HttpMethod.GET))
        .andExpect(
            request ->
                assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Bearer user-token"))
        .andExpect(
            request -> {
              URI uri = request.getURI();
              var query = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
              assertThat(query.getFirst("capturedAt")).isEqualTo(capturedAt.toString());
            })
        .andRespond(
            withSuccess(
                """
                    [
                      {
                        "symbol":"AAPL",
                        "price":201.25,
                        "capturedAt":"2026-11-01T00:00:00Z",
                        "bucketStart":"2026-11-01T00:00:00Z"
                      }
                    ]
                    """,
                MediaType.APPLICATION_JSON));

    var result = client.retrievePriceCheckpoints(List.of("AAPL"), capturedAt, "user-token");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().capturedAt()).isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
    server.verify();
  }

  @Test
  void returnsEmptyPageWhenNoLatestPricesMatch() {
    server
        .expect(
            requestTo(
                "http://market-data.test/internal/prices/latest?symbol.in=UNKNOWN&offset=0&limit=10"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).header("X-Total-Count", "0"));

    var result = client.getLatestPrices(List.of("UNKNOWN"), 0, 10, "user-token");

    assertThat(result.content()).isEmpty();
    assertThat(result.totalCount()).isZero();
    server.verify();
  }

  @Test
  void omitsOptionalRetrieveTimestampAndAuthorizationWhenNotProvided() {
    server
        .expect(requestTo("http://market-data.test/internal/prices/history?symbol.in=AAPL"))
        .andExpect(
            request -> {
              assertThat(request.getHeaders()).doesNotContainKey(HttpHeaders.AUTHORIZATION);
              var query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
              assertThat(query).doesNotContainKey("capturedAt");
            })
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    var result = client.retrievePriceCheckpoints(List.of("AAPL"), null, null);

    assertThat(result).isEmpty();
    server.verify();
  }

  @Test
  void requestsCompleteSampledHistoryWithoutPaginationMetadata() {
    Instant from = Instant.parse("2026-11-01T00:00:00Z");
    Instant to = Instant.parse("2026-11-08T00:00:00Z");
    server
        .expect(
            requestTo(
                "http://market-data.test/internal/prices/history/sampled?symbol.in=AAPL&capturedAt.gte=2026-11-01T00:00:00Z&capturedAt.lte=2026-11-08T00:00:00Z"))
        .andExpect(
            request ->
                assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Bearer service-token"))
        .andRespond(
            withSuccess(
                """
                [{"symbol":"AAPL","price":201.25,"capturedAt":"2026-11-08T00:00:00Z","bucketStart":"2026-11-08T00:00:00Z"}]
                """,
                MediaType.APPLICATION_JSON));

    var result = client.getSampledPriceHistory(List.of("AAPL"), from, to, "service-token");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().symbol()).isEqualTo("AAPL");
    server.verify();
  }

  @Test
  void getsExchangeRatesForParallelCurrencyListsWithBearerToken() {
    server
        .expect(
            requestTo(
                "http://market-data.test/internal/exchange-rates/retrieve?baseCurrency=USD,GBP&quoteCurrency=GBP,USD"))
        .andExpect(method(org.springframework.http.HttpMethod.GET))
        .andExpect(
            request ->
                assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Bearer service-token"))
        .andRespond(
            withSuccess(
                """
                [
                  {"baseCurrency":"USD","quoteCurrency":"GBP","rate":0.7727,"effectiveDate":"2026-11-24"},
                  {"baseCurrency":"GBP","quoteCurrency":"USD","rate":1.2941,"effectiveDate":"2026-11-24"}
                ]
                """,
                MediaType.APPLICATION_JSON));

    var rates =
        client.getExchangeRates(List.of("USD", "GBP"), List.of("GBP", "USD"), "service-token");

    assertThat(rates).hasSize(2);
    assertThat(rates.getFirst().baseCurrency()).isEqualTo("USD");
    assertThat(rates.getFirst().quoteCurrency()).isEqualTo("GBP");
    assertThat(rates.getFirst().rate()).isEqualByComparingTo("0.7727");
    assertThat(rates.getFirst().effectiveDate()).isEqualTo(java.time.LocalDate.parse("2026-11-24"));
    server.verify();
  }

  @Test
  void rejectsSuccessfulResponseWithoutTotalCountHeader() {
    server
        .expect(
            requestTo(
                "http://market-data.test/internal/prices/latest?symbol.in=AAPL&offset=0&limit=10"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.getLatestPrices(List.of("AAPL"), 0, 10, "user-token"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Market-data response is missing X-Total-Count");
    server.verify();
  }

  @Test
  void propagatesUnauthorizedResponseFromMarketData() {
    server
        .expect(
            requestTo(
                "http://market-data.test/internal/prices/latest?symbol.in=AAPL&offset=0&limit=10"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    assertThatThrownBy(() -> client.getLatestPrices(List.of("AAPL"), 0, 10, null))
        .isInstanceOf(org.springframework.web.client.HttpClientErrorException.Unauthorized.class);
    server.verify();
  }
}
