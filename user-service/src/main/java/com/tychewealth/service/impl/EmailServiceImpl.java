package com.tychewealth.service.impl;

import com.tychewealth.service.EmailService;
import com.tychewealth.service.email.EmailMessage;
import com.tychewealth.service.helper.email.EmailServiceHelper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailServiceImpl implements EmailService {

  private final EmailServiceHelper emailServiceHelper;

  @Override
  public void send(EmailMessage emailMessage) {
    emailServiceHelper.send(emailMessage);
  }
}
