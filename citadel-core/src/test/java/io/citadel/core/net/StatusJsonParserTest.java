package io.citadel.core.net;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.network.ServerListPingStatus;
import org.junit.jupiter.api.Test;

class StatusJsonParserTest {

  @Test
  void parseFullStatusResponse() {
    String json =
        "{"
            + "\"version\":{\"name\":\"Paper 1.21\",\"protocol\":767},"
            + "\"players\":{\"max\":100,\"online\":42,\"sample\":[]},"
            + "\"description\":{\"text\":\"Hello World\"}"
            + "}";
    ServerListPingStatus status = StatusJsonParser.parse(json);
    assertEquals(767, status.getProtocolVersion());
    assertEquals("Paper 1.21", status.getVersion());
    assertEquals("Hello World", status.getMotd());
    assertEquals(100, status.getMaxPlayers());
    assertEquals(42, status.getOnlinePlayers());
  }

  @Test
  void parseWithStringDescription() {
    String json =
        "{"
            + "\"version\":{\"name\":\"Vanilla\",\"protocol\":766},"
            + "\"players\":{\"max\":20,\"online\":0},"
            + "\"description\":\"A Minecraft Server\""
            + "}";
    ServerListPingStatus status = StatusJsonParser.parse(json);
    assertEquals("A Minecraft Server", status.getMotd());
  }

  @Test
  void parseWithMinimalFields() {
    String json =
        "{"
            + "\"version\":{\"name\":\"\",\"protocol\":0},"
            + "\"players\":{\"max\":0,\"online\":0},"
            + "\"description\":{\"text\":\"\"}"
            + "}";
    ServerListPingStatus status = StatusJsonParser.parse(json);
    assertEquals(0, status.getProtocolVersion());
    assertEquals("", status.getVersion());
    assertEquals("", status.getMotd());
    assertEquals(0, status.getMaxPlayers());
    assertEquals(0, status.getOnlinePlayers());
  }

  @Test
  void parseWithMissingFields() {
    String json = "{}";
    ServerListPingStatus status = StatusJsonParser.parse(json);
    assertEquals(0, status.getProtocolVersion());
    assertEquals("", status.getVersion());
    assertEquals("", status.getMotd());
    assertEquals(0, status.getMaxPlayers());
    assertEquals(0, status.getOnlinePlayers());
  }

  @Test
  void parseRealWorldExample() {
    String json =
        "{\"version\":{\"name\":\"Spigot 1.20.4\",\"protocol\":765},"
            + "\"players\":{\"max\":200,\"online\":15,\"sample\":[]},"
            + "\"description\":{\"text\":\"§aWelcome!\"},"
            + "\"favicon\":\"data:image/png;base64,...\"}";
    ServerListPingStatus status = StatusJsonParser.parse(json);
    assertEquals(765, status.getProtocolVersion());
    assertEquals("Spigot 1.20.4", status.getVersion());
    assertEquals("§aWelcome!", status.getMotd());
    assertEquals(200, status.getMaxPlayers());
    assertEquals(15, status.getOnlinePlayers());
  }
}
