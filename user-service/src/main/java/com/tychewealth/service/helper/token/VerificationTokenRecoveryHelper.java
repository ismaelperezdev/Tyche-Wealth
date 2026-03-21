package com.tychewealth.service.helper.token;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.repository.UserRepository;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@AllArgsConstructor
public class VerificationTokenRecoveryHelper {

  private final UserRepository userRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void restoreVerificationTokenExpiryWithErrorHandling(
      Long userId, Instant previousVerificationTokenExpiresAt) {
    try {
      userRepository
          .findById(userId)
          .ifPresent(
              user -> {
                user.setVerificationTokenExpiresAt(previousVerificationTokenExpiresAt);
                userRepository.save(user);
              });
    } catch (RuntimeException ex) {
      log.error(
          LogConstants.REQUEST_CONFLICT + LogConstants.USER_ID,
          LogConstants.EMAIL,
          LogConstants.SEND_ACTION,
          "failed to restore verification token expiry",
          userId,
          ex);
    }
  }
}
