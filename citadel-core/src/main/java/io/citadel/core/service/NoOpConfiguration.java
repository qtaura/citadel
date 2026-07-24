package io.citadel.core.service;

import io.citadel.api.service.Configuration;
import io.citadel.api.service.ConfigurationListener;
import io.citadel.api.service.ConfigurationSection;
import java.util.List;
import java.util.Set;

/**
 * Placeholder implementation of {@link Configuration}. Replaced by the real implementation in
 * Milestone 5.
 *
 * <p>All methods throw {@link UnsupportedOperationException}.
 *
 * @deprecated replaced by {@code CitadelConfiguration} in Milestone 5
 */
@Deprecated
public final class NoOpConfiguration implements Configuration {

  @Override
  public String getString(String path) {
    throw unsupported();
  }

  @Override
  public String getString(String path, String defaultValue) {
    throw unsupported();
  }

  @Override
  public boolean getBoolean(String path) {
    throw unsupported();
  }

  @Override
  public boolean getBoolean(String path, boolean defaultValue) {
    throw unsupported();
  }

  @Override
  public int getInt(String path) {
    throw unsupported();
  }

  @Override
  public int getInt(String path, int defaultValue) {
    throw unsupported();
  }

  @Override
  public long getLong(String path) {
    throw unsupported();
  }

  @Override
  public long getLong(String path, long defaultValue) {
    throw unsupported();
  }

  @Override
  public double getDouble(String path) {
    throw unsupported();
  }

  @Override
  public double getDouble(String path, double defaultValue) {
    throw unsupported();
  }

  @Override
  public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass) {
    throw unsupported();
  }

  @Override
  public <T extends Enum<T>> T getEnum(String path, Class<T> enumClass, T defaultValue) {
    throw unsupported();
  }

  @Override
  public List<String> getStringList(String path) {
    throw unsupported();
  }

  @Override
  public <T> List<T> getList(String path) {
    throw unsupported();
  }

  @Override
  public ConfigurationSection getSection(String path) {
    throw unsupported();
  }

  @Override
  public boolean contains(String path) {
    throw unsupported();
  }

  @Override
  public Set<String> getKeys() {
    throw unsupported();
  }

  @Override
  public ConfigurationSection getRoot() {
    throw unsupported();
  }

  @Override
  public ConfigurationSection getPluginSection(String pluginName) {
    throw unsupported();
  }

  @Override
  public void reload() {
    throw unsupported();
  }

  @Override
  public void addListener(ConfigurationListener listener) {
    throw unsupported();
  }

  @Override
  public void removeListener(ConfigurationListener listener) {
    throw unsupported();
  }

  private static UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException(
        "NoOpConfiguration is obsolete; use CitadelConfiguration");
  }
}
