package com.tychewealth.email;

import static com.tychewealth.constants.ApiConstants.RESEND_EMAILS_PATH;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER_PREFIX;
import static com.tychewealth.constants.LogConstants.EMAIL;
import static com.tychewealth.constants.LogConstants.EMAIL_DAILY_QUOTA_SKIPPED_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.RESEND_DELIVERY_FAILED_MESSAGE;
import static com.tychewealth.constants.LogConstants.SEND_ACTION;
import static com.tychewealth.constants.RedisConstants.EMAIL_DAILY_LIMIT_CLIENT_KEY;
import static com.tychewealth.constants.RedisConstants.EMAIL_DAILY_LIMIT_NAMESPACE;
import static com.tychewealth.utils.Utils.currentUtcDate;
import static com.tychewealth.utils.Utils.durationUntilNextUtcMidnight;

import com.tychewealth.dto.email.ResendEmailPropertiesDto;
import com.tychewealth.dto.email.request.EmailMessageDto;
import com.tychewealth.dto.email.request.ResendSendEmailRequestDto;
import com.tychewealth.error.exception.EmailException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.ratelimit.RateLimitStore;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@RequiredArgsConstructor
public class EmailSender {

  private final RestClient restClient;
  private final ResendEmailPropertiesDto resendEmailProperties;
  private final int emailDailyLimit;
  private final RateLimitStore rateLimitStore;
  private final Clock clock;

  public void send(EmailMessageDto emailMessageDto) {
    if (!canSendWithinDailyQuota()) {
      return;
    }

    try {
      restClient
          .post()
          .uri(RESEND_EMAILS_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .header(
              AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER_PREFIX + resendEmailProperties.getApiKey())
          .body(toResendRequest(emailMessageDto))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ex) {
      log.error(REQUEST_CONFLICT, EMAIL, SEND_ACTION, RESEND_DELIVERY_FAILED_MESSAGE, ex);
      throw EmailException.of(
          ErrorDefinition.EMAIL_DELIVERY_FAILED, null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private boolean canSendWithinDailyQuota() {
    long currentCount =
        rateLimitStore.increment(
            EMAIL_DAILY_LIMIT_NAMESPACE + ":" + currentUtcDate(clock),
            EMAIL_DAILY_LIMIT_CLIENT_KEY,
            durationUntilNextUtcMidnight(clock));

    if (currentCount > emailDailyLimit) {
      log.info(REQUEST_CONFLICT, EMAIL, SEND_ACTION, EMAIL_DAILY_QUOTA_SKIPPED_MESSAGE);
      return false;
    }
    return true;
  }

  private ResendSendEmailRequestDto toResendRequest(EmailMessageDto emailMessageDto) {
    return new ResendSendEmailRequestDto(
        resendEmailProperties.getFrom(),
        emailMessageDto.to(),
        emailMessageDto.subject(),
        emailMessageDto.html(),
        emailMessageDto.text());
  }
}
