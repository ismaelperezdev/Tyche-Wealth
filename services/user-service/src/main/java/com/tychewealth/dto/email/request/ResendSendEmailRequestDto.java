package com.tychewealth.dto.email.request;

public record ResendSendEmailRequestDto(
    String from, String to, String subject, String html, String text) {}
