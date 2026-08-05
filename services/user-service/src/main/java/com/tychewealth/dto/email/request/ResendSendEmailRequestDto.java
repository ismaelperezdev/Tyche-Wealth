package com.tychewealth.dto.email.request;

/** Request body matching the payload expected by the Resend email API. */
public record ResendSendEmailRequestDto(
    String from, String to, String subject, String html, String text) {}
