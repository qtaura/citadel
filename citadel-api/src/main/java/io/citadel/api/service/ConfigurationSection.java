package io.citadel.api.service;

import java.util.List;
import java.util.Set;

/**
 * A named section within the Citadel configuration hierarchy.
 *
 * <p>Provides strongly typed accessors for configuration values. Each accessor accepts a
 * dot-separated path (e.g. {@code "accounts.default.proxy.port"}) and returns the value at that
 * path within this section.
 *
 * <p>Every accessor comes in two forms:
 *
 * <ul>
 *   <li>Without a default value — throws {@link MissingValueException} if the path does not exist
 *       and {@link InvalidValueException} if the value has the wrong type.
 *   <li>With a default value — returns the default if the path does not exist and throws {@link
 *       InvalidValueException} if the value has the wrong type.
 * </ul>
 *
 * <p>All methods are thread-safe.
 */
public interface ConfigurationSection {

  /**
   * Returns the string value at the given path.
   *
   * @param path dot-separated configuration path
   * @return the string value
   * @throws MissingValueException if the path does not exist
   * @throws InvalidValueException if the value is not a string
   */
  String getString(String path);

  /**
   * Returns the string value at the given path, or {@code defaultValue} if the path does not exist.
   *
   * @param path dot-separated configuration path
   * @param defaultValue returned when the path is absent
   * @return the string value or default
   * @throws InvalidValueException if the value exists but is not a string
   */
  String getString(String path, String defaultValue);

  /**
   * Returns the boolean value at the given path.
   *
   * @param path dot-separated configuration path
   * @return the boolean value
   * @throws MissingValueException if the path does not exist
   * @throws InvalidValueException if the value is not a boolean
   */
  boolean getBoolean(String path);

  /**
   * Returns the boolean value at the given path, or {@code defaultValue} if the path does not
   * exist.
   *
   * @param path dot-separated configuration path
   * @param defaultValue returned when the path is absent
   * @return the boolean value or default
   * @throws InvalidValueException if the value exists but is not a boolean
   */
  boolean getBoolean(String path, boolean defaultValue);

  /**
   * Returns the integer value at the given path.
   *
   * @param path dot-separated configuration path
   * @return the integer value
   * @throws MissingValueException if the path does not exist
   * @throws InvalidValueException if the value is not an integer
   */
  int getInt(String path);

  /**
   * Returns the integer value at the given path, or {@code defaultValue} if the path does not
   * exist.
   *
   * @param path dot-separated configuration path
   * @param defaultValue returned when the path is absent
   * @return the integer value or default
   * @throws InvalidValueException if the value exists but is not an integer
   */
  int getInt(String path, int defaultValue);

  /**
   * Returns the long value at the given path.
   *
   * @param path dot-separated configuration path
   * @return the long value
   * @throws MissingValueException if the path does not exist
   * @throws InvalidValueException if the value is not a long
   */
  long getLong(String path);

  /**
   * Returns the long value at the given path, or {@code defaultValue} if the path does not exist.
   *
   * @param path dot-separated configuration path
   * @param defaultValue returned when the path is absent
   * @return the long value or default
   * @throws InvalidValueException if the value exists but is not a long
   */
  long getLong(String path, long defaultValue);

  /**
   * Returns the double value at the given path.
   *
   * @param path dot-separated configuration path
   * @return the double value
   * @throws MissingValueException if the path does not exist
   * @throws InvalidValueException if the value is not a double
   */
  double getDouble(String path);

  /**
   * Returns the double value at the given path, or {@code defaultValue} if the path does not exist.
   *
   * @param path dot-separated configuration path
   * @param defaultValue returned when the path is absent
   * @return the double value or default
   * @throws InvalidValueException if the value exists but is not a double
   */
  double getDouble(String path, double defaultValue);

  /**
   * Returns the enum constant at the given path.
   *
   * <p>The configuration value must be a string matching one of the enum constant names (case
   * sensitive).
   *
   * @param path dot-separated configuration path
   * @param enumClass the enum type
   * @return the enum constant
   * @throws MissingValueException if the path does not exist
   * @throws InvalidValueException if the value is not a string or does not match any enum constant
   */
  <T extends Enum<T>> T getEnum(String path, Class<T> enumClass);

  /**
   * Returns the enum constant at the given path, or {@code defaultValue} if the path does not
   * exist.
   *
   * @param path dot-separated configuration path
   * @param enumClass the enum type
   * @param defaultValue returned when the path is absent
   * @return the enum constant or default
   * @throws InvalidValueException if the value exists but is not a string or does not match any
   *     enum constant
   */
  <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue);

  /**
   * Returns the string list at the given path.
   *
   * @param path dot-separated configuration path
   * @return the string list (unmodifiable)
   * @throws MissingValueException if the path does not exist
   * @throws InvalidValueException if the value is not a list
   */
  List<String> getStringList(String path);

  /**
   * Returns the list at the given path.
   *
   * @param path dot-separated configuration path
   * @return the list (unmodifiable)
   * @throws MissingValueException if the path does not exist
   * @throws InvalidValueException if the value is not a list
   */
  <T> List<T> getList(String path);

  /**
   * Returns the sub-section at the given path.
   *
   * <p>If the path does not exist, an empty section is returned (never null).
   *
   * @param path dot-separated configuration path
   * @return the sub-section (never null)
   * @throws InvalidValueException if the value exists but is not a section (map)
   */
  ConfigurationSection getSection(String path);

  /**
   * Returns whether the given path exists in this configuration.
   *
   * @param path dot-separated configuration path
   * @return true if the path exists
   */
  boolean contains(String path);

  /**
   * Returns the set of direct child keys at the root of this section.
   *
   * @return unmodifiable set of keys (never null)
   */
  Set<String> getKeys();
}
