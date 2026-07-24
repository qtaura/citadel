package io.citadel.core.plugin.testplugin;

import io.citadel.api.plugin.Plugin;
import io.citadel.api.plugin.PluginContext;

public class NoAnnotationPlugin implements Plugin {

  @Override
  public void onLoad(PluginContext context) {}

  @Override
  public void onEnable() {}

  @Override
  public void onDisable() {}
}
