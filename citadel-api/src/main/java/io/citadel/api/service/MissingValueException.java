package io.citadel.api.service;

/**
 * Thrown when a required configuration value is not present.
 *
 * <p>This exception is thrown by the no-default overloads of typed accessors. Callers who wish to
 * provide a fallback should use the overloads that accept a default value.
 */
public class MissingValueException extends ConfigurationException {

  private static final long serialVersionUID = 1L;

  /**
   * @param path the configuration path that is missing
   */
  public MissingValueException(String path) {
    super(path, "Configuration value '" + path + "' is not present");
  }
}
