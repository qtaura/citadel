package io.citadel.core.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class CreateAzureApp {
  static final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  static final Gson gson = new Gson();
  static final String DEVICE_CODE =
      "FBgABIQEAAAAdDD7nC9b5Q7JPd_okEQRFRXZvU3RzQXJ0aWZhY3RzAQAAAAAAsYaPdTr4CPzxbPX6xkY3pLgRExBUNmq8pswB_TlBubD-jWfEGTm1XHS85hjHU-2hIGlBk6-XW0K57gv_5TwIW2BOm9r1OmDksQniAFuZHnfJA31LSvMFcxbev9rtNwTvDHnOUKYJSPFw7MpgNsZIUahPZXL9qQrR6uSrcYhZ6H2nzIAC7YBye6rGcq09ab_KjmvM3Vbrp89DK7CnwrSIVfDmEjcPv403QAH20e5I6KR_3sHNsvSP0b0muz7S8b0OIMZMpuZ14gE_4MAVEUXY4gI1T2IuI5AS-hY1ROxWCdy1KNNm4LPSqUvEpJO2waimknyo7lYuhu_1Kq3doHfONSPiwa4g_LC4I2Riyc8avWXKk0EOAyrkXCrSSBNtMG6TAGCwXA1CUKiUBOb";

  public static void main(String[] args) throws Exception {
    String clientId = "1950a258-227b-4e31-a9cf-717495945fc2";
    String tenant = "organizations";

    // Step 1: Poll for token
    System.out.println("Polling for token...");
    String tokenBody =
        "client_id="
            + urlEncode(clientId)
            + "&grant_type="
            + urlEncode("urn:ietf:params:oauth:grant-type:device_code")
            + "&device_code="
            + urlEncode(DEVICE_CODE);
    String tokenUrl = "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token";
    String graphToken = null;
    for (int i = 0; i < 60; i++) {
      Thread.sleep(2000);
      String resp = post(tokenUrl, tokenBody);
      JsonObject obj = gson.fromJson(resp, JsonObject.class);
      if (obj.has("access_token")) {
        graphToken = obj.get("access_token").getAsString();
        System.out.println("Got Graph API token!");
        break;
      }
      if (obj.has("error")
          && !"authorization_pending".equals(obj.get("error").getAsString())
          && !"slow_down".equals(obj.get("error").getAsString())) {
        System.out.println("Token error: " + resp);
        return;
      }
    }
    if (graphToken == null) {
      System.out.println(
          "Timed out waiting for authentication. Open https://login.microsoft.com/device and enter code F5D6E92D5");
      return;
    }

    // Step 2: Create Azure AD app
    System.out.println("Creating Azure AD app...");
    JsonObject publicClient = new JsonObject();
    publicClient.addProperty("redirectUris", "http://localhost");
    JsonObject app = new JsonObject();
    app.addProperty("displayName", "Citadel Bot Platform");
    app.addProperty("signInAudience", "AzureADMyOrg");
    app.add("publicClient", publicClient);
    String appJson = gson.toJson(app);

    HttpRequest createReq =
        HttpRequest.newBuilder()
            .uri(URI.create("https://graph.microsoft.com/v1.0/applications"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + graphToken)
            .POST(HttpRequest.BodyPublishers.ofString(appJson))
            .build();
    HttpResponse<String> createResp = client.send(createReq, HttpResponse.BodyHandlers.ofString());
    System.out.println("Create app HTTP " + createResp.statusCode());
    if (createResp.statusCode() == 201) {
      JsonObject created = gson.fromJson(createResp.body(), JsonObject.class);
      String newClientId = created.get("appId").getAsString();
      String appObjId = created.get("id").getAsString();
      System.out.println("NEW CLIENT ID: " + newClientId);
      System.out.println("App object ID: " + appObjId);

      // Step 3: Enable public client flows
      Thread.sleep(3000);
      JsonObject patch = new JsonObject();
      patch.addProperty("isFallbackPublicClient", true);
      JsonObject spa = new JsonObject();
      spa.addProperty("redirectUris", "http://localhost");
      patch.add("spa", spa);

      HttpRequest patchReq =
          HttpRequest.newBuilder()
              .uri(URI.create("https://graph.microsoft.com/v1.0/applications/" + appObjId))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + graphToken)
              .method("PATCH", HttpRequest.BodyPublishers.ofString(gson.toJson(patch)))
              .build();
      HttpResponse<String> patchResp = client.send(patchReq, HttpResponse.BodyHandlers.ofString());
      System.out.println("Patch app HTTP " + patchResp.statusCode());
      System.out.println("Response: " + patchResp.body());

      System.out.println("\n=== COPY THIS INTO citadel.yml ===");
      System.out.println("authentication:");
      System.out.println("  azure_client_id: \"" + newClientId + "\"");
      System.out.println("  azure_tenant: \"consumers\"");
    } else {
      System.out.println("Failed to create app: " + createResp.body());
    }
  }

  static String urlEncode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  static String post(String url, String body) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
  }
}
