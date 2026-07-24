package io.citadel.core.plugin.testplugin;

import io.citadel.api.event.Event;
import io.citadel.api.event.EventBus;
import io.citadel.api.plugin.Plugin;
import io.citadel.api.plugin.PluginContext;
import io.citadel.api.plugin.PluginMetadata;

@PluginMetadata(name = "SubscribeOnEnablePlugin", version = "1.0.0", apiVersion = "0.1.0")
public class SubscribeOnEnablePlugin implements Plugin {

  private PluginContext context;

  @Override
  public void onLoad(PluginContext context) {
    this.context = context;
  }

  @Override
  public void onEnable() {
    context.getService(EventBus.class).ifPresent(bus -> bus.subscribe(Event.class, e -> {}));
  }

  @Override
  public void onDisable() {}
}
