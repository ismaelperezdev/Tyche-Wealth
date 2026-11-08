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
  void getsPriceHistoryWithinTimeRange() {
    Instant from = Instant.parse("2026-11-01T00:00:00Z");
    Instant to = Instant.parse("2026-11-08T00:00:00Z");
    server
        .expect(
            requestTo(
                "http://market-data.test/internal/prices/history?symbol.in=AAPL&capturedAt.gte=2026-11-01T00:00:00Z&capturedAt.lte=2026-11-08T00:00:00Z&offset=5&limit=10"))
        .andExpect(method(org.springframework.http.HttpMethod.GET))
        .andExpect(
            request ->
                assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Bearer user-token"))
        .andExpect(
            request -> {
              URI uri = request.getURI();
              var query = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
              assertThat(query.getFirst("capturedAt.gte")).isEqualTo(from.toString());
              assertThat(query.getFirst("capturedAt.lte")).isEqualTo(to.toString());
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
                    MediaType.APPLICATION_JSON)
                .header("X-Total-Count", "1"));

    var result = client.getPriceHistory(List.of("AAPL"), from, to, 5, 10, "user-token");

    assertThat(result.totalCount()).isEqualTo(1);
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().bucketStart()).isEqualTo(from);
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
  void omitsOptionalHistoryBoundsAndAuthorizationWhenNotProvided() {
    server
        .expect(
            requestTo(
                "http://market-data.test/internal/prices/history?symbol.in=AAPL&offset=0&limit=10"))
        .andExpect(
            request -> {
              assertThat(request.getHeaders()).doesNotContainKey(HttpHeaders.AUTHORIZATION);
              var query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
              assertThat(query).doesNotContainKeys("capturedAt.gte", "capturedAt.lte");
            })
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).header("X-Total-Count", "0"));

    var result = client.getPriceHistory(List.of("AAPL"), null, null, 0, 10, null);

    assertThat(result.content()).isEmpty();
    assertThat(result.totalCount()).isZero();
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
