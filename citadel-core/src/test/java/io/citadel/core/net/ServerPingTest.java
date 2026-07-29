package io.citadel.core.net;

public final class ServerPingTest {
  public static void main(String[] args) throws Exception {
    // Ping 6b6t to see if it sends online-mode info in status
    NetworkClient client =
        new NetworkClient(
            new io.citadel.api.event.EventBus() {
              public <T extends io.citadel.api.event.Event>
                  io.citadel.api.event.Subscription subscribe(
                      Class<T> type, io.citadel.api.event.EventHandler<T> handler) {
                return () -> {};
              }

              public <T extends io.citadel.api.event.Event> void unsubscribe(
                  Class<T> type, io.citadel.api.event.EventHandler<T> handler) {}

              public void publish(io.citadel.api.event.Event event) {}

              public void publishAsync(io.citadel.api.event.Event event) {}
            },
            new io.citadel.api.service.Logger() {
              public boolean isTraceEnabled() {
                return false;
              }

              public boolean isDebugEnabled() {
                return false;
              }

              public boolean isInfoEnabled() {
                return false;
              }

              public boolean isWarnEnabled() {
                return false;
              }

              public boolean isErrorEnabled() {
                return false;
              }

              public void trace(String m) {}

              public void trace(String f, Object... a) {}

              public void trace(String m, Throwable t) {}

              public void trace(String a, String m) {}

              public void trace(String a, String f, Object... v) {}

              public void trace(String a, String m, Throwable t) {}

              public void debug(String m) {}

              public void debug(String f, Object... a) {}

              public void debug(String m, Throwable t) {}

              public void debug(String a, String m) {}

              public void debug(String a, String f, Object... v) {}

              public void debug(String a, String m, Throwable t) {}

              public void info(String m) {}

              public void info(String f, Object... a) {}

              public void info(String m, Throwable t) {}

              public void info(String a, String m) {}

              public void info(String a, String f, Object... v) {}

              public void info(String a, String m, Throwable t) {}

              public void warn(String m) {}

              public void warn(String f, Object... a) {}

              public void warn(String m, Throwable t) {}

              public void warn(String a, String m) {}

              public void warn(String a, String f, Object... v) {}

              public void warn(String a, String m, Throwable t) {}

              public void error(String m) {}

              public void error(String f, Object... a) {}

              public void error(String m, Throwable t) {}

              public void error(String a, String m) {}

              public void error(String a, String f, Object... v) {}

              public void error(String a, String m, Throwable t) {}
            });

    try {
      var status = client.queryStatus("6b6t.org", 25565);
      System.out.println("Players online: " + status.playersOnline());
      System.out.println("Max players: " + status.playersMax());
      System.out.println("Description: " + status.description());
      System.out.println("Version: " + status.versionName());
    } catch (Exception e) {
      System.out.println("Ping failed: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
