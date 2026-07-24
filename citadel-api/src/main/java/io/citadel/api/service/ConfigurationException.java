package io.citadel.api.service;

/**
 * Base exception for all configuration-related errors.
 *
 * <p>Every configuration exception carries the {@link #getPath() configuration path} that caused
 * the error, enabling precise error messages such as {@code "accounts.default.proxy.port"}.
 */
public class ConfigurationException extends RuntimeException {

  private final String path;

  private static final long serialVersionUID = 1L;

  /**
   * @param path the configuration path that caused the error
   * @param message human-readable error description
   */
  public ConfigurationException(String path, String message) {
    super(message);
    this.path = path;
  }

  /**
   * @param path the configuration path that caused the error
   * @param message human-readable error description
   * @param cause the underlying cause
   */
  public ConfigurationException(String path, String message, Throwable cause) {
    super(message, cause);
    this.path = path;
  }

  /** Returns the configuration path that caused this error. */
  public String getPath() {
    return path;
  }
}
