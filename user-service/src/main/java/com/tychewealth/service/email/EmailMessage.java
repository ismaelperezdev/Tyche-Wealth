package com.tychewealth.service.email;

public record EmailMessage(String to, String subject, String html, String text) {}
