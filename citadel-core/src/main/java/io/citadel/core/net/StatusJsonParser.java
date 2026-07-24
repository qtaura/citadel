package io.citadel.core.net;

import io.citadel.api.network.ServerListPingStatus;

final class StatusJsonParser {

  private StatusJsonParser() {}

  static ServerListPingStatus parse(String json) {
    int protocolVersion = extractInt(json, "\"protocol\":");
    String serverVersion = extractString(json, "\"name\":");
    String motd = extractMotd(json);
    int maxPlayers = extractInt(json, "\"max\":");
    int onlinePlayers = extractInt(json, "\"online\":");

    return new ServerListPingStatus(
        protocolVersion, serverVersion, motd, maxPlayers, onlinePlayers);
  }

  private static int extractInt(String json, String key) {
    int idx = json.indexOf(key);
    if (idx < 0) {
      return 0;
    }
    int start = idx + key.length();
    while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
      start++;
    }
    int end = start;
    while (end < json.length()
        && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
      end++;
    }
    return Integer.parseInt(json.substring(start, end));
  }

  private static String extractString(String json, String key) {
    int idx = json.indexOf(key);
    if (idx < 0) {
      return "";
    }
    int start = idx + key.length();
    while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
      start++;
    }
    if (start < json.length() && json.charAt(start) == '"') {
      start++;
      int end = start;
      while (end < json.length() && json.charAt(end) != '"') {
        if (json.charAt(end) == '\\') {
          end++;
        }
        end++;
      }
      return json.substring(start, end);
    }
    return "";
  }

  private static String extractMotd(String json) {
    int descIdx = json.indexOf("\"description\":");
    if (descIdx < 0) {
      return "";
    }
    int start = descIdx + "\"description\":".length();
    while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
      start++;
    }
    if (start < json.length() && json.charAt(start) == '"') {
      return extractString(json.substring(descIdx), "\"description\":");
    }
    if (start < json.length() && json.charAt(start) == '{') {
      return extractString(json.substring(descIdx), "\"text\":");
    }
    return "";
  }
}
