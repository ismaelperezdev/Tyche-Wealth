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
      return normalizeJsonLikeContent(stripJsonComments(sanitized.substring(jsonStart).trim()));
    }

    return normalizeJsonLikeContent(
        stripJsonComments(sanitized.substring(jsonStart, jsonEnd + 1).trim()));
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

  private static String stripJsonComments(String value) {
    StringBuilder sanitized = new StringBuilder(value.length());
    boolean inString = false;
    boolean escaping = false;

    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);

      int commentEnd = skipComment(value, index, inString, sanitized);
      if (commentEnd != NOT_FOUND) {
        index = commentEnd;
      } else {
        sanitized.append(current);
        boolean wasEscaping = escaping;
        escaping = updateEscapingState(current, escaping);
        inString = updateStringState(current, inString, wasEscaping);
      }
    }

    return sanitized.toString().trim();
  }

  private static int skipComment(
      String value, int index, boolean inString, StringBuilder sanitized) {
    if (inString || value.charAt(index) != '/' || index + 1 >= value.length()) {
      return NOT_FOUND;
    }

    char next = value.charAt(index + 1);
    return switch (next) {
      case '/' -> {
        trimTrailingInlineWhitespace(sanitized);
        yield skipLineComment(value, index + 2);
      }
      case '*' -> {
        trimTrailingInlineWhitespace(sanitized);
        yield skipBlockComment(value, index + 2);
      }
      default -> NOT_FOUND;
    };
  }

  private static void trimTrailingInlineWhitespace(StringBuilder value) {
    int index = value.length() - 1;
    while (index >= 0) {
      char current = value.charAt(index);
      if (current == '\n' || current == '\r' || !Character.isWhitespace(current)) {
        break;
      }
      value.deleteCharAt(index);
      index--;
    }
  }

  private static String normalizeJsonLikeContent(String value) {
    return quoteUnquotedStringValues(value);
  }

  private static String quoteUnquotedStringValues(String value) {
    StringBuilder sanitized = new StringBuilder(value.length() + 16);
    JsonStringState state = new JsonStringState();

    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      sanitized.append(current);
      state.advance(current);

      if (shouldSkipQuoting(current, state)) {
        continue;
      }

      int tokenEnd = appendQuotedBareValue(value, index, sanitized);
      if (tokenEnd != NOT_FOUND) {
        index = tokenEnd - 1;
      }
    }

    return sanitized.toString();
  }

  private static boolean shouldSkipQuoting(char current, JsonStringState state) {
    return state.isEscaping() || state.isInString() || current != ':';
  }

  private static int appendQuotedBareValue(String value, int index, StringBuilder sanitized) {
    int valueStart = skipWhitespace(value, index + 1);
    if (valueStart >= value.length()) {
      return NOT_FOUND;
    }

    char valueStartChar = value.charAt(valueStart);
    if (startsJsonLiteral(valueStartChar)
        || startsReservedLiteral(value, valueStart)
        || startsNumber(valueStartChar)) {
      return NOT_FOUND;
    }

    int tokenEnd = findBareTokenEnd(value, valueStart);
    if (tokenEnd <= valueStart) {
      return NOT_FOUND;
    }

    sanitized.append(value, index + 1, valueStart);
    sanitized.append(QUOTE);
    sanitized.append(escapeJsonString(value.substring(valueStart, tokenEnd).trim()));
    sanitized.append(QUOTE);
    return tokenEnd;
  }

  private static int skipWhitespace(String value, int index) {
    int current = index;
    while (current < value.length() && Character.isWhitespace(value.charAt(current))) {
      current++;
    }
    return current;
  }

  private static boolean startsJsonLiteral(char current) {
    return current == QUOTE || current == OBJECT_OPENING || current == ARRAY_OPENING;
  }

  private static boolean startsReservedLiteral(String value, int index) {
    return startsKeyword(value, index, "true")
        || startsKeyword(value, index, "false")
        || startsKeyword(value, index, "null");
  }

  private static boolean startsKeyword(String value, int index, String keyword) {
    return value.regionMatches(index, keyword, 0, keyword.length())
        && isKeywordBoundary(value, index + keyword.length());
  }

  private static boolean isKeywordBoundary(String value, int index) {
    return index >= value.length()
        || Character.isWhitespace(value.charAt(index))
        || value.charAt(index) == ','
        || value.charAt(index) == OBJECT_CLOSING
        || value.charAt(index) == ARRAY_CLOSING;
  }

  private static boolean startsNumber(char current) {
    return current == '-' || Character.isDigit(current);
  }

  private static int findBareTokenEnd(String value, int start) {
    int index = start;
    while (index < value.length()) {
      char current = value.charAt(index);
      if (current == ','
          || current == OBJECT_CLOSING
          || current == ARRAY_CLOSING
          || current == '\n'
          || current == '\r') {
        break;
      }
      index++;
    }
    return index;
  }

  private static String escapeJsonString(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static int skipLineComment(String value, int index) {
    while (index < value.length()) {
      char current = value.charAt(index);
      if (current == '\n' || current == '\r') {
        return index - 1;
      }
      index++;
    }
    return value.length();
  }

  private static int skipBlockComment(String value, int index) {
    while (index + 1 < value.length()) {
      if (value.charAt(index) == '*' && value.charAt(index + 1) == '/') {
        return index + 1;
      }
      index++;
    }
    return value.length();
  }

  private record JsonDelimiters(char opening, char closing) {

    private static JsonDelimiters fromOpening(char opening) {
      if (opening == ARRAY_OPENING) {
        return new JsonDelimiters(ARRAY_OPENING, ARRAY_CLOSING);
      }
      return new JsonDelimiters(OBJECT_OPENING, OBJECT_CLOSING);
    }
  }

  private static final class JsonStringState {
    private boolean inString;
    private boolean escaping;

    private void advance(char current) {
      boolean wasEscaping = escaping;
      escaping = updateEscapingState(current, escaping);
      inString = updateStringState(current, inString, wasEscaping);
    }

    private boolean isInString() {
      return inString;
    }

    private boolean isEscaping() {
      return escaping;
    }
  }
}
