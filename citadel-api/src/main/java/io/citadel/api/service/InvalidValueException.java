package io.citadel.api.service;

/**
 * Thrown when a configuration value has a type that does not match the requested accessor.
 *
 * <p>The exception message includes the expected type and the actual value, enabling precise error
 * messages such as:
 *
 * <pre>{@code
 * Configuration value 'accounts.default.proxy.port' expected int but got String("hello")
 * }</pre>
 */
public class InvalidValueException extends ConfigurationException {

  private final String expectedType;
  private final Object actualValue;

  private static final long serialVersionUID = 1L;

  /**
   * @param path the configuration path with the invalid value
   * @param expectedType a human-readable description of the expected type
   * @param actualValue the actual value that was found
   */
  public InvalidValueException(String path, String expectedType, Object actualValue) {
    super(
        path,
        "Configuration value '"
            + path
            + "' expected "
            + expectedType
            + " but got "
            + describe(actualValue));
    this.expectedType = expectedType;
    this.actualValue = actualValue;
  }

  /**
   * @param path the configuration path with the invalid value
   * @param expectedType a human-readable description of the expected type
   * @param actualValue the actual value that was found
   * @param cause the underlying cause
   */
  public InvalidValueException(
      String path, String expectedType, Object actualValue, Throwable cause) {
    super(
        path,
        "Configuration value '"
            + path
            + "' expected "
            + expectedType
            + " but got "
            + describe(actualValue),
        cause);
    this.expectedType = expectedType;
    this.actualValue = actualValue;
  }

  /** Returns a human-readable description of the expected type. */
  public String getExpectedType() {
    return expectedType;
  }

  /** Returns the actual value that was found (may be null). */
  public Object getActualValue() {
    return actualValue;
  }

  private static String describe(Object value) {
    if (value == null) {
      return "null";
    }
    return value.getClass().getSimpleName() + "(\"" + value + "\")";
  }
}
