package com.tychewealth.service;

import com.tychewealth.service.email.EmailMessage;

public interface EmailService {

  void send(EmailMessage emailMessage);
}
