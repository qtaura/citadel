package io.citadel.core.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CitadelTest {

  private static final PrintStream ORIGINAL_OUT = System.out;

  @BeforeEach
  void setUp() {
    // Redirect stdout for testing
    System.setOut(new PrintStream(new ByteArrayOutputStream()));
  }

  @AfterEach
  void tearDown() {
    System.setOut(ORIGINAL_OUT);
  }

  @Test
  void helpArgReturnsTrue() {
    assertTrue(Citadel.handleHelpOrVersion(new String[] {"--help"}));
  }

  @Test
  void shortHelpArgReturnsTrue() {
    assertTrue(Citadel.handleHelpOrVersion(new String[] {"-h"}));
  }

  @Test
  void versionArgReturnsTrue() {
    assertTrue(Citadel.handleHelpOrVersion(new String[] {"--version"}));
  }

  @Test
  void shortVersionArgReturnsTrue() {
    assertTrue(Citadel.handleHelpOrVersion(new String[] {"-v"}));
  }

  @Test
  void noArgsReturnsFalse() {
    assertFalse(Citadel.handleHelpOrVersion(new String[0]));
  }

  @Test
  void unknownArgThrowsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Citadel.handleHelpOrVersion(new String[] {"--unknown"}));
  }

  @Test
  void multipleArgsProcessesFirst() {
    // Should process --help and return true without reaching --unknown
    assertTrue(Citadel.handleHelpOrVersion(new String[] {"--help", "--unknown"}));
  }
}
