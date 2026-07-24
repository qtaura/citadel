package io.citadel.core.plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

final class PluginJarHelper {

  private PluginJarHelper() {}

  static Path createPluginJar(Class<?> pluginClass, Path targetDir)
      throws IOException, URISyntaxException {
    Path jarFile = targetDir.resolve(pluginClass.getSimpleName() + ".jar");
    String classResourcePath = pluginClass.getName().replace('.', '/') + ".class";
    URL classUrl = Thread.currentThread().getContextClassLoader().getResource(classResourcePath);
    if (classUrl == null) {
      throw new IOException("Cannot find class resource: " + classResourcePath);
    }
    Path classFile = Path.of(classUrl.toURI());
    byte[] classBytes = Files.readAllBytes(classFile);

    try (OutputStream out = Files.newOutputStream(jarFile);
        JarOutputStream jos = new JarOutputStream(out)) {
      JarEntry entry = new JarEntry(classResourcePath);
      jos.putNextEntry(entry);
      jos.write(classBytes);
      jos.closeEntry();
    }

    return jarFile;
  }
}
