package io.citadel.core.net;

import io.citadel.api.network.ProtocolState;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface Packet {

  int getPacketId(ProtocolState state);

  void write(DataOutput out) throws IOException;

  void read(DataInput in) throws IOException;
}
