package io.citadel.core.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class XboxDeviceFlowTest {
  public static void main(String[] args) throws Exception {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    // Try Xbox Live device code endpoint (no Azure AD client ID needed)
    String[] appIds = {
      "MCWin32", // Minecraft Java via Xbox
      "00000000402b5328", // fallback
    };

    for (String appId : appIds) {
      System.out.println("\n=== Testing Xbox device code with AppId=" + appId + " ===");
      try {
        String jsonBody = "{\"DeviceType\":\"Win32\",\"AppId\":\"" + appId + "\"}";
        HttpRequest req =
            HttpRequest.newBuilder()
                .uri(URI.create("https://device.login.xboxlive.com/device/request"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("HTTP " + resp.statusCode());
        System.out.println(resp.body().substring(0, Math.min(resp.body().length(), 500)));
      } catch (Exception e) {
        System.out.println("FAILED: " + e.getMessage());
      }
    }
  }
}
