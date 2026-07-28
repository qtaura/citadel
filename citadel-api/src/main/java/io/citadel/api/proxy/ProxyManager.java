package io.citadel.api.proxy;

import io.citadel.api.service.Service;
import java.util.List;

public interface ProxyManager extends Service {

  ProxyDefinition get(String id);

  List<ProxyDefinition> getAll();

  List<ProxyDefinition> findByType(ProxyType type);

  boolean register(ProxyDefinition proxy);

  boolean unregister(String id);

  boolean contains(String id);

  int size();
}
