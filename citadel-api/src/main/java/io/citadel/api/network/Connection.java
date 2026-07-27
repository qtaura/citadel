package io.citadel.api.network;

import java.io.Closeable;
import java.io.IOException;

/** Network connection to a Minecraft server. */
public interface Connection extends Closeable {

  void connect() throws IOException;

  ProtocolState getProtocolState();

  ConnectionState getState();

  boolean isConnected();

  String getHost();

  int getPort();

  void setReadTimeout(int readTimeout) throws IOException;
}
