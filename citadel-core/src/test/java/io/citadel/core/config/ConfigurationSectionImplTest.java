package io.citadel.core.config;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.service.InvalidValueException;
import io.citadel.api.service.MissingValueException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

enum TestLevel {
  INFO,
  WARN,
  ERROR
}

class ConfigurationSectionImplTest {

  private static ConfigurationSectionImpl section(Map<String, Object> raw) {
    return new ConfigurationSectionImpl(raw);
  }

  @Test
  void getStringReturnsValue() {
    ConfigurationSectionImpl s = section(Map.of("key", "value"));
    assertEquals("value", s.getString("key"));
  }

  @Test
  void getStringWithDefaultReturnsValue() {
    ConfigurationSectionImpl s = section(Map.of("key", "value"));
    assertEquals("value", s.getString("key", "default"));
  }

  @Test
  void getStringWithDefaultReturnsDefaultWhenMissing() {
    ConfigurationSectionImpl s = section(Map.of());
    assertEquals("default", s.getString("missing", "default"));
  }

  @Test
  void getStringThrowsWhenMissing() {
    ConfigurationSectionImpl s = section(Map.of());
    assertThrows(MissingValueException.class, () -> s.getString("missing"));
  }

  @Test
  void getStringThrowsWhenWrongType() {
    ConfigurationSectionImpl s = section(Map.of("key", 42));
    assertThrows(InvalidValueException.class, () -> s.getString("key"));
  }

  @Test
  void getBooleanReturnsValue() {
    ConfigurationSectionImpl s = section(Map.of("key", true));
    assertTrue(s.getBoolean("key"));
  }

  @Test
  void getBooleanWithDefaultReturnsDefaultWhenMissing() {
    ConfigurationSectionImpl s = section(Map.of());
    assertTrue(s.getBoolean("missing", true));
  }

  @Test
  void getBooleanThrowsWhenWrongType() {
    ConfigurationSectionImpl s = section(Map.of("key", "notabool"));
    assertThrows(InvalidValueException.class, () -> s.getBoolean("key"));
  }

  @Test
  void getIntReturnsValue() {
    ConfigurationSectionImpl s = section(Map.of("key", 42));
    assertEquals(42, s.getInt("key"));
  }

  @Test
  void getIntWithDefaultReturnsDefaultWhenMissing() {
    ConfigurationSectionImpl s = section(Map.of());
    assertEquals(10, s.getInt("missing", 10));
  }

  @Test
  void getIntThrowsWhenOverflow() {
    ConfigurationSectionImpl s = section(Map.of("key", Long.MAX_VALUE));
    assertThrows(InvalidValueException.class, () -> s.getInt("key"));
  }

  @Test
  void getLongReturnsValue() {
    ConfigurationSectionImpl s = section(Map.of("key", 42L));
    assertEquals(42L, s.getLong("key"));
  }

  @Test
  void getDoubleReturnsValue() {
    ConfigurationSectionImpl s = section(Map.of("key", 3.14));
    assertEquals(3.14, s.getDouble("key"), 1e-9);
  }

  @Test
  void getDoubleAcceptsInt() {
    ConfigurationSectionImpl s = section(Map.of("key", 42));
    assertEquals(42.0, s.getDouble("key"), 1e-9);
  }

  @Test
  void getEnumReturnsValue() {
    ConfigurationSectionImpl s = section(Map.of("key", "INFO"));
    assertEquals(TestLevel.INFO, s.getEnum("key", TestLevel.class));
  }

  @Test
  void getEnumWithDefaultReturnsDefaultWhenMissing() {
    ConfigurationSectionImpl s = section(Map.of());
    assertEquals(TestLevel.INFO, s.getEnum("missing", TestLevel.class, TestLevel.INFO));
  }

  @Test
  void getEnumThrowsOnInvalidConstant() {
    ConfigurationSectionImpl s = section(Map.of("key", "NONEXISTENT"));
    assertThrows(InvalidValueException.class, () -> s.getEnum("key", TestLevel.class));
  }

  @Test
  void getStringListReturnsValues() {
    ConfigurationSectionImpl s = section(Map.of("key", List.of("a", "b")));
    assertEquals(List.of("a", "b"), s.getStringList("key"));
  }

  @Test
  void getStringListThrowsOnNonStringElements() {
    ConfigurationSectionImpl s = section(Map.of("key", List.of("a", 42)));
    assertThrows(InvalidValueException.class, () -> s.getStringList("key"));
  }

  @Test
  void getListReturnsValues() {
    ConfigurationSectionImpl s = section(Map.of("key", List.of(1, 2, 3)));
    List<Integer> list = s.getList("key");
    assertEquals(List.of(1, 2, 3), list);
  }

  @Test
  void getListThrowsWhenMissing() {
    ConfigurationSectionImpl s = section(Map.of());
    assertThrows(MissingValueException.class, () -> s.getList("missing"));
  }

  @Test
  void getSectionReturnsSubsection() {
    ConfigurationSectionImpl s = section(Map.of("nested", Map.of("key", "value")));
    assertEquals("value", s.getSection("nested").getString("key"));
  }

  @Test
  void getSectionReturnsEmptyForMissingPath() {
    ConfigurationSectionImpl s = section(Map.of());
    assertNotNull(s.getSection("missing"));
    assertTrue(s.getSection("missing").getKeys().isEmpty());
  }

  @Test
  void getSectionThrowsOnNonMapValue() {
    ConfigurationSectionImpl s = section(Map.of("key", "notamap"));
    assertThrows(InvalidValueException.class, () -> s.getSection("key"));
  }

  @Test
  void containsReturnsTrue() {
    ConfigurationSectionImpl s = section(Map.of("key", "value"));
    assertTrue(s.contains("key"));
  }

  @Test
  void containsReturnsFalse() {
    ConfigurationSectionImpl s = section(Map.of());
    assertFalse(s.contains("missing"));
  }

  @Test
  void getKeysReturnsKeys() {
    ConfigurationSectionImpl s = section(Map.of("a", 1, "b", 2));
    assertEquals(Set.of("a", "b"), s.getKeys());
  }

  @Test
  void nestedPathResolution() {
    ConfigurationSectionImpl s =
        section(Map.of("accounts", Map.of("default", Map.of("proxy", Map.of("port", 8080)))));
    assertEquals(8080, s.getInt("accounts.default.proxy.port"));
  }

  @Test
  void nestedPathMissingReturnsNull() {
    ConfigurationSectionImpl s = section(Map.of("accounts", Map.of("default", Map.of())));
    assertFalse(s.contains("accounts.default.proxy"));
  }

  @Test
  void missingValueExceptionIncludesPath() {
    ConfigurationSectionImpl s = section(Map.of());
    MissingValueException e = assertThrows(MissingValueException.class, () -> s.getInt("foo.bar"));
    assertTrue(e.getPath().contains("foo.bar"));
  }

  @Test
  void invalidValueExceptionIncludesPathAndType() {
    ConfigurationSectionImpl s = section(Map.of("key", "hello"));
    InvalidValueException e = assertThrows(InvalidValueException.class, () -> s.getInt("key"));
    assertTrue(e.getPath().contains("key"));
    assertNotNull(e.getExpectedType());
    assertNotNull(e.getActualValue());
  }
}
