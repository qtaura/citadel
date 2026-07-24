package io.citadel.core.plugin.testplugin;

import io.citadel.api.plugin.Plugin;
import io.citadel.api.plugin.PluginContext;
import io.citadel.api.plugin.PluginMetadata;

@PluginMetadata(name = "FailingOnEnablePlugin", version = "1.0.0", apiVersion = "0.1.0")
public class FailingOnEnablePlugin implements Plugin {

  @Override
  public void onLoad(PluginContext context) {}

  @Override
  public void onEnable() {
    throw new RuntimeException("onEnable failed");
  }

  @Override
  public void onDisable() {}
}
