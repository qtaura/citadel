package io.citadel.core.plugin;

import io.citadel.api.plugin.Plugin;
import io.citadel.api.plugin.PluginMetadata;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scans a directory for plugin JARs and extracts their metadata.
 *
 * <p>Discovery follows these steps for each JAR:
 *
 * <ol>
 *   <li>Open the JAR and iterate its entries
 *   <li>Find exactly one class annotated with {@link PluginMetadata @PluginMetadata}
 *   <li>Verify the annotated class implements {@link Plugin}
 *   <li>Check API version compatibility
 *   <li>Build a {@link PluginDescriptor}
 * </ol>
 */
public final class PluginScanner {

  private static final String JAR_GLOB = "*.jar";

  private final Path pluginsDirectory;

  public PluginScanner(Path pluginsDirectory) {
    this.pluginsDirectory = pluginsDirectory;
  }

  /**
   * Scans the configured directory and returns descriptors for all valid plugins.
   *
   * @param apiVersion the core API version to check against
   * @return list of discovered plugin descriptors (empty if no plugins found)
   */
  public List<PluginDescriptor> scan(String apiVersion) {
    List<Path> jars = findJars();
    List<PluginDescriptor> descriptors = new ArrayList<>();

    for (Path jar : jars) {
      try {
        PluginDescriptor descriptor = processJar(jar, apiVersion);
        if (descriptor != null) {
          descriptors.add(descriptor);
        }
      } catch (Exception e) {
        System.err.println(
            "[Citadel] Failed to process plugin JAR " + jar.getFileName() + ": " + e.getMessage());
      }
    }
    return descriptors;
  }

  private List<Path> findJars() {
    List<Path> jars = new ArrayList<>();
    if (!Files.isDirectory(pluginsDirectory)) {
      return jars;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDirectory, JAR_GLOB)) {
      for (Path entry : stream) {
        jars.add(entry);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to list plugins directory: " + pluginsDirectory, e);
    }
    return jars;
  }

  private PluginDescriptor processJar(Path jarPath, String coreApiVersion) throws IOException {
    ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
    try (PluginClassLoader tempLoader = new PluginClassLoader(jarPath, parentLoader);
        JarFile jar = new JarFile(jarPath.toFile())) {
      Class<?> annotatedClass = findAnnotatedClass(jar, tempLoader);

      if (annotatedClass == null) {
        System.err.println("[Citadel] No @PluginMetadata class found in " + jarPath.getFileName());
        return null;
      }

      if (!Plugin.class.isAssignableFrom(annotatedClass)) {
        System.err.println(
            "[Citadel] Class "
                + annotatedClass.getName()
                + " in "
                + jarPath.getFileName()
                + " has @PluginMetadata but does not implement Plugin");
        return null;
      }

      PluginMetadata meta = annotatedClass.getAnnotation(PluginMetadata.class);

      if (!isApiCompatible(meta.apiVersion(), coreApiVersion)) {
        System.err.println(
            "[Citadel] Plugin "
                + meta.name()
                + " requires API v"
                + meta.apiVersion()
                + " but core has v"
                + coreApiVersion
                + "; skipping");
        return null;
      }

      @SuppressWarnings("unchecked")
      Class<? extends Plugin> mainClass = (Class<? extends Plugin>) annotatedClass;

      return new PluginDescriptor(
          meta.name(), meta.version(), meta.apiVersion(), mainClass.getName(), jarPath, meta);
    }
  }

  /**
   * Iterates JAR entries looking for a class annotated with @PluginMetadata.
   *
   * @return the annotated class, or null if none found
   */
  private Class<?> findAnnotatedClass(JarFile jar, ClassLoader classLoader) {
    Class<?> result = null;
    java.util.Enumeration<JarEntry> entries = jar.entries();

    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      if (!entry.getName().endsWith(".class")) {
        continue;
      }
      String className = entry.getName().replace('/', '.').replace(".class", "");
      try {
        Class<?> clazz = Class.forName(className, false, classLoader);
        if (clazz.isAnnotationPresent(PluginMetadata.class)) {
          if (result != null) {
            System.err.println(
                "[Citadel] Multiple @PluginMetadata classes found in "
                    + jar.getName()
                    + "; expected exactly one");
            return null;
          }
          result = clazz;
        }
      } catch (ClassNotFoundException | NoClassDefFoundError e) {
        // Skip classes that cannot be loaded
      }
    }
    return result;
  }

  /**
   * Simple prefix-based API version check. Supports "major.minor.patch" format.
   *
   * <p>The plugin is considered compatible if its required major version equals the core's major
   * version. For snapshot versions, the base version is used for comparison.
   */
  static boolean isApiCompatible(String requiredVersion, String coreVersion) {
    int requiredMajor = parseMajor(requiredVersion);
    int coreMajor = parseMajor(coreVersion);
    return requiredMajor == coreMajor;
  }

  private static int parseMajor(String version) {
    String normalised = version;
    if (normalised.endsWith("-SNAPSHOT")) {
      normalised = normalised.substring(0, normalised.length() - "-SNAPSHOT".length());
    }
    String[] parts = normalised.split("\\.");
    try {
      return Integer.parseInt(parts[0]);
    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
      return -1;
    }
  }
}
