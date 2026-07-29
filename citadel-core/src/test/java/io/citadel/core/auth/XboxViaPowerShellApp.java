package io.citadel.core.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class XboxViaPowerShellApp {
  static final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  static final Gson gson = new Gson();
  static final String PS_CLIENT_ID = "1950a258-227b-4e31-a9cf-717495945fc2";

  public static void main(String[] args) throws Exception {
    // Step 1: Get device code
    String body =
        "client_id="
            + PS_CLIENT_ID
            + "&scope="
            + URLEncoder.encode("XboxLive.signin XboxLive.offline_access", "UTF-8");
    String resp = post("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode", body);
    JsonObject deviceResp = gson.fromJson(resp, JsonObject.class);
    if (deviceResp.has("error")) {
      System.out.println("Device code error: " + resp);
      System.out.println("\nTrying organizations tenant...");
      resp = post("https://login.microsoftonline.com/organizations/oauth2/v2.0/devicecode", body);
      deviceResp = gson.fromJson(resp, JsonObject.class);
    }
    String userCode = deviceResp.get("user_code").getAsString();
    String deviceCode = deviceResp.get("device_code").getAsString();
    int expiresIn = deviceResp.get("expires_in").getAsInt();
    System.out.println("\nSTEP 1: Open https://login.microsoft.com/device");
    System.out.println("STEP 2: Enter code: " + userCode);
    System.out.println("STEP 3: Sign in. This gives the bot Xbox access.");
    System.out.println();

    // Step 2: Poll for token
    String pollBody =
        "client_id="
            + PS_CLIENT_ID
            + "&grant_type=urn:ietf:params:oauth:grant-type:device_code"
            + "&device_code="
            + URLEncoder.encode(deviceCode, "UTF-8");
    String tokenUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

    String accessToken = null;
    String refreshToken = null;
    long deadline = System.currentTimeMillis() + expiresIn * 1000L;
    poll:
    while (System.currentTimeMillis() < deadline) {
      Thread.sleep(3000);
      String pollResp = post(tokenUrl, pollBody);
      JsonObject obj = gson.fromJson(pollResp, JsonObject.class);
      if (obj.has("access_token")) {
        accessToken = obj.get("access_token").getAsString();
        refreshToken = obj.has("refresh_token") ? obj.get("refresh_token").getAsString() : null;
        break;
      }
      String err = obj.has("error") ? obj.get("error").getAsString() : "";
      if ("authorization_pending".equals(err) || "slow_down".equals(err)) continue;
      System.out.println("Token error: " + pollResp);
      // Try organizations
      if (pollResp.contains("AADSTS50059")) {
        tokenUrl = "https://login.microsoftonline.com/organizations/oauth2/v2.0/token";
        continue poll;
      }
      return;
    }
    if (accessToken == null) {
      System.out.println("Timed out.");
      return;
    }
    System.out.println("Got MS token! Now doing XBL -> XSTS -> Minecraft...");

    // Step 3: XBL
    String xblBody =
        "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d="
            + accessToken
            + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}";
    String xblResp = postJson("https://user.auth.xboxlive.com/user/authenticate", xblBody);
    JsonObject xblObj = gson.fromJson(xblResp, JsonObject.class);
    String xblToken = xblObj.get("Token").getAsString();
    String uhs =
        xblObj
            .getAsJsonObject("DisplayClaims")
            .getAsJsonArray("xui")
            .get(0)
            .getAsJsonObject()
            .get("uhs")
            .getAsString();
    System.out.println("XBL OK");

    // Step 4: XSTS
    String xstsBody =
        "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\""
            + xblToken
            + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";
    String xstsResp = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody);
    JsonObject xstsObj = gson.fromJson(xstsResp, JsonObject.class);
    String xstsToken = xstsObj.get("Token").getAsString();
    System.out.println("XSTS OK");

    // Step 5: Minecraft login
    String identityToken = "XBL3.0 x=" + uhs + ";" + xstsToken;
    String mcBody = "{\"identityToken\":\"" + identityToken + "\"}";
    String mcResp =
        postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcBody);
    JsonObject mcObj = gson.fromJson(mcResp, JsonObject.class);
    String mcAccessToken = mcObj.get("access_token").getAsString();
    System.out.println("MC Login OK");

    // Step 6: Profile
    HttpRequest profileReq =
        HttpRequest.newBuilder()
            .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
            .header("Authorization", "Bearer " + mcAccessToken)
            .GET()
            .build();
    HttpResponse<String> profileResp =
        client.send(profileReq, HttpResponse.BodyHandlers.ofString());
    JsonObject profileObj = gson.fromJson(profileResp.body(), JsonObject.class);
    String uuid = profileObj.get("id").getAsString();
    String username = profileObj.get("name").getAsString();
    System.out.println("\n========== AUTHENTICATED! ==========");
    System.out.println("UUID: " + uuid);
    System.out.println("Username: " + username);
    System.out.println("MC Access Token: " + mcAccessToken.substring(0, 30) + "...");
    System.out.println("\nNow the bot can connect to 6b6t!");
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

  static String postJson(String url, String body) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
  }
}
