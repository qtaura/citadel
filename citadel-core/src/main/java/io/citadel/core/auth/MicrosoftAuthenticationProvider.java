package io.citadel.core.auth;

import io.citadel.api.account.Account;
import io.citadel.api.account.AccountType;
import io.citadel.api.auth.AuthenticationProvider;
import io.citadel.api.auth.Session;
import io.citadel.api.service.Logger;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

public final class MicrosoftAuthenticationProvider implements AuthenticationProvider {

  private final Logger logger;
  private final net.lenni0451.commons.httpclient.HttpClient httpClient;

  public MicrosoftAuthenticationProvider(Logger logger) {
    this.logger = Objects.requireNonNull(logger, "logger");
    this.httpClient = MinecraftAuth.createHttpClient("citadel/0.1.0");
  }

  @Override
  public AccountType accountType() {
    return AccountType.MICROSOFT;
  }

  @Override
  public Session authenticate(Account account) {
    logger.info("Starting Microsoft authentication for account {}", account.id());
    try {
      System.out.println("[AUTH] Requesting device code...");
      System.out.flush();
      JavaAuthManager authManager =
          JavaAuthManager.create(httpClient)
              .login(
                  DeviceCodeMsaAuthService::new,
                  (Consumer<MsaDeviceCode>)
                      deviceCode -> {
                        String url = deviceCode.getDirectVerificationUri();
                        System.out.println("\n========================================");
                        System.out.println("Open this URL in your browser and sign in:");
                        System.out.println(url);
                        System.out.println("========================================\n");
                        System.out.flush();
                      });
      System.out.println("[AUTH] Device code flow complete, getting tokens...");
      System.out.flush();
      var mcToken = authManager.getMinecraftToken().getUpToDate();
      System.out.println("[AUTH] Got Minecraft token");
      System.out.flush();
      var profile = authManager.getMinecraftProfile().getUpToDate();
      System.out.println("[AUTH] Got profile: " + profile.getName());
      System.out.flush();
      logger.info(
          "Microsoft authentication succeeded for account {} as {}",
          account.id(),
          profile.getName());
      return new Session(
          account.id(),
          profile.getId(),
          profile.getName(),
          AccountType.MICROSOFT,
          Optional.of(mcToken.getToken()),
          Optional.of(Instant.ofEpochMilli(mcToken.getExpireTimeMs())));
    } catch (Exception e) {
      System.out.println("[ERROR] Microsoft authentication failed: " + e.getMessage());
      e.printStackTrace(System.out);
      System.out.flush();
      throw new RuntimeException(
          "Microsoft authentication failed for account " + account.id() + ": " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<Session> refresh(Session stale) {
    try {
      JavaAuthManager authManager =
          JavaAuthManager.fromJson(httpClient, new com.google.gson.JsonObject());
      var mcToken = authManager.getMinecraftToken().getUpToDate();
      var profile = authManager.getMinecraftProfile().getUpToDate();
      logger.info("Session refreshed for account {}", stale.accountId());
      return Optional.of(
          new Session(
              stale.accountId(),
              profile.getId(),
              profile.getName(),
              AccountType.MICROSOFT,
              Optional.of(mcToken.getToken()),
              Optional.of(Instant.ofEpochMilli(mcToken.getExpireTimeMs()))));
    } catch (Exception e) {
      logger.warn(
          "Failed to refresh session for account {}: {}", stale.accountId(), e.getMessage());
      return Optional.empty();
    }
  }
}
