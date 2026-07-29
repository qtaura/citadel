package io.citadel.core.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

public final class MinecraftFromWinCred {
  static final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  static final Gson gson = new Gson();

  public static void main(String[] args) throws Exception {
    // Azure AD token from Windows Credential Manager
    String azureToken = args.length > 0 ? args[0] : null;
    if (azureToken == null || azureToken.isEmpty()) {
      System.out.println("USAGE: pass the Azure AD JWT token as argument");
      System.out.println("Extract it from Windows Credential Manager (MCLMS|...|AzureToken)");
      return;
    }
    System.out.println("Azure token length: " + azureToken.length());

    // Step 1: XBL
    System.out.print("XBL... ");
    String xblBody =
        "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d="
            + azureToken
            + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}";
    HttpRequest xblReq =
        HttpRequest.newBuilder()
            .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(xblBody))
            .build();
    HttpResponse<String> xblResp = client.send(xblReq, HttpResponse.BodyHandlers.ofString());
    if (xblResp.statusCode() != 200) {
      System.out.println(
          "FAILED HTTP "
              + xblResp.statusCode()
              + ": "
              + xblResp.body().substring(0, Math.min(xblResp.body().length(), 200)));
      return;
    }
    JsonObject xblObj = gson.fromJson(xblResp.body(), JsonObject.class);
    String xblToken = xblObj.get("Token").getAsString();
    String uhs =
        xblObj
            .getAsJsonObject("DisplayClaims")
            .getAsJsonArray("xui")
            .get(0)
            .getAsJsonObject()
            .get("uhs")
            .getAsString();
    System.out.println("OK");

    // Step 2: XSTS
    System.out.print("XSTS... ");
    String xstsBody =
        "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\""
            + xblToken
            + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";
    HttpRequest xstsReq =
        HttpRequest.newBuilder()
            .uri(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(xstsBody))
            .build();
    HttpResponse<String> xstsResp = client.send(xstsReq, HttpResponse.BodyHandlers.ofString());
    if (xstsResp.statusCode() != 200) {
      System.out.println(
          "FAILED HTTP "
              + xstsResp.statusCode()
              + ": "
              + xstsResp.body().substring(0, Math.min(xstsResp.body().length(), 200)));
      return;
    }
    JsonObject xstsObj = gson.fromJson(xstsResp.body(), JsonObject.class);
    String xstsToken = xstsObj.get("Token").getAsString();
    System.out.println("OK");

    // Step 3: Minecraft login
    System.out.print("Minecraft login... ");
    String identityToken = "XBL3.0 x=" + uhs + ";" + xstsToken;
    String mcBody = "{\"identityToken\":\"" + identityToken + "\"}";
    HttpRequest mcReq =
        HttpRequest.newBuilder()
            .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mcBody))
            .build();
    HttpResponse<String> mcResp = client.send(mcReq, HttpResponse.BodyHandlers.ofString());
    if (mcResp.statusCode() != 200) {
      System.out.println(
          "FAILED HTTP "
              + mcResp.statusCode()
              + ": "
              + mcResp.body().substring(0, Math.min(mcResp.body().length(), 200)));
      return;
    }
    JsonObject mcObj = gson.fromJson(mcResp.body(), JsonObject.class);
    String mcAccessToken = mcObj.get("access_token").getAsString();
    int expiresIn = mcObj.get("expires_in").getAsInt();
    System.out.println("OK (expires in " + expiresIn + "s)");

    // Step 4: Profile
    System.out.print("Profile... ");
    HttpRequest profileReq =
        HttpRequest.newBuilder()
            .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
            .header("Authorization", "Bearer " + mcAccessToken)
            .GET()
            .build();
    HttpResponse<String> profileResp =
        client.send(profileReq, HttpResponse.BodyHandlers.ofString());
    if (profileResp.statusCode() != 200) {
      System.out.println("FAILED HTTP " + profileResp.statusCode());
      return;
    }
    JsonObject profileObj = gson.fromJson(profileResp.body(), JsonObject.class);
    String uuid = profileObj.get("id").getAsString();
    String username = profileObj.get("name").getAsString();
    System.out.println("OK");

    System.out.println("\n========== AUTHENTICATED ==========");
    System.out.println("Username: " + username);
    System.out.println("UUID: " + uuid);
    System.out.println("MC Token: " + mcAccessToken.substring(0, 30) + "...");
    System.out.println("Expires: " + Instant.now().plusSeconds(expiresIn));

    // Output machine-readable format
    System.out.println("\n--- SESSION DATA (save this) ---");
    System.out.println("PROFILE_ID=" + uuid);
    System.out.println("USERNAME=" + username);
    System.out.println("ACCESS_TOKEN=" + mcAccessToken);
    System.out.println("EXPIRES_AT=" + Instant.now().plusSeconds(expiresIn));
  }
}
