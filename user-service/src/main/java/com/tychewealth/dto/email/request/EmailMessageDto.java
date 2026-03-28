package com.tychewealth.dto.email.request;

public record EmailMessageDto(String to, String subject, String html, String text) {}
