package io.citadel.core.auth;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class FindWorkingClientId {
  public static void main(String[] args) throws Exception {
    String[][] candidates = {
      {"00000000402b5328", "old Minecraft Java Ed"},
      {"000000004C12AEE0", "old Minecraft Dungeons"},
      {"00000000482D5C52", "Minecraft for Win10"},
      {"bdf34548-3a6c-4030-aaf9-0a3a7c50b204", "Prism Launcher"},
      {"d0d7a7c0-5d6e-4f5d-b6c7-8c9d0a1b2c3d", "ATLauncher"},
      {"12ef0f80-b2a1-4bb4-9dfd-8ee52aa4b18f", "MultiMC"},
      {"00000000000000000000000000000000", "test"},
    };
    HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    for (String[] candidate : candidates) {
      String cid = candidate[0];
      String name = candidate[1];
      System.out.print("Testing " + name + " (" + cid + ") ... ");
      try {
        String url = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
        String body =
            "client_id="
                + URLEncoder.encode(cid, StandardCharsets.UTF_8)
                + "&scope="
                + URLEncoder.encode(
                    "XboxLive.signin XboxLive.offline_access", StandardCharsets.UTF_8);
        HttpRequest req =
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
          System.out.println("OK - DEVICE CODE WORKS!");
        } else if (resp.body().contains("user_code")) {
          System.out.println("OK - DEVICE CODE WORKS (200 but has user_code)!");
        } else if (resp.body().contains("AADSTS700016")) {
          System.out.println("APP NOT FOUND in consumers tenant");
        } else if (resp.body().contains("AADSTS50059")) {
          System.out.println("NO TENANT INFO");
        } else {
          String shortErr = resp.body().substring(0, Math.min(resp.body().length(), 100));
          System.out.println("HTTP " + resp.statusCode() + ": " + shortErr);
        }
      } catch (Exception e) {
        System.out.println("ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
      }
    }
  }
}
