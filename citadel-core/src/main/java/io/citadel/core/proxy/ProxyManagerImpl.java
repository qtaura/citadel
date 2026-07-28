package io.citadel.core.proxy;

import io.citadel.api.event.EventBus;
import io.citadel.api.event.proxy.ProxyRegisteredEvent;
import io.citadel.api.event.proxy.ProxyRemovedEvent;
import io.citadel.api.event.proxy.ProxyUpdatedEvent;
import io.citadel.api.proxy.ProxyDefinition;
import io.citadel.api.proxy.ProxyManager;
import io.citadel.api.proxy.ProxyType;
import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationException;
import io.citadel.api.service.ConfigurationSection;
import io.citadel.api.service.Logger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ProxyManagerImpl implements ProxyManager {

  private static final Set<String> KNOWN_PROXY_FIELDS = Set.of("type", "host", "port");

  private final Map<String, ProxyDefinition> proxies;
  private final EventBus eventBus;
  private final Logger logger;

  public ProxyManagerImpl(Configuration config, EventBus eventBus, Logger logger) {
    this.proxies = new ConcurrentHashMap<>();
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    this.logger = Objects.requireNonNull(logger, "logger");
    Objects.requireNonNull(config, "config");
    loadProxies(config);
    config.addListener(this::onReload);
  }

  @Override
  public ProxyDefinition get(String id) {
    Objects.requireNonNull(id, "id");
    return proxies.get(id);
  }

  @Override
  public List<ProxyDefinition> getAll() {
    return List.copyOf(proxies.values());
  }

  @Override
  public List<ProxyDefinition> findByType(ProxyType type) {
    Objects.requireNonNull(type, "type");
    return proxies.values().stream().filter(p -> p.type() == type).toList();
  }

  @Override
  public boolean register(ProxyDefinition proxy) {
    Objects.requireNonNull(proxy, "proxy");
    ProxyDefinition existing = proxies.putIfAbsent(proxy.id(), proxy);
    if (existing != null) {
      return false;
    }
    logger.info("Registered proxy {}", proxy.id());
    eventBus.publishAsync(new ProxyRegisteredEvent(proxy));
    return true;
  }

  @Override
  public boolean unregister(String id) {
    Objects.requireNonNull(id, "id");
    ProxyDefinition removed = proxies.remove(id);
    if (removed == null) {
      return false;
    }
    logger.info("Unregistered proxy {}", id);
    eventBus.publishAsync(new ProxyRemovedEvent(removed));
    return true;
  }

  @Override
  public boolean contains(String id) {
    Objects.requireNonNull(id, "id");
    return proxies.containsKey(id);
  }

  @Override
  public int size() {
    return proxies.size();
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private void onReload(Configuration config) {
    Map<String, ProxyDefinition> parsed = parseAll(config);
    for (Map.Entry<String, ProxyDefinition> entry : parsed.entrySet()) {
      ProxyDefinition existing = proxies.put(entry.getKey(), entry.getValue());
      if (existing != null && !existing.equals(entry.getValue())) {
        logger.info("Proxy {} updated during reload", entry.getKey());
        eventBus.publishAsync(new ProxyUpdatedEvent(existing, entry.getValue()));
      }
    }
    proxies.keySet().retainAll(parsed.keySet());
  }

  private void loadProxies(Configuration config) {
    for (Map.Entry<String, ProxyDefinition> entry : parseAll(config).entrySet()) {
      proxies.put(entry.getKey(), entry.getValue());
    }
  }

  private Map<String, ProxyDefinition> parseAll(Configuration config) {
    ConfigurationSection section = config.getSection("proxies");
    validateAll(section);
    Map<String, ProxyDefinition> result = new LinkedHashMap<>();
    for (String id : section.getKeys()) {
      ConfigurationSection def = section.getSection(id);
      ProxyDefinition proxy = parseProxy(id, def);
      result.put(proxy.id(), proxy);
    }
    return result;
  }

  private static void validateAll(ConfigurationSection section) {
    for (String id : section.getKeys()) {
      ConfigurationSection def = section.getSection(id);
      validateFields(def);
      validateProxyConfig(id, def);
    }
  }

  private static void validateProxyConfig(String id, ConfigurationSection def) {
    ProxyType type = def.getEnum("type", ProxyType.class);
    if (type == null) {
      throw new ConfigurationException(
          id + ".type", "Proxy type is required. Valid values: DIRECT, SOCKS5, HTTP");
    }
    if (type != ProxyType.DIRECT) {
      validateHost(id, type, def);
      validatePort(id, type, def);
    }
  }

  private static void validateHost(String id, ProxyType type, ConfigurationSection def) {
    String host = def.getString("host", null);
    if (host == null || host.isBlank()) {
      throw new ConfigurationException(id + ".host", "host is required for proxy type " + type);
    }
  }

  private static void validatePort(String id, ProxyType type, ConfigurationSection def) {
    int port = def.getInt("port", 0);
    if (port < 1 || port > 65535) {
      throw new ConfigurationException(
          id + ".port", "port must be between 1 and 65535 for proxy type " + type);
    }
  }

  private static ProxyDefinition parseProxy(String id, ConfigurationSection section) {
    ProxyType type = section.getEnum("type", ProxyType.class);
    if (type == ProxyType.DIRECT) {
      return new ProxyDefinition(id, ProxyType.DIRECT, null, 0);
    }
    String host = section.getString("host");
    int port = section.getInt("port");
    return new ProxyDefinition(id, type, host, port);
  }

  private static void validateFields(ConfigurationSection section) {
    for (String key : section.getKeys()) {
      if (!KNOWN_PROXY_FIELDS.contains(key)) {
        throw new ConfigurationException(
            key,
            "Unknown configuration field '"
                + key
                + "'. Valid fields: "
                + String.join(", ", KNOWN_PROXY_FIELDS));
      }
    }
  }
}
