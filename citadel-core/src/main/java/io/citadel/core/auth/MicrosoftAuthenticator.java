package io.citadel.core.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class MicrosoftAuthenticator {

  private static final String CLIENT_ID = "00000000402b5328";
  private static final String DEVICE_CODE_URL =
      "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
  private static final String TOKEN_URL =
      "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
  private static final String XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
  private static final String XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
  private static final String MINECRAFT_LOGIN_URL =
      "https://api.minecraftservices.com/authentication/login_with_xbox";
  private static final String MINECRAFT_PROFILE_URL =
      "https://api.minecraftservices.com/minecraft/profile";
  private static final int HTTP_TIMEOUT_SECONDS = 30;

  private final Gson gson;
  private final HttpClient httpClient;

  public MicrosoftAuthenticator() {
    this.gson = new Gson();
    this.httpClient =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS)).build();
  }

  public DeviceCodeResult requestDeviceCode() throws AuthenticationException {
    String body =
        "client_id="
            + urlEncode(CLIENT_ID)
            + "&scope="
            + urlEncode("XboxLive.signin XboxLive.offline_access");
    String json = postForm(DEVICE_CODE_URL, body);
    try {
      JsonObject obj = gson.fromJson(json, JsonObject.class);
      String userCode = getString(obj, "user_code");
      String deviceCode = getString(obj, "device_code");
      String verificationUri = getString(obj, "verification_uri");
      int expiresIn = obj.get("expires_in").getAsInt();
      int interval = obj.get("interval").getAsInt();
      return new DeviceCodeResult(userCode, deviceCode, verificationUri, expiresIn, interval);
    } catch (Exception e) {
      throw new AuthenticationException(
          "Failed to parse device code response: " + e.getMessage(), e);
    }
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  public OAuthToken pollForToken(String deviceCode, int expiresIn, int interval)
      throws AuthenticationException, InterruptedException {
    long deadline = System.currentTimeMillis() + expiresIn * 1000L;
    while (System.currentTimeMillis() < deadline) {
      Thread.sleep(interval * 1000L);
      String body =
          "client_id="
              + urlEncode(CLIENT_ID)
              + "&grant_type="
              + urlEncode("urn:ietf:params:oauth:grant-type:device_code")
              + "&device_code="
              + urlEncode(deviceCode);
      String json = postForm(TOKEN_URL, body);
      try {
        JsonObject obj = gson.fromJson(json, JsonObject.class);
        if (obj.has("access_token")) {
          String accessToken = getString(obj, "access_token");
          String refreshToken = getString(obj, "refresh_token");
          int tokenExpiresIn = obj.get("expires_in").getAsInt();
          return new OAuthToken(
              accessToken, refreshToken, Instant.now().plusSeconds(tokenExpiresIn));
        }
        if (obj.has("error")) {
          String error = getString(obj, "error");
          if ("authorization_pending".equals(error) || "slow_down".equals(error)) {
            continue;
          }
          if ("expired_token".equals(error)) {
            throw new AuthenticationException("Device code expired");
          }
          throw new AuthenticationException("OAuth error: " + error);
        }
      } catch (JsonSyntaxException e) {
        throw new AuthenticationException("Failed to parse token response", e);
      }
    }
    throw new AuthenticationException("Device code flow timed out");
  }

  public OAuthToken refreshAccessToken(String refreshToken) throws AuthenticationException {
    String body =
        "client_id="
            + urlEncode(CLIENT_ID)
            + "&refresh_token="
            + urlEncode(refreshToken)
            + "&grant_type="
            + urlEncode("refresh_token")
            + "&scope="
            + urlEncode("XboxLive.signin XboxLive.offline_access");
    String json = postForm(TOKEN_URL, body);
    try {
      JsonObject obj = gson.fromJson(json, JsonObject.class);
      if (obj.has("error")) {
        throw new AuthenticationException(
            "Token refresh failed: " + getString(obj, "error_description"));
      }
      String accessToken = getString(obj, "access_token");
      String newRefreshToken = getString(obj, "refresh_token");
      int tokenExpiresIn = obj.get("expires_in").getAsInt();
      return new OAuthToken(
          accessToken, newRefreshToken, Instant.now().plusSeconds(tokenExpiresIn));
    } catch (JsonSyntaxException e) {
      throw new AuthenticationException("Failed to parse refresh token response", e);
    }
  }

  public XblToken authenticateXbl(String accessToken) throws AuthenticationException {
    String requestBody =
        gson.toJson(
            JsonObjectBuilder.create()
                .putObject(
                    "Properties",
                    JsonObjectBuilder.create()
                        .put("AuthMethod", "RPS")
                        .put("SiteName", "user.auth.xboxlive.com")
                        .put("RpsTicket", "d=" + accessToken))
                .put("RelyingParty", "http://auth.xboxlive.com")
                .put("TokenType", "JWT"));
    String json = postJson(XBL_AUTH_URL, requestBody);
    try {
      JsonObject obj = gson.fromJson(json, JsonObject.class);
      String token = getString(obj, "Token");
      String uhs =
          obj.getAsJsonObject("DisplayClaims")
              .getAsJsonArray("xui")
              .get(0)
              .getAsJsonObject()
              .get("uhs")
              .getAsString();
      return new XblToken(token, uhs);
    } catch (Exception e) {
      throw new AuthenticationException("XBL authentication failed: " + e.getMessage(), e);
    }
  }

  public XstsToken authenticateXsts(String xblToken) throws AuthenticationException {
    String requestBody =
        gson.toJson(
            JsonObjectBuilder.create()
                .putObject(
                    "Properties",
                    JsonObjectBuilder.create()
                        .put("SandboxId", "RETAIL")
                        .putArray("UserTokens", new String[] {xblToken}))
                .put("RelyingParty", "rp://api.minecraftservices.com/")
                .put("TokenType", "JWT"));
    String json = postJson(XSTS_AUTH_URL, requestBody);
    try {
      JsonObject obj = gson.fromJson(json, JsonObject.class);
      String token = getString(obj, "Token");
      String uhs =
          obj.getAsJsonObject("DisplayClaims")
              .getAsJsonArray("xui")
              .get(0)
              .getAsJsonObject()
              .get("uhs")
              .getAsString();
      return new XstsToken(token, uhs);
    } catch (Exception e) {
      throw new AuthenticationException("XSTS authentication failed: " + e.getMessage(), e);
    }
  }

  public MinecraftToken loginMinecraft(String uhs, String xstsToken)
      throws AuthenticationException {
    String identityToken = "XBL3.0 x=" + uhs + ";" + xstsToken;
    String requestBody =
        gson.toJson(JsonObjectBuilder.create().put("identityToken", identityToken));
    String json = postJson(MINECRAFT_LOGIN_URL, requestBody);
    try {
      JsonObject obj = gson.fromJson(json, JsonObject.class);
      String accessToken = getString(obj, "access_token");
      int expiresIn = obj.get("expires_in").getAsInt();
      return new MinecraftToken(accessToken, Instant.now().plusSeconds(expiresIn));
    } catch (Exception e) {
      throw new AuthenticationException("Minecraft login failed: " + e.getMessage(), e);
    }
  }

  public MinecraftProfile lookupProfile(String minecraftAccessToken)
      throws AuthenticationException {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(MINECRAFT_PROFILE_URL))
              .header("Authorization", "Bearer " + minecraftAccessToken)
              .GET()
              .build();
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new AuthenticationException(
            "Profile lookup failed: HTTP " + response.statusCode() + " " + response.body());
      }
      JsonObject obj = gson.fromJson(response.body(), JsonObject.class);
      String id = getString(obj, "id");
      String name = getString(obj, "name");
      UUID profileId = parseUuid(id);
      return new MinecraftProfile(profileId, name);
    } catch (IOException | InterruptedException e) {
      throw new AuthenticationException("Profile lookup failed: " + e.getMessage(), e);
    }
  }

  private String postForm(String url, String body) throws AuthenticationException {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Content-Type", "application/x-www-form-urlencoded")
              .POST(BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      return response.body();
    } catch (IOException | InterruptedException e) {
      throw new AuthenticationException("HTTP request failed: " + e.getMessage(), e);
    }
  }

  private String postJson(String url, String jsonBody) throws AuthenticationException {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Content-Type", "application/json")
              .POST(BodyPublishers.ofString(jsonBody))
              .build();
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      return response.body();
    } catch (IOException | InterruptedException e) {
      throw new AuthenticationException("HTTP request failed: " + e.getMessage(), e);
    }
  }

  private static String getString(JsonObject obj, String key) {
    return obj.get(key).getAsString();
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static UUID parseUuid(String uuidWithoutDashes) {
    String withDashes =
        uuidWithoutDashes.replaceFirst(
            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
    return UUID.fromString(withDashes);
  }

  public record DeviceCodeResult(
      String userCode, String deviceCode, String verificationUri, int expiresIn, int interval) {}

  public record OAuthToken(String accessToken, String refreshToken, Instant expiresAt) {}

  public record XblToken(String token, String uhs) {}

  public record XstsToken(String token, String uhs) {}

  public record MinecraftToken(String accessToken, Instant expiresAt) {}

  public record MinecraftProfile(UUID profileId, String username) {}
}
