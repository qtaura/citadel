package io.citadel.core.plugin.testplugin;

import io.citadel.api.plugin.Plugin;
import io.citadel.api.plugin.PluginContext;
import io.citadel.api.plugin.PluginMetadata;

@PluginMetadata(name = "TestPlugin", version = "1.0.0", apiVersion = "0.1.0")
public class TestPlugin implements Plugin {

  @Override
  public void onLoad(PluginContext context) {}

  @Override
  public void onEnable() {}

  @Override
  public void onDisable() {}
}
