package io.citadel.core.bootstrap;

import io.citadel.api.service.Service;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for core services.
 *
 * <p>Subsystems register their service implementations at startup. Plugins access them through
 * {@link io.citadel.api.plugin.PluginContext#getService(Class)}, which delegates to this registry.
 *
 * <p>Each service type may have at most one registered implementation. Registering a second
 * implementation for the same type overwrites the previous one.
 */
public final class ServiceRegistry {

  private final Map<Class<? extends Service>, Service> services = new ConcurrentHashMap<>();

  /**
   * Registers a service implementation for its interface type.
   *
   * @param <T> the service type
   * @param type the service interface class
   * @param service the implementation instance
   * @throws NullPointerException if type or service is null
   */
  public <T extends Service> void register(Class<T> type, T service) {
    services.put(type, service);
  }

  /**
   * Retrieves a registered service by its interface type.
   *
   * @param <T> the service type
   * @param type the service interface class
   * @return an {@link Optional} containing the registered implementation, or empty if not found
   */
  @SuppressWarnings("unchecked")
  public <T extends Service> Optional<T> get(Class<T> type) {
    return Optional.ofNullable((T) services.get(type));
  }
}
