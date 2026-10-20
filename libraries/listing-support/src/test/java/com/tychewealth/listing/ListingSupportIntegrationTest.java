package com.tychewealth.listing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

  @BeforeEach
  void insertAssets() {
    ListingTestData.insertSampleAssets(entityManager);
  }

  @Test
  void resolvesListQueryBuildsJpaSpecificationAndPaginatesThroughMvc() throws Exception {
    mockMvc
        .perform(
            get("/integration/assets")
                .param("details.category", "Technology")
                .param("sort", "-symbol")
                .param("offset", "1")
                .param("limit", "1")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "[{\"symbol\":\"AAPL\",\"name\":\"Apple\",\"details\":{\"category\":\"Technology\"}}]"));
  }

  @Test
  void filtersByNestedResponseFieldBeforeApplyingPagination() throws Exception {
    mockMvc
        .perform(
            get("/integration/assets")
                .param("details.category", "Energy")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "[{\"symbol\":\"XOM\",\"name\":\"Exxon\",\"details\":{\"category\":\"Energy\"}}]"));
  }

  @Test
  void returnsAnEmptyArrayWhenNestedFilterHasNoMatches() throws Exception {
    mockMvc
        .perform(
            get("/integration/assets")
                .param("details.category", "Commodities")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void rejectsUnknownNestedFilterFieldsAsBadRequest() throws Exception {
    mockMvc
        .perform(
            get("/integration/assets")
                .param("details.unknown", "value")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"code\":\"TYCHE-9106\"}"));
  }

  @Test
  void convertsDateAndEnumFiltersAndReturnsBadRequestForInvalidValues() throws Exception {
    mockMvc
        .perform(
            get("/integration/assets")
                .param("acquiredOn", "2023-01-10")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("[{\"symbol\":\"AAPL\"}]"));

    mockMvc
        .perform(
            get("/integration/assets")
                .param("status", "ACTIVE")
                .param("sort", "symbol")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("[{\"symbol\":\"AAPL\"},{\"symbol\":\"MSFT\"}]"));

    mockMvc
        .perform(get("/integration/assets").param("acquiredOn", "not-a-date"))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"code\":\"TYCHE-9105\"}"));

    mockMvc
        .perform(get("/integration/assets").param("status", "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"code\":\"TYCHE-9105\"}"));
  }

  @Test
  void supportsExplicitNullFiltersWhenTheFieldMappingDefinesTheNullToken() throws Exception {
    mockMvc
        .perform(
            get("/integration/assets").param("status", "null").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("[{\"symbol\":\"BABA\",\"status\":null}]"));

    mockMvc
        .perform(
            get("/integration/assets")
                .param("status.neq", "null")
                .param("sort", "symbol")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content().json("[{\"symbol\":\"AAPL\"},{\"symbol\":\"MSFT\"},{\"symbol\":\"XOM\"}]"));
  }
}
