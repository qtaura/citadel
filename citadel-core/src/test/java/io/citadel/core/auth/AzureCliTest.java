package io.citadel.core.auth;

public final class AzureCliTest {
  public static void main(String[] args) throws Exception {
    java.net.http.HttpClient client =
        java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    // Try Azure PowerShell client ID (known to work with device code flow)
    String clientId = "1950a258-227b-4e31-a9cf-717495945fc2";
    String url = "https://login.microsoftonline.com/organizations/oauth2/v2.0/devicecode";
    String body =
        "client_id="
            + java.net.URLEncoder.encode(clientId, java.nio.charset.StandardCharsets.UTF_8)
            + "&scope="
            + java.net.URLEncoder.encode(
                "https://graph.microsoft.com/.default", java.nio.charset.StandardCharsets.UTF_8);

    java.net.http.HttpRequest req =
        java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
            .build();
    java.net.http.HttpResponse<String> resp =
        client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
    System.out.println("HTTP " + resp.statusCode());
    System.out.println(resp.body().substring(0, Math.min(resp.body().length(), 500)));

    if (resp.body().contains("user_code")) {
      System.out.println("\n*** IT WORKS! ***");
      var json = new com.google.gson.Gson().fromJson(resp.body(), com.google.gson.JsonObject.class);
      System.out.println("user_code: " + json.get("user_code").getAsString());
      System.out.println("verification_uri: " + json.get("verification_uri").getAsString());
      System.out.println("\nOpen the URL, enter the code, and I can create a new Azure AD app.");
    }
  }
}
