package com.tychewealth.listing;

import static com.tychewealth.listing.ListingTestConstants.ACQUIRED_ON_FIELD;
import static com.tychewealth.listing.ListingTestConstants.ASSET_AAPL;
import static com.tychewealth.listing.ListingTestConstants.ASSET_ENDPOINT;
import static com.tychewealth.listing.ListingTestConstants.CATEGORY_ENERGY;
import static com.tychewealth.listing.ListingTestConstants.CATEGORY_TECHNOLOGY;
import static com.tychewealth.listing.ListingTestConstants.CONTENT_RANGE_HEADER;
import static com.tychewealth.listing.ListingTestConstants.DATE_AAPL_ACQUIRED;
import static com.tychewealth.listing.ListingTestConstants.DETAILS_CATEGORY_FIELD;
import static com.tychewealth.listing.ListingTestConstants.FIELDS_PARAM;
import static com.tychewealth.listing.ListingTestConstants.LIMIT_PARAM;
import static com.tychewealth.listing.ListingTestConstants.LINK_HEADER;
import static com.tychewealth.listing.ListingTestConstants.NULL_TOKEN;
import static com.tychewealth.listing.ListingTestConstants.OFFSET_PARAM;
import static com.tychewealth.listing.ListingTestConstants.PAGE_PARAM;
import static com.tychewealth.listing.ListingTestConstants.RANGE_HEADER;
import static com.tychewealth.listing.ListingTestConstants.SORT_PARAM;
import static com.tychewealth.listing.ListingTestConstants.STATUS_ACTIVE;
import static com.tychewealth.listing.ListingTestConstants.STATUS_FIELD;
import static com.tychewealth.listing.ListingTestConstants.SYMBOL_FIELD;
import static com.tychewealth.listing.ListingTestConstants.TOTAL_COUNT_HEADER;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tychewealth.listing.enums.ErrorDefinition;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    classes = ListingTestData.TestApplication.class,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:listing-integration;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.liquibase.enabled=false"
    })
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ListingSupportIntegrationTest {

  private final MockMvc mockMvc;
  private final EntityManager entityManager;

  @Autowired
  ListingSupportIntegrationTest(MockMvc mockMvc, EntityManager entityManager) {
    this.mockMvc = mockMvc;
    this.entityManager = entityManager;
  }

  @Test
  void serializesAllAnnotatedResponseFieldsWithoutSelection() throws Exception {
    mockMvc
        .perform(get(ASSET_ENDPOINT).param(SYMBOL_FIELD, ASSET_AAPL))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-aapl-full.json")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", ","})
  void serializesAllAnnotatedResponseFieldsWithEmptySelection(String fields) throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT).param(SYMBOL_FIELD, ASSET_AAPL).queryParam(FIELDS_PARAM, fields))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-aapl-full.json")));
  }

  @Test
  void projectsAnnotatedResponseFieldsWhenSelectionIsPresent() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .param(SYMBOL_FIELD, ASSET_AAPL)
                .queryParam(FIELDS_PARAM, "symbol,details.category"))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-aapl-projection.json")));
  }

  private String jsonFixture(String name) throws IOException {
    try (var input = getClass().getResourceAsStream("/json/" + name)) {
      if (input == null) {
        throw new IllegalArgumentException("Missing JSON test fixture: " + name);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @BeforeEach
  void insertAssets() {
    ListingTestData.insertSampleAssets(entityManager);
  }

  @Test
  void resolvesListQueryBuildsJpaSpecificationAndPaginatesThroughMvc() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .param(DETAILS_CATEGORY_FIELD, CATEGORY_TECHNOLOGY)
                .param(SORT_PARAM, "-symbol")
                .param(OFFSET_PARAM, "1")
                .param(LIMIT_PARAM, "1")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-technology-filter.json")));
  }

  @Test
  void usesDefaultPaginationAndReturnsAllFourAssets() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .queryParam(SORT_PARAM, SYMBOL_FIELD)
                .queryParam(FIELDS_PARAM, SYMBOL_FIELD))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-symbols.json")))
        .andExpect(header().string(TOTAL_COUNT_HEADER, "4"))
        .andExpect(
            header()
                .string(
                    LINK_HEADER,
                    "</integration/assets?sort=symbol&fields=symbol&offset=0&limit=20>; rel=\"self\""));
  }

  @Test
  void convertsLegacyPageAndBuildsPaginationMetadata() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .queryParam(PAGE_PARAM, "1")
                .queryParam(LIMIT_PARAM, "2")
                .queryParam(SORT_PARAM, SYMBOL_FIELD)
                .queryParam(FIELDS_PARAM, SYMBOL_FIELD))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-symbols-second-page.json")))
        .andExpect(header().string(TOTAL_COUNT_HEADER, "4"))
        .andExpect(
            header()
                .string(
                    LINK_HEADER,
                    "</integration/assets?page=1&sort=symbol&fields=symbol&offset=2&limit=2>; rel=\"self\", </integration/assets?page=1&sort=symbol&fields=symbol&offset=0&limit=2>; rel=\"first\", </integration/assets?page=1&sort=symbol&fields=symbol&offset=0&limit=2>; rel=\"prev\""));
  }

  @Test
  void appliesRangeAndFallsBackToDefaultsForInvalidRange() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .header(RANGE_HEADER, "items=2-3")
                .queryParam(SORT_PARAM, SYMBOL_FIELD)
                .queryParam(FIELDS_PARAM, SYMBOL_FIELD))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-symbols-second-page.json")))
        .andExpect(header().string(CONTENT_RANGE_HEADER, "items 2-3/4"))
        .andExpect(header().string(TOTAL_COUNT_HEADER, "4"))
        .andExpect(header().string(LINK_HEADER, containsString("offset=2&limit=2>; rel=\"self\"")));
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .header(RANGE_HEADER, "invalid")
                .queryParam(SORT_PARAM, SYMBOL_FIELD)
                .queryParam(FIELDS_PARAM, SYMBOL_FIELD))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-symbols.json")))
        .andExpect(header().doesNotExist(CONTENT_RANGE_HEADER))
        .andExpect(
            header().string(LINK_HEADER, containsString("offset=0&limit=20>; rel=\"self\"")));
  }

  @Test
  void returnsEmptyArrayBeyondTheLastRowWithMatchingTotal() throws Exception {
    mockMvc
        .perform(get(ASSET_ENDPOINT).queryParam(OFFSET_PARAM, "10").queryParam(LIMIT_PARAM, "2"))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-empty.json")))
        .andExpect(header().string(TOTAL_COUNT_HEADER, "4"))
        .andExpect(
            header().string(LINK_HEADER, containsString("offset=10&limit=2>; rel=\"self\"")));
  }

  @Test
  void consecutivePagesHaveNoMissingOrDuplicateAssets() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .queryParam(OFFSET_PARAM, "0")
                .queryParam(LIMIT_PARAM, "2")
                .queryParam(SORT_PARAM, SYMBOL_FIELD)
                .queryParam(FIELDS_PARAM, SYMBOL_FIELD))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-symbols-first-page.json")))
        .andExpect(header().string(LINK_HEADER, containsString("offset=2&limit=2>; rel=\"next\"")));
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .queryParam(OFFSET_PARAM, "2")
                .queryParam(LIMIT_PARAM, "2")
                .queryParam(SORT_PARAM, SYMBOL_FIELD)
                .queryParam(FIELDS_PARAM, SYMBOL_FIELD))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-symbols-second-page.json")))
        .andExpect(header().string(LINK_HEADER, containsString("offset=0&limit=2>; rel=\"prev\"")));
  }

  @Test
  void filtersByNestedResponseFieldBeforeApplyingPagination() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .param(DETAILS_CATEGORY_FIELD, CATEGORY_ENERGY)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-xom-filter.json")));
  }

  @Test
  void returnsAnEmptyArrayWhenNestedFilterHasNoMatches() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .param(DETAILS_CATEGORY_FIELD, "Commodities")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-empty.json")));
  }

  @Test
  void rejectsUnknownNestedFilterFieldsAsBadRequest() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .param("details.unknown", "value")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.FILTER_FIELD_NOT_FOUND.getCode()));
  }

  @Test
  void convertsDateAndEnumFiltersAndReturnsBadRequestForInvalidValues() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .param(ACQUIRED_ON_FIELD, DATE_AAPL_ACQUIRED)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-date-filter.json")));

    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .param(STATUS_FIELD, STATUS_ACTIVE)
                .param(SORT_PARAM, SYMBOL_FIELD)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-active-filter.json")));

    mockMvc
        .perform(get(ASSET_ENDPOINT).param(ACQUIRED_ON_FIELD, "not-a-date"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.FILTER_PARSE_ERROR.getCode()));

    mockMvc
        .perform(get(ASSET_ENDPOINT).param(STATUS_FIELD, "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.FILTER_PARSE_ERROR.getCode()));
  }

  @Test
  void supportsExplicitNullFiltersWhenTheFieldMappingDefinesTheNullToken() throws Exception {
    mockMvc
        .perform(
            get(ASSET_ENDPOINT).param(STATUS_FIELD, NULL_TOKEN).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-null-filter.json")));

    mockMvc
        .perform(
            get(ASSET_ENDPOINT)
                .param("status.neq", NULL_TOKEN)
                .param(SORT_PARAM, SYMBOL_FIELD)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(jsonFixture("assets-non-null-filter.json")));
  }
}
