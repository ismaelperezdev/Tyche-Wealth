package com.tychewealth.email;

import static com.tychewealth.constants.ApiConstants.RESEND_EMAILS_PATH;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER_PREFIX;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_DAILY_LIMIT;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_HTML_BODY;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_SUBJECT_VERIFY;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_TEXT_BODY;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_VALID;
import static com.tychewealth.constants.TestConstants.TEST_RESEND_API_KEY;
import static com.tychewealth.constants.TestConstants.TEST_RESEND_BASE_URL;
import static com.tychewealth.constants.TestConstants.TEST_RESEND_FROM;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.tychewealth.dto.email.ResendEmailPropertiesDto;
import com.tychewealth.dto.email.request.EmailMessageDto;
import com.tychewealth.error.exception.EmailException;
import com.tychewealth.testhelper.InMemoryRateLimitStore;
import com.tychewealth.testhelper.RateLimitWebTestHelper.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class EmailServiceHelperTest {

  private MockRestServiceServer mockRestServiceServer;
  private EmailSender emailSender;
  private MutableClock clock;

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    mockRestServiceServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    ResendEmailPropertiesDto resendEmailProperties = new ResendEmailPropertiesDto();
    resendEmailProperties.setApiKey(TEST_RESEND_API_KEY);
    resendEmailProperties.setFrom(TEST_RESEND_FROM);
    resendEmailProperties.setBaseUrl(TEST_RESEND_BASE_URL);
    clock = new MutableClock();

    RestClient restClient = restClientBuilder.baseUrl(TEST_RESEND_BASE_URL).build();
    emailSender =
        new EmailSender(
            restClient,
            resendEmailProperties,
            TEST_EMAIL_DAILY_LIMIT,
            new InMemoryRateLimitStore(clock),
            clock);
  }

  @Test
  void sendPostsEmailToResend() {
    mockRestServiceServer
        .expect(requestTo(TEST_RESEND_BASE_URL + RESEND_EMAILS_PATH))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER_PREFIX + TEST_RESEND_API_KEY))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "from": "Tyche Wealth <auth@tyche-wealth.com>",
                      "to": "valid@tychewealth.com",
                      "subject": "Verify your email",
                      "html": "<p>Hello</p>",
                      "text": "Hello"
                    }
                    """))
        .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{}"));

    emailSender.send(
        new EmailMessageDto(
            TEST_EMAIL_VALID,
            TEST_EMAIL_SUBJECT_VERIFY,
            TEST_EMAIL_HTML_BODY,
            TEST_EMAIL_TEXT_BODY));

    mockRestServiceServer.verify();
  }

  @Test
  void sendWrapsProviderFailures() {
    mockRestServiceServer
        .expect(requestTo(TEST_RESEND_BASE_URL + RESEND_EMAILS_PATH))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
    EmailMessageDto emailMessageDto =
        new EmailMessageDto(
            TEST_EMAIL_VALID, TEST_EMAIL_SUBJECT_VERIFY, TEST_EMAIL_HTML_BODY, null);

    assertThrows(EmailException.class, () -> emailSender.send(emailMessageDto));
  }

  @Test
  void sendSkipsDeliveryWhenDailyQuotaIsExceeded() {
    mockRestServiceServer
        .expect(requestTo(TEST_RESEND_BASE_URL + RESEND_EMAILS_PATH))
        .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{}"));
    mockRestServiceServer
        .expect(requestTo(TEST_RESEND_BASE_URL + RESEND_EMAILS_PATH))
        .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("{}"));

    assertDoesNotThrow(
        () ->
            emailSender.send(
                new EmailMessageDto(
                    TEST_EMAIL_VALID, TEST_EMAIL_SUBJECT_VERIFY, TEST_EMAIL_HTML_BODY, null)));
    assertDoesNotThrow(
        () ->
            emailSender.send(
                new EmailMessageDto(
                    TEST_EMAIL_VALID, TEST_EMAIL_SUBJECT_VERIFY, TEST_EMAIL_HTML_BODY, null)));
    assertDoesNotThrow(
        () ->
            emailSender.send(
                new EmailMessageDto(
                    TEST_EMAIL_VALID, TEST_EMAIL_SUBJECT_VERIFY, TEST_EMAIL_HTML_BODY, null)));

    mockRestServiceServer.verify();
  }
}
