package io.citadel.core.config;

import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.InvalidValueException;
import io.citadel.api.service.MissingValueException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link ConfigurationSection} backed by a {@code Map<String, Object>}.
 *
 * <p>This class provides all the typed accessor logic. It is used for both the root configuration
 * and every nested subsection.
 */
final class ConfigurationSectionImpl implements ConfigurationSection {

  private static final ConfigurationSection EMPTY =
      new ConfigurationSectionImpl(Collections.emptyMap());

  private final Map<String, Object> raw;

  ConfigurationSectionImpl(Map<String, Object> raw) {
    this.raw = raw;
  }

  @Override
  public String getString(String path) {
    return convertString(resolve(path), path);
  }

  @Override
  public String getString(String path, String defaultValue) {
    Object value = resolve(path);
    if (value == null) {
      return defaultValue;
    }
    return convertString(value, path);
  }

  @Override
  public boolean getBoolean(String path) {
    return convertBoolean(resolve(path), path);
  }

  @Override
  public boolean getBoolean(String path, boolean defaultValue) {
    Object value = resolve(path);
    if (value == null) {
      return defaultValue;
    }
    return convertBoolean(value, path);
  }

  @Override
  public int getInt(String path) {
    return convertInt(resolve(path), path);
  }

  @Override
  public int getInt(String path, int defaultValue) {
    Object value = resolve(path);
    if (value == null) {
      return defaultValue;
    }
    return convertInt(value, path);
  }

  @Override
  public long getLong(String path) {
    return convertLong(resolve(path), path);
  }

  @Override
  public long getLong(String path, long defaultValue) {
    Object value = resolve(path);
    if (value == null) {
      return defaultValue;
    }
    return convertLong(value, path);
  }

  @Override
  public double getDouble(String path) {
    return convertDouble(resolve(path), path);
  }

  @Override
  public double getDouble(String path, double defaultValue) {
    Object value = resolve(path);
    if (value == null) {
      return defaultValue;
    }
    return convertDouble(value, path);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
    String name = convertString(resolve(path), path);
    try {
      return Enum.valueOf(enumClass, name);
    } catch (IllegalArgumentException e) {
      throw new InvalidValueException(
          path, "one of " + enumClass.getSimpleName() + " constants", name, e);
    }
  }

  @Override
  public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
    Object value = resolve(path);
    if (value == null) {
      return defaultValue;
    }
    String name = convertString(value, path);
    try {
      return Enum.valueOf(enumClass, name);
    } catch (IllegalArgumentException e) {
      throw new InvalidValueException(
          path, "one of " + enumClass.getSimpleName() + " constants", name, e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getStringList(String path) {
    List<Object> rawList = convertList(resolve(path), path);
    List<String> result = new ArrayList<>(rawList.size());
    for (int i = 0; i < rawList.size(); i++) {
      Object element = rawList.get(i);
      if (!(element instanceof String)) {
        throw new InvalidValueException(path + "[" + i + "]", "string", element);
      }
      result.add((String) element);
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> getList(String path) {
    List<Object> rawList = convertList(resolve(path), path);
    List<T> result = new ArrayList<>(rawList.size());
    for (Object element : rawList) {
      result.add((T) element);
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  @SuppressWarnings("unchecked")
  public ConfigurationSection getSection(String path) {
    Object value = resolve(path);
    if (value == null) {
      return EMPTY;
    }
    if (value instanceof Map) {
      return new ConfigurationSectionImpl(Collections.unmodifiableMap((Map<String, Object>) value));
    }
    throw new InvalidValueException(path, "section", value);
  }

  @Override
  public boolean contains(String path) {
    return resolve(path) != null;
  }

  @Override
  public Set<String> getKeys() {
    return raw.keySet();
  }

  /**
   * Resolves a dot-separated path against the raw map.
   *
   * @return the raw value, or null if the path does not exist
   */
  @SuppressWarnings("unchecked")
  Object resolve(String path) {
    String[] parts = path.split("\\.");
    Map<String, Object> current = raw;
    for (int i = 0; i < parts.length - 1; i++) {
      Object next = current.get(parts[i]);
      if (!(next instanceof Map)) {
        return null;
      }
      current = (Map<String, Object>) next;
    }
    return current.get(parts[parts.length - 1]);
  }

  Map<String, Object> getRaw() {
    return raw;
  }

  private static String convertString(Object value, String path) {
    if (value == null) {
      throw new MissingValueException(path);
    }
    if (value instanceof String) {
      return (String) value;
    }
    throw new InvalidValueException(path, "string", value);
  }

  private static boolean convertBoolean(Object value, String path) {
    if (value == null) {
      throw new MissingValueException(path);
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    throw new InvalidValueException(path, "boolean", value);
  }

  private static int convertInt(Object value, String path) {
    if (value == null) {
      throw new MissingValueException(path);
    }
    if (value instanceof Number) {
      Number number = (Number) value;
      long longValue = number.longValue();
      if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
        return (int) longValue;
      }
    }
    throw new InvalidValueException(path, "int", value);
  }

  private static long convertLong(Object value, String path) {
    if (value == null) {
      throw new MissingValueException(path);
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    throw new InvalidValueException(path, "long", value);
  }

  private static double convertDouble(Object value, String path) {
    if (value == null) {
      throw new MissingValueException(path);
    }
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    throw new InvalidValueException(path, "double", value);
  }

  @SuppressWarnings("unchecked")
  private static List<Object> convertList(Object value, String path) {
    if (value == null) {
      throw new MissingValueException(path);
    }
    if (value instanceof List) {
      return (List<Object>) value;
    }
    throw new InvalidValueException(path, "list", value);
  }
}
