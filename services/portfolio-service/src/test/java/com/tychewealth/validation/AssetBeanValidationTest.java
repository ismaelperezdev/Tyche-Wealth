package com.tychewealth.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AssetBeanValidationTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void tearDownValidator() {
    if (validatorFactory != null) {
      validatorFactory.close();
    }
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.AssetTestData#invalidBeanValidationCases")
  void beanValidationReturnsExpectedViolation(
      Object target, String propertyName, String expectedMessage) {
    assertTrue(
        validator.validate(target).stream()
            .anyMatch(
                violation ->
                    violation.getPropertyPath().toString().equals(propertyName)
                        && violation.getMessage().equals(expectedMessage)));
  }
}
