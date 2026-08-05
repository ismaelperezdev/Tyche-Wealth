package com.tychewealth.enums;

/** Describes the outcome of an attempt to send a transactional email. */
public enum EmailSendResult {
  DELIVERED,
  SKIPPED_DAILY_QUOTA
}
