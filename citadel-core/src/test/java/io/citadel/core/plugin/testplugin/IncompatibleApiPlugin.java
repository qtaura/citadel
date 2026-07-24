package io.citadel.core.plugin.testplugin;

import io.citadel.api.plugin.Plugin;
import io.citadel.api.plugin.PluginContext;
import io.citadel.api.plugin.PluginMetadata;

@PluginMetadata(name = "IncompatibleApiPlugin", version = "1.0.0", apiVersion = "1.0.0")
public class IncompatibleApiPlugin implements Plugin {

  @Override
  public void onLoad(PluginContext context) {}

  @Override
  public void onEnable() {}

  @Override
  public void onDisable() {}
}
