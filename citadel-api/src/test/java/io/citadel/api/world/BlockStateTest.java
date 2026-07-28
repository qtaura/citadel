package io.citadel.api.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BlockStateTest {

  @Test
  void constructorStoresFields() {
    BlockState state = new BlockState(1, 2);
    assertEquals(1, state.id());
    assertEquals(2, state.data());
  }

  @Test
  void airConstant() {
    assertTrue(BlockState.AIR.isAir());
    assertEquals(0, BlockState.AIR.id());
    assertEquals(0, BlockState.AIR.data());
  }

  @Test
  void nonAirState() {
    assertFalse(new BlockState(1, 0).isAir());
    assertFalse(new BlockState(5, 3).isAir());
  }

  @Test
  void equalsAndHashCode() {
    BlockState a = new BlockState(1, 2);
    BlockState b = new BlockState(1, 2);
    BlockState c = new BlockState(1, 3);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
  }

  @Test
  void toStringForAir() {
    String str = BlockState.AIR.toString();
    assertTrue(str.contains("AIR"));
  }

  @Test
  void toStringForNonAir() {
    String str = new BlockState(5, 3).toString();
    assertTrue(str.contains("5"));
    assertTrue(str.contains("3"));
  }
}
