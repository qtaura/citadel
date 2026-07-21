package io.citadel.core.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import io.citadel.api.service.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServiceRegistryTest {

  private ServiceRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new ServiceRegistry();
  }

  @Test
  void returnsRegisteredService() {
    TestService service = new ServiceImpl();
    registry.register(TestService.class, service);
    assertSame(service, registry.get(TestService.class).orElseThrow());
  }

  @Test
  void returnsEmptyForUnregisteredService() {
    assertTrue(registry.get(TestService.class).isEmpty());
  }

  @Test
  void overwritesExistingService() {
    TestService first = new ServiceImpl();
    TestService second = new ServiceImpl();
    registry.register(TestService.class, first);
    registry.register(TestService.class, second);
    assertSame(second, registry.get(TestService.class).orElseThrow());
  }

  @Test
  void throwsOnNullType() {
    assertThrows(NullPointerException.class, () -> registry.register(null, new ServiceImpl()));
  }

  @Test
  void throwsOnNullService() {
    assertThrows(NullPointerException.class, () -> registry.register(TestService.class, null));
  }

  private interface TestService extends Service {}

  private static final class ServiceImpl implements TestService {}
}
