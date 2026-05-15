package com.tychewealth.ai;

public final class AiResponseSanitizer {

  private static final int NOT_FOUND = -1;
  private static final String MARKDOWN_FENCE = "```";
  private static final String TRUE = "true";
  private static final String FALSE = "false";
  private static final String NULL = "null";
  private static final char ARRAY_OPENING = '[';
  private static final char ARRAY_CLOSING = ']';
  private static final char OBJECT_OPENING = '{';
  private static final char OBJECT_CLOSING = '}';
  private static final char QUOTE = '"';
  private static final char ESCAPE = '\\';
  private static final char COLON = ':';
  private static final char COMMA = ',';
  private static final char MINUS = '-';
  private static final char SLASH = '/';
  private static final char ASTERISK = '*';
  private static final char LINE_FEED = '\n';
  private static final char CARRIAGE_RETURN = '\r';

  private AiResponseSanitizer() {}

  public static String sanitizeAiResponse(String aiResponse) {
    String response = aiResponse == null ? "" : aiResponse.trim();
    int arrayStart;
    int objectStart;
    int jsonStart = NOT_FOUND;
    int jsonEnd;

    response = stripMarkdownFence(response);

    arrayStart = response.indexOf(ARRAY_OPENING);
    objectStart = response.indexOf(OBJECT_OPENING);
    if (arrayStart == NOT_FOUND) {
      jsonStart = objectStart;
    }
    if (arrayStart != NOT_FOUND && objectStart == NOT_FOUND) {
      jsonStart = arrayStart;
    }
    if (arrayStart != NOT_FOUND && objectStart != NOT_FOUND) {
      jsonStart = Math.min(arrayStart, objectStart);
    }

    if (jsonStart == NOT_FOUND) {
      return response;
    }

    jsonEnd = findJsonEnd(response, jsonStart);
    String jsonCandidate =
        jsonEnd == NOT_FOUND
            ? response.substring(jsonStart)
            : response.substring(jsonStart, jsonEnd + 1);
    return quoteBareStringValues(stripJsonComments(jsonCandidate.trim()));
  }

  private static String stripMarkdownFence(String value) {
    if (!value.startsWith(MARKDOWN_FENCE)) {
      return value;
    }

    int firstLineBreak = value.indexOf(LINE_FEED);
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

  private static int findJsonEnd(String value, int start) {
    char opening = value.charAt(start);
    char closing = opening == ARRAY_OPENING ? ARRAY_CLOSING : OBJECT_CLOSING;
    int depth = 0;
    int index = 0;
    int currentIndex;
    boolean inString = false;
    boolean escaping = false;
    boolean structural;
    boolean nextInString;
    boolean nextEscaping;

    for (char current : value.toCharArray()) {
      currentIndex = index++;
      structural =
          currentIndex >= start && !inString && !escaping && current != ESCAPE && current != QUOTE;
      nextEscaping = nextEscaping(current, escaping);
      nextInString = nextInString(current, inString, escaping);
      escaping = nextEscaping;
      inString = nextInString;

      if (!structural) {
        continue;
      }

      if (current == opening) {
        depth++;
      }

      if (current == closing && --depth == 0) {
        return currentIndex;
      }
    }

    return NOT_FOUND;
  }

  private static String stripJsonComments(String value) {
    StringBuilder sanitized = new StringBuilder(value.length());
    int index = 0;
    int skipUntil = NOT_FOUND;
    int currentIndex;
    boolean inString = false;
    boolean escaping = false;
    CommentStripState stepResult;

    for (char current : value.toCharArray()) {
      currentIndex = index++;
      stepResult =
          stripJsonCommentStep(
              value, sanitized, current, currentIndex, skipUntil, inString, escaping);
      skipUntil = stepResult.skipUntil();
      inString = stepResult.inString();
      escaping = stepResult.escaping();
    }

    return sanitized.toString().trim();
  }

  private static CommentStripState stripJsonCommentStep(
      String value,
      StringBuilder sanitized,
      char current,
      int currentIndex,
      int skipUntil,
      boolean inString,
      boolean escaping) {
    int commentEnd;
    int trimStart;

    if (skipUntil != NOT_FOUND && currentIndex <= skipUntil) {
      return new CommentStripState(skipUntil, inString, escaping);
    }

    commentEnd = findCommentEnd(value, currentIndex, inString);
    if (commentEnd == NOT_FOUND) {
      sanitized.append(current);
      return new CommentStripState(
          skipUntil, nextInString(current, inString, escaping), nextEscaping(current, escaping));
    }

    trimStart = sanitized.length();
    for (int trimIndex = sanitized.length() - 1; trimIndex >= 0; trimIndex--) {
      char trimmed = sanitized.charAt(trimIndex);
      if (trimmed == LINE_FEED || trimmed == CARRIAGE_RETURN || !Character.isWhitespace(trimmed)) {
        break;
      }
      trimStart = trimIndex;
    }
    if (trimStart < sanitized.length()) {
      sanitized.setLength(trimStart);
    }

    return new CommentStripState(commentEnd, inString, escaping);
  }

  private static int findCommentEnd(String value, int index, boolean inString) {
    char next;
    int commentEnd;
    int lineFeed;
    int carriageReturn;

    if (inString || value.charAt(index) != SLASH || index + 1 >= value.length()) {
      return NOT_FOUND;
    }

    next = value.charAt(index + 1);
    if (next == ASTERISK) {
      commentEnd = value.indexOf("*/", index + 2);
      return commentEnd == NOT_FOUND ? value.length() : commentEnd + 1;
    }

    if (next != SLASH) {
      return NOT_FOUND;
    }

    lineFeed = value.indexOf(LINE_FEED, index + 2);
    carriageReturn = value.indexOf(CARRIAGE_RETURN, index + 2);
    if (lineFeed == NOT_FOUND) {
      return carriageReturn == NOT_FOUND ? value.length() : carriageReturn - 1;
    }

    if (carriageReturn == NOT_FOUND) {
      return lineFeed - 1;
    }

    return Math.min(lineFeed, carriageReturn) - 1;
  }

  private static String quoteBareStringValues(String value) {
    StringBuilder sanitized = new StringBuilder(value.length() + 16);
    int index = 0;
    int skipUntil = NOT_FOUND;
    int tokenEnd;
    boolean inString = false;
    boolean escaping = false;
    boolean skipCurrent;
    boolean nextInString;
    boolean nextEscaping;

    for (char current : value.toCharArray()) {
      skipCurrent = index <= skipUntil;
      if (!skipCurrent) {
        sanitized.append(current);
        nextEscaping = nextEscaping(current, escaping);
        nextInString = nextInString(current, inString, escaping);
        escaping = nextEscaping;
        inString = nextInString;
      }

      if (skipCurrent || inString || current != COLON) {
        index++;
        continue;
      }

      tokenEnd = appendQuotedBareValue(value, index, sanitized);
      if (tokenEnd != NOT_FOUND) {
        skipUntil = tokenEnd - 1;
      }
      index++;
    }

    return sanitized.toString();
  }

  private static int appendQuotedBareValue(String value, int colonIndex, StringBuilder sanitized) {
    int valueStart =
        java.util.stream.IntStream.range(colonIndex + 1, value.length())
            .filter(index -> !Character.isWhitespace(value.charAt(index)))
            .findFirst()
            .orElse(value.length());
    if (valueStart >= value.length() || isJsonValueAlreadyValid(value, valueStart)) {
      return NOT_FOUND;
    }

    int tokenEnd = findBareValueEnd(value, valueStart);
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

  private static boolean isJsonValueAlreadyValid(String value, int valueStart) {
    char valueStartChar = value.charAt(valueStart);
    return valueStartChar == QUOTE
        || valueStartChar == OBJECT_OPENING
        || valueStartChar == ARRAY_OPENING
        || valueStartChar == MINUS
        || Character.isDigit(valueStartChar)
        || matchesKeyword(value, valueStart, TRUE)
        || matchesKeyword(value, valueStart, FALSE)
        || matchesKeyword(value, valueStart, NULL);
  }

  private static int findBareValueEnd(String value, int start) {
    int index = 0;
    for (char current : value.toCharArray()) {
      if (index < start) {
        index++;
        continue;
      }

      if (current == COMMA
          || current == OBJECT_CLOSING
          || current == ARRAY_CLOSING
          || current == LINE_FEED
          || current == CARRIAGE_RETURN) {
        return index;
      }

      index++;
    }
    return value.length();
  }

  private static boolean matchesKeyword(String value, int index, String keyword) {
    int boundaryIndex = index + keyword.length();
    return value.regionMatches(index, keyword, 0, keyword.length())
        && (boundaryIndex >= value.length()
            || Character.isWhitespace(value.charAt(boundaryIndex))
            || value.charAt(boundaryIndex) == COMMA
            || value.charAt(boundaryIndex) == OBJECT_CLOSING
            || value.charAt(boundaryIndex) == ARRAY_CLOSING);
  }

  private static boolean nextEscaping(char current, boolean escaping) {
    return !escaping && current == ESCAPE;
  }

  private static boolean nextInString(char current, boolean inString, boolean escaping) {
    return (!escaping && current == QUOTE) != inString;
  }

  private record CommentStripState(int skipUntil, boolean inString, boolean escaping) {}
}
