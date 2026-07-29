package io.citadel.core.auth;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class LocalAuthServer {

  private final HttpServer server;
  private final int port;
  private final CompletableFuture<String> codeFuture;

  @SuppressWarnings("PMD.CognitiveComplexity")
  LocalAuthServer() throws IOException {
    this.server = HttpServer.create(new InetSocketAddress(0), 0);
    this.port = server.getAddress().getPort();
    this.codeFuture = new CompletableFuture<>();
    server.createContext(
        "/callback",
        exchange -> {
          try {
            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            String code = null;
            if (query != null) {
              for (String param : query.split("&")) {
                String[] parts = param.split("=", 2);
                if (parts.length == 2 && "code".equals(parts[0])) {
                  code = parts[1];
                  break;
                }
              }
            }
            String response;
            if (code != null) {
              response =
                  "<html><body><h1>Authenticated!</h1><p>You can close this window.</p></body></html>";
              codeFuture.complete(code);
            } else {
              response =
                  "<html><body><h1>Authentication failed</h1><p>No authorization code received.</p></body></html>";
              codeFuture.completeExceptionally(
                  new AuthenticationException("No authorization code in redirect"));
            }
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
          } catch (Exception e) {
            codeFuture.completeExceptionally(e);
          }
        });
    server.setExecutor(null);
  }

  void start() {
    server.start();
  }

  void stop() {
    server.stop(0);
  }

  int getPort() {
    return port;
  }

  String getRedirectUri() {
    return "http://localhost:" + port + "/callback";
  }

  @SuppressWarnings("PMD.PreserveStackTrace")
  String waitForCode(long timeout, TimeUnit unit)
      throws InterruptedException, TimeoutException, AuthenticationException {
    try {
      return codeFuture.get(timeout, unit);
    } catch (java.util.concurrent.ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof AuthenticationException authEx) {
        throw authEx;
      }
      throw new AuthenticationException("Auth callback failed: " + cause.getMessage(), cause);
    }
  }
}
