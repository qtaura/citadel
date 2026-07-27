package io.citadel.core.auth;

/** Indicates that authentication or Minecraft login failed. */
public class AuthenticationException extends Exception {

  private static final long serialVersionUID = 1L;

  public AuthenticationException(String message) {
    super(message);
  }

  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
