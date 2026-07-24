package io.citadel.core.config;

/**
 * Generates the default Citadel configuration file as a YAML string.
 *
 * <p>The default configuration is heavily commented and organized so that users rarely need to
 * consult external documentation. It includes placeholder sections for all future subsystems:
 * networking, authentication, proxies, accounts, logging, dashboard, and plugins.
 */
final class DefaultConfiguration {

  private DefaultConfiguration() {}

  /** Returns the default configuration as a YAML string. */
  static String generate() {
    return """
        # Citadel Configuration File
        # ===========================
        # This file was automatically generated on first startup.
        # It contains default values for all configuration sections.
        #
        # To customize Citadel, edit this file and restart (or call reload).
        # All values are optional -- defaults are used when not specified.
        #
        # Configuration sections:
        #   networking    -- Minecraft server connection settings
        #   authentication -- Microsoft/Mojang auth settings
        #   proxies       -- Proxy server pool configuration
        #   accounts      -- Minecraft account definitions
        #   logging       -- Log output configuration
        #   dashboard     -- Web dashboard settings
        #   plugins       -- Per-plugin configuration namespaces

        # --- Networking ---
        # Server connection parameters.
        # Individual accounts can override these defaults.
        networking:
          server:
            # The hostname or IP address of the Minecraft server
            host: "localhost"
            # The port number (default: 25565)
            port: 25565

        # --- Authentication ---
        # Microsoft/Mojang authentication settings.
        authentication:
          # Authentication method: "microsoft" or "mojang"
          method: "microsoft"
          # Credential storage encryption
          encryption:
            # Encryption algorithm
            algorithm: "AES-256-GCM"

        # --- Proxies ---
        # Proxy configuration for distributing connections.
        # Each proxy is identified by name and supports SOCKS5 and HTTP.
        proxies:
          # Default proxy pool (empty = direct connections)
          pool: []

        # --- Accounts ---
        # Minecraft account definitions.
        # Each account needs a unique name and credentials.
        # Example:
        #   accounts:
        #     my_account:
        #       username: "user@example.com"
        #       auth_method: "microsoft"
        #       server: "my.server.com"
        accounts: {}

        # --- Logging ---
        # Log output configuration.
        logging:
          # Minimum log level: TRACE, DEBUG, INFO, WARN, ERROR, FATAL
          level: "INFO"
          # Log file path (relative to working directory)
          file: "logs/citadel.log"
          # Maximum log file size before rotation (e.g., "10MB", "1GB")
          max_size: "10MB"
          # Maximum number of archived log files to keep
          max_history: 7

        # --- Dashboard ---
        # Web dashboard server configuration.
        dashboard:
          # Enable the dashboard server
          enabled: true
          # Bind address (use 0.0.0.0 for all interfaces)
          host: "0.0.0.0"
          # Web server port
          port: 8080
          # Dashboard authentication
          authentication:
            # Set to a password to enable authentication
            password: ""
          # WebSocket connection for real-time updates
          websocket:
            enabled: true

        # --- Plugins ---
        # Plugin-specific configuration.
        # Each plugin automatically receives its own section.
        # Example:
        #   plugins:
        #     map-art:
        #       image_path: "input.png"
        #       accounts: ["builder1", "builder2"]
        plugins: {}
        """;
  }
}
