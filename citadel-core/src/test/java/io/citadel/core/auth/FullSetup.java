package io.citadel.core.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class FullSetup {
  static final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  static final Gson gson = new Gson();
  static final String POWERSHELL_CLIENT_ID = "1950a258-227b-4e31-a9cf-717495945fc2";

  public static void main(String[] args) throws Exception {
    // Step 1: Get device code for Graph API
    System.out.println("Generating device code...");
    String scope = URLEncoder.encode("https://graph.microsoft.com/.default", "UTF-8");
    String body = "client_id=" + POWERSHELL_CLIENT_ID + "&scope=" + scope;
    String resp =
        post("https://login.microsoftonline.com/organizations/oauth2/v2.0/devicecode", body);
    JsonObject deviceResp = gson.fromJson(resp, JsonObject.class);
    String userCode = deviceResp.get("user_code").getAsString();
    String deviceCode = deviceResp.get("device_code").getAsString();
    int expiresIn = deviceResp.get("expires_in").getAsInt();

    System.out.println("\n========================================");
    System.out.println("1. Open: https://login.microsoft.com/device");
    System.out.println("2. Enter code: " + userCode);
    System.out.println("3. Sign in with your Microsoft account");
    System.out.println("This is needed to create a custom Azure AD app.");
    System.out.println("========================================\n");

    // Step 2: Poll for token
    String pollBody =
        "client_id="
            + POWERSHELL_CLIENT_ID
            + "&grant_type=urn:ietf:params:oauth:grant-type:device_code"
            + "&device_code="
            + URLEncoder.encode(deviceCode, "UTF-8");
    String tokenUrl = "https://login.microsoftonline.com/organizations/oauth2/v2.0/token";

    String graphToken = null;
    long deadline = System.currentTimeMillis() + expiresIn * 1000L;
    while (System.currentTimeMillis() < deadline) {
      Thread.sleep(3000);
      String pollResp = post(tokenUrl, pollBody);
      JsonObject obj = gson.fromJson(pollResp, JsonObject.class);
      if (obj.has("access_token")) {
        graphToken = obj.get("access_token").getAsString();
        System.out.println("Authenticated! Creating Azure AD app...");
        break;
      }
      String err = obj.has("error") ? obj.get("error").getAsString() : "";
      if (!"authorization_pending".equals(err) && !"slow_down".equals(err)) {
        System.out.println("Error: " + pollResp);
        return;
      }
    }
    if (graphToken == null) {
      System.out.println("Timed out.");
      return;
    }

    // Step 3: Create the Azure AD app
    String appJson =
        "{\"displayName\":\"Citadel Bot\",\"signInAudience\":\"AzureADMyOrg\",\"publicClient\":{\"redirectUris\":[\"http://localhost\"]}}";
    HttpRequest createReq =
        HttpRequest.newBuilder()
            .uri(URI.create("https://graph.microsoft.com/v1.0/applications"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + graphToken)
            .POST(HttpRequest.BodyPublishers.ofString(appJson))
            .build();
    HttpResponse<String> createResp = client.send(createReq, HttpResponse.BodyHandlers.ofString());
    System.out.println("Create app: HTTP " + createResp.statusCode());

    if (createResp.statusCode() == 201) {
      JsonObject created = gson.fromJson(createResp.body(), JsonObject.class);
      String newClientId = created.get("appId").getAsString();
      String appObjId = created.get("id").getAsString();
      System.out.println("NEW CLIENT ID: " + newClientId);

      // Enable public client
      Thread.sleep(3000);
      String patchJson =
          "{\"isFallbackPublicClient\":true,\"spa\":{\"redirectUris\":[\"http://localhost\"]}}";
      HttpRequest patchReq =
          HttpRequest.newBuilder()
              .uri(URI.create("https://graph.microsoft.com/v1.0/applications/" + appObjId))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + graphToken)
              .method("PATCH", HttpRequest.BodyPublishers.ofString(patchJson))
              .build();
      HttpResponse<String> patchResp = client.send(patchReq, HttpResponse.BodyHandlers.ofString());
      System.out.println("Patch app: HTTP " + patchResp.statusCode());

      System.out.println("\n!! COPY THIS INTO citadel.yml !!");
      System.out.println("authentication:");
      System.out.println("  azure_client_id: \"" + newClientId + "\"");
      System.out.println("  azure_tenant: \"consumers\"");
      System.out.println("  offline_mode: false");
    } else {
      System.out.println("FAILED: " + createResp.body());
      // Maybe the token scope doesn't allow app creation
      System.out.println("\nTrying to check token scope...");
      HttpRequest checkReq =
          HttpRequest.newBuilder()
              .uri(URI.create("https://graph.microsoft.com/v1.0/me"))
              .header("Authorization", "Bearer " + graphToken)
              .GET()
              .build();
      HttpResponse<String> checkResp = client.send(checkReq, HttpResponse.BodyHandlers.ofString());
      System.out.println(
          "Me endpoint: HTTP "
              + checkResp.statusCode()
              + " "
              + checkResp.body().substring(0, Math.min(checkResp.body().length(), 200)));
    }
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
