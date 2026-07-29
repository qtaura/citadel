package io.citadel.core.auth;

public final class LiveComTest {
  public static void main(String[] args) throws Exception {
    String clientId = "00000000402b5328";
    String scope = java.net.URLEncoder.encode("XboxLive.signin", "UTF-8");
    String redirectUri =
        java.net.URLEncoder.encode("https://login.live.com/oauth20_desktop.srf", "UTF-8");
    String authorizeUrl =
        "https://login.live.com/oauth20_authorize.srf"
            + "?client_id="
            + clientId
            + "&response_type=code"
            + "&redirect_uri="
            + redirectUri
            + "&scope="
            + scope;

    System.out.println("Open this URL in your browser:");
    System.out.println(authorizeUrl);
    System.out.println("\nSign in with your Microsoft account.");
    System.out.println("After signing in, you'll be redirected to a URL.");
    System.out.println("Copy the ENTIRE redirect URL from your browser's address bar\n");
    System.out.println("Then run the next step with the code from the URL.\n");
    System.out.println("The URL will look like:");
    System.out.println("https://login.live.com/oauth20_desktop.srf?code=M.xxx...");
  }
}
