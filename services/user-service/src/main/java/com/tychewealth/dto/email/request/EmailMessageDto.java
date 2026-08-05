package com.tychewealth.dto.email.request;

/** Internal email message model used before mapping to the Resend request contract. */
public record EmailMessageDto(String to, String subject, String html, String text) {}
