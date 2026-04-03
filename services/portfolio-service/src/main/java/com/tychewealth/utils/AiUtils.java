package com.tychewealth.utils;

public final class AiUtils {

  private static final int NOT_FOUND = -1;
  private static final String MARKDOWN_FENCE = "```";
  private static final char ARRAY_OPENING = '[';
  private static final char ARRAY_CLOSING = ']';
  private static final char OBJECT_OPENING = '{';
  private static final char OBJECT_CLOSING = '}';
  private static final char QUOTE = '"';
  private static final char ESCAPE = '\\';

  private AiUtils() {}

  public static String sanitizeAiResponse(String aiResponse) {
    String sanitized = trimToEmpty(aiResponse);
    sanitized = stripMarkdownFence(sanitized);

    int jsonStart = findJsonStart(sanitized);
    if (jsonStart == NOT_FOUND) {
      return sanitized;
    }

    int jsonEnd = findJsonEnd(sanitized, jsonStart);
    if (jsonEnd == NOT_FOUND) {
      return sanitized.substring(jsonStart).trim();
    }

    return sanitized.substring(jsonStart, jsonEnd + 1).trim();
  }

  private static String trimToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private static String stripMarkdownFence(String value) {
    if (!value.startsWith(MARKDOWN_FENCE)) {
      return value;
    }

    int firstLineBreak = value.indexOf('\n');
    if (firstLineBreak == NOT_FOUND) {
      return value;
    }

    String withoutOpeningFence = value.substring(firstLineBreak + 1).trim();
    if (!withoutOpeningFence.endsWith(MARKDOWN_FENCE)) {
      return withoutOpeningFence;
    }

    return withoutOpeningFence
        .substring(0, withoutOpeningFence.length() - MARKDOWN_FENCE.length())
        .trim();
  }

  private static int findJsonStart(String value) {
    int arrayStart = value.indexOf(ARRAY_OPENING);
    int objectStart = value.indexOf(OBJECT_OPENING);
    if (arrayStart == NOT_FOUND) {
      return objectStart;
    }
    if (objectStart == NOT_FOUND) {
      return arrayStart;
    }
    return Math.min(arrayStart, objectStart);
  }

  private static int findJsonEnd(String value, int start) {
    JsonDelimiters delimiters = JsonDelimiters.fromOpening(value.charAt(start));
    int depth = 0;
    boolean inString = false;
    boolean escaping = false;

    for (int index = start; index < value.length(); index++) {
      char current = value.charAt(index);

      if (shouldSkipCharacter(current, inString, escaping)) {
        boolean wasEscaping = escaping;
        inString = updateStringState(current, inString, wasEscaping);
        escaping = updateEscapingState(current, wasEscaping);
        continue;
      }

      if (current == delimiters.opening()) {
        depth++;
      }

      if (current == delimiters.closing()) {
        depth--;
        if (depth == 0) {
          return index;
        }
      }
    }

    return NOT_FOUND;
  }

  private static boolean shouldSkipCharacter(char current, boolean inString, boolean escaping) {
    return escaping || inString || current == ESCAPE || current == QUOTE;
  }

  private static boolean updateEscapingState(char current, boolean escaping) {
    if (escaping) {
      return false;
    }
    return current == ESCAPE;
  }

  private static boolean updateStringState(char current, boolean inString, boolean escaping) {
    if (escaping || current != QUOTE) {
      return inString;
    }
    return !inString;
  }

  private record JsonDelimiters(char opening, char closing) {

    private static JsonDelimiters fromOpening(char opening) {
      if (opening == ARRAY_OPENING) {
        return new JsonDelimiters(ARRAY_OPENING, ARRAY_CLOSING);
      }
      return new JsonDelimiters(OBJECT_OPENING, OBJECT_CLOSING);
    }
  }
}
