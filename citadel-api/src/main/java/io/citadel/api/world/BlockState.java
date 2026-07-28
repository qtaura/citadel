package io.citadel.api.world;

public record BlockState(int id, int data) {

  public static final BlockState AIR = new BlockState(0, 0);

  public boolean isAir() {
    return id == 0;
  }

  @Override
  public String toString() {
    if (isAir()) {
      return "BlockState{AIR}";
    }
    return "BlockState{id=" + id + ", data=" + data + "}";
  }
}
