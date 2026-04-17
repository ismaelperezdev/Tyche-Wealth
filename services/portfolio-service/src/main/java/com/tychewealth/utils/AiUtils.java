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
    String sanitized = aiResponse == null ? "" : aiResponse.trim();
    sanitized = stripMarkdownFence(sanitized);

    int jsonStart = findJsonStart(sanitized);
    if (jsonStart == NOT_FOUND) {
      return sanitized;
    }

    int jsonEnd = findJsonEnd(sanitized, jsonStart);
    String jsonCandidate =
        jsonEnd == NOT_FOUND
            ? sanitized.substring(jsonStart)
            : sanitized.substring(jsonStart, jsonEnd + 1);
    return normalizeJsonLikeContent(stripJsonComments(jsonCandidate.trim()));
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
    char opening = value.charAt(start);
    char closing = opening == ARRAY_OPENING ? ARRAY_CLOSING : OBJECT_CLOSING;
    int depth = 0;
    boolean inString = false;
    boolean escaping = false;

    for (int index = start; index < value.length(); index++) {
      char current = value.charAt(index);
      boolean shouldSkip = escaping || inString || current == ESCAPE || current == QUOTE;
      boolean wasEscaping = escaping;
      escaping = !escaping && current == ESCAPE;
      if (!wasEscaping && current == QUOTE) {
        inString = !inString;
      }

      if (shouldSkip) {
        continue;
      }

      if (current == opening) {
        depth++;
      } else if (current == closing && --depth == 0) {
        return index;
      }
    }

    return NOT_FOUND;
  }

  private static String stripJsonComments(String value) {
    StringBuilder sanitized = new StringBuilder(value.length());
    boolean inString = false;
    boolean escaping = false;

    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      int commentEnd = findCommentEnd(value, index, inString);

      if (commentEnd != NOT_FOUND) {
        trimTrailingInlineWhitespace(sanitized);
        index = commentEnd;
      } else {
        sanitized.append(current);
      }

      boolean wasEscaping = escaping;
      escaping = !escaping && current == ESCAPE;
      if (!wasEscaping && current == QUOTE) {
        inString = !inString;
      }
    }

    return sanitized.toString().trim();
  }

  private static int findCommentEnd(String value, int index, boolean inString) {
    if (inString || value.charAt(index) != '/' || index + 1 >= value.length()) {
      return NOT_FOUND;
    }

    return switch (value.charAt(index + 1)) {
      case '/' -> scanLineCommentEnd(value, index + 2);
      case '*' -> scanBlockCommentEnd(value, index + 2);
      default -> NOT_FOUND;
    };
  }

  private static int scanLineCommentEnd(String value, int start) {
    for (int index = start; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current == '\n' || current == '\r') {
        return index - 1;
      }
    }
    return value.length();
  }

  private static int scanBlockCommentEnd(String value, int start) {
    for (int index = start; index + 1 < value.length(); index++) {
      if (value.charAt(index) == '*' && value.charAt(index + 1) == '/') {
        return index + 1;
      }
    }
    return value.length();
  }

  private static void trimTrailingInlineWhitespace(StringBuilder value) {
    int index = value.length() - 1;
    while (index >= 0) {
      char current = value.charAt(index);
      if (current == '\n' || current == '\r' || !Character.isWhitespace(current)) {
        break;
      }
      value.deleteCharAt(index--);
    }
  }

  private static String normalizeJsonLikeContent(String value) {
    StringBuilder sanitized = new StringBuilder(value.length() + 16);
    boolean inString = false;
    boolean escaping = false;

    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      sanitized.append(current);
      boolean wasEscaping = escaping;
      escaping = escaping ? false : current == ESCAPE;
      if (!wasEscaping && current == QUOTE) {
        inString = !inString;
      }

      if (escaping || inString || current != ':') {
        continue;
      }

      int tokenEnd = appendQuotedBareValue(value, index, sanitized);
      if (tokenEnd != NOT_FOUND) {
        index = tokenEnd - 1;
      }
    }

    return sanitized.toString();
  }

  private static int appendQuotedBareValue(String value, int colonIndex, StringBuilder sanitized) {
    int valueStart = colonIndex + 1;
    while (valueStart < value.length() && Character.isWhitespace(value.charAt(valueStart))) {
      valueStart++;
    }
    if (valueStart >= value.length()) {
      return NOT_FOUND;
    }

    char valueStartChar = value.charAt(valueStart);
    if (valueStartChar == QUOTE
        || valueStartChar == OBJECT_OPENING
        || valueStartChar == ARRAY_OPENING
        || matchesKeyword(value, valueStart, "true")
        || matchesKeyword(value, valueStart, "false")
        || matchesKeyword(value, valueStart, "null")
        || valueStartChar == '-'
        || Character.isDigit(valueStartChar)) {
      return NOT_FOUND;
    }

    int tokenEnd = valueStart;
    while (tokenEnd < value.length()) {
      char current = value.charAt(tokenEnd);
      if (current == ','
          || current == OBJECT_CLOSING
          || current == ARRAY_CLOSING
          || current == '\n'
          || current == '\r') {
        break;
      }
      tokenEnd++;
    }
    if (tokenEnd <= valueStart) {
      return NOT_FOUND;
    }

    sanitized.append(value, colonIndex + 1, valueStart);
    sanitized.append(QUOTE);
    sanitized.append(
        value.substring(valueStart, tokenEnd).trim().replace("\\", "\\\\").replace("\"", "\\\""));
    sanitized.append(QUOTE);
    return tokenEnd;
  }

  private static boolean matchesKeyword(String value, int index, String keyword) {
    int boundaryIndex = index + keyword.length();
    return value.regionMatches(index, keyword, 0, keyword.length())
        && (boundaryIndex >= value.length()
            || Character.isWhitespace(value.charAt(boundaryIndex))
            || value.charAt(boundaryIndex) == ','
            || value.charAt(boundaryIndex) == OBJECT_CLOSING
            || value.charAt(boundaryIndex) == ARRAY_CLOSING);
  }

}
