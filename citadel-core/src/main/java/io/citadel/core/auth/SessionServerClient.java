package io.citadel.core.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.UUID;

public final class SessionServerClient {

  private static final String JOIN_URL = "https://sessionserver.mojang.com/session/minecraft/join";
  private static final int HTTP_TIMEOUT_SECONDS = 10;

  private final Gson gson;
  private final HttpClient httpClient;

  public SessionServerClient() {
    this.gson = new Gson();
    this.httpClient =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS)).build();
  }

  public void joinServer(String accessToken, UUID profileId, String serverId)
      throws AuthenticationException {
    try {
      String profileIdNoDash = profileId.toString().replace("-", "");
      JsonObject body = new JsonObject();
      body.addProperty("accessToken", accessToken);
      body.addProperty("selectedProfile", profileIdNoDash);
      body.addProperty("serverId", serverId);
      String jsonBody = gson.toJson(body);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(JOIN_URL))
              .header("Content-Type", "application/json")
              .POST(BodyPublishers.ofString(jsonBody))
              .build();
      HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
      if (response.statusCode() != 204) {
        throw new AuthenticationException(
            "Session server join failed: HTTP " + response.statusCode() + " " + response.body());
      }
    } catch (IOException | InterruptedException e) {
      throw new AuthenticationException("Session server join failed: " + e.getMessage(), e);
    }
  }
}
