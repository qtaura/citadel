# Citadel — Roadmap

**Status:** Pre-Implementation Planning
**License:** GPLv3

---

## Overview

This document describes exactly how Citadel will be built from an empty repository to the v1.0 release. It is an implementation roadmap, not an architecture document. The architecture is described in `ARCHITECTURE.md` and is the authoritative source for design decisions.

Every milestone in this roadmap produces working, testable software. No milestone is a "black hole" — if a milestone takes too long, its scope is wrong and should be reduced.

The milestones are ordered to reflect two principles from the architecture: **build infrastructure before features** and **stabilize APIs before relying on them**. Earlier milestones are foundational. Later milestones compose those foundations into a working platform.

---

## Development Principles

These rules guide every contribution to Citadel. They are derived from the architecture's guiding principles and are enforced during code review.

### Every Milestone Must Be Runnable

After every milestone, the project builds and runs. There is no "broken while we finish this feature" period. If a milestone's scope makes this impossible, the milestone is too large and must be split.

### Write Documentation Alongside Code

Public API surfaces are documented before the first release. Internal code is documented where its purpose is not obvious. Documentation is reviewed alongside code, not added later.

### Tests Accompany New Functionality

Every new subsystem includes tests that cover its public contract. Bug fixes include tests that reproduce the bug. Tests are part of the milestone's definition of done.

### Refactor Only When Necessary

If the code works and is not blocking the next milestone, leave it. Refactoring for its own sake delays working software. When refactoring is necessary, it is done as a separate, small milestone.

### Keep the Core Small

If a feature could be implemented as a plugin without modifying `citadel-api` or `citadel-core`, propose it as a plugin. The core is infrastructure, not a feature bucket. See `ARCHITECTURE.md` section 6.5 for what belongs in the core.

### Prefer Plugins Over Core Features

This is the single most important rule. When in doubt, build it as a plugin. The Map Art and Discord plugins are not "examples" — they are proof that the plugin API works. If a plugin cannot be built against the API, the API is missing something.

### Solve Real Problems Before Theoretical Ones

Do not build infrastructure for future use cases that have not yet arrived. The world cache, event bus, and scheduler all start simple. They are extended only when a plugin or user demonstrates a concrete need.

### Target the Current Minecraft Version

The networking layer targets the current major Minecraft release. Protocol versioning is centralized. Supporting multiple Minecraft versions is explicitly deferred. See `ARCHITECTURE.md` section 4 (Non-Goals).

---

## Progress Tracker

### Stage 1 — Foundation

- [ ] 1. Project Initialization
- [ ] 2. Public API
- [ ] 3. Core Bootstrap

### Stage 2 — Core Infrastructure

- [ ] 4. Plugin Loader
- [ ] 5. Configuration
- [ ] 6. Logging
- [ ] 7. Event Bus

### Stage 3 — Networking

- [ ] 8. Networking Foundation
- [ ] 9. Authentication
- [ ] 10. Account Management
- [ ] 11. Minecraft Connections
- [ ] 12. Multi-Account Management

### Stage 4 — Platform

- [ ] 13. Dashboard Backend
- [ ] 14. Dashboard Frontend
- [ ] 15. Plugin API Stabilization

### Stage 5 — Official Plugins

- [ ] 16. Map Art Plugin (MVP)
- [ ] 17. Discord Integration Plugin

### Stage 6 — Release

- [ ] 18. Testing & Performance
- [ ] 19. Public Beta
- [ ] 20. Version 1.0

---

## Milestones

---

### Milestone 1 — Project Initialization

**Stage:** Foundation

**Difficulty:** ⭐ Very Easy

**Depends On:** None

**Goal:** Set up the repository, build system, toolchain, and CI so that every contributor works in the same environment.

**Why:** Before any code is written, the project infrastructure must be established. This avoids "works on my machine" problems later and provides a consistent developer experience from day one.

**Deliverables:**
- Git repository with the monorepo structure described in `ARCHITECTURE.md` section 7.1
- Build system configuration with multi-module support (`citadel-api`, `citadel-core`, `citadel-dashboard`, `citadel-dashboard-ui`, `plugins/`)
- Java toolchain configuration (target version, language level)
- Code formatting configuration enforced in CI
- Static analysis configuration
- Continuous integration workflow (build on every push and pull request)
- `README.md` with project description, build instructions, and license
- `LICENSE` file (GPLv3)
- `.gitignore` for the build system and IDE artifacts

**Success Criteria:**
- Fresh clone followed by build produces a clean output with no errors
- CI passes on every push
- Code formatting check fails on improperly formatted code and passes on correct code
- All module directories exist and compile (containing only placeholder classes)
- A new contributor can clone, build, and open the project in their IDE within 10 minutes

**Out of Scope:**
- Any application logic
- Plugin API design (next milestone)
- Dashboard frontend toolchain

---

### Milestone 2 — Public API

**Stage:** Foundation

**Difficulty:** ⭐⭐ Easy

**Depends On:**
- Milestone 1

**Goal:** Define the `citadel-api` interfaces that all other modules depend on.

**Why:** The architecture mandates that `citadel-api` is designed before `citadel-core` is implemented (ARCHITECTURE.md, principle 2.4). The API is the contract between the core and plugins. Its interfaces must be stable before implementation begins.

**Deliverables:**
- `Plugin` interface with lifecycle methods (onLoad, onEnable, onDisable)
- `PluginContext` interface for accessing core services
- `Event` base class and `EventBus` interface
- Core service interfaces (Configuration, Scheduler, AccountManager, Logger — empty contracts initially)
- Metadata annotation for plugin discovery (`@Plugin` with name, version, apiVersion)
- Package structure: `io.citadel.api.plugin`, `io.citadel.api.event`, `io.citadel.api.service`

**Success Criteria:**
- `citadel-api` compiles with zero dependencies on any other module
- A stub plugin can be written against the API without importing any internal classes
- All interfaces have documentation
- The API module has its own version number, independent of the core version

**Out of Scope:**
- Implementation of any service interface
- Event dispatch logic (the EventBus interface exists, the implementation is in citadel-core)
- Plugin discovery or loading
- Any Minecraft-specific types

---

### Milestone 3 — Core Bootstrap

**Stage:** Foundation

**Difficulty:** ⭐⭐ Easy

**Depends On:**
- Milestone 1
- Milestone 2

**Goal:** Citadel starts, shuts down gracefully, and provides a service registry.

**Why:** The core is the application entry point. Establishing the startup and shutdown lifecycle early ensures that every subsequent milestone has a foundation to build on.

**Deliverables:**
- `Citadel` main class with command-line argument parsing
- Application lifecycle (start → running → shutdown)
- Graceful shutdown handling (SIGTERM, SIGINT)
- Service registry where subsystems register themselves and plugins access them through `PluginContext`
- Logging of startup progress and shutdown completion (console output only)

**Success Criteria:**
- Running the application prints "Citadel starting..." to stdout
- Calling `serviceRegistry.get(ServiceType.class)` returns a non-null instance for every registered service
- Pressing Ctrl+C triggers graceful shutdown and all registered services receive a shutdown notification
- No exceptions are thrown during startup or shutdown
- The application exits with code 0 on normal shutdown

**Out of Scope:**
- Plugin loading (next milestone)
- Configuration file parsing (next stage)
- Networking or Minecraft-specific code

---

### Milestone 4 — Plugin Loader

**Stage:** Core Infrastructure

**Difficulty:** ⭐⭐⭐ Moderate

**Depends On:**
- Milestone 1
- Milestone 2
- Milestone 3
- Milestone 6 (logging must exist before plugins can produce visible output)

**Goal:** Citadel discovers and loads plugins from a directory, managing their lifecycle.

**Why:** The plugin architecture is the defining characteristic of Citadel. Before any other infrastructure is built, the plugin loader must work so that all subsequent features can be tested through plugins.

**Deliverables:**
- Plugin discovery from a configurable directory (default `plugins/`, initially hardcoded)
- Plugin artifact parsing (JAR files with metadata from annotations or manifest)
- Classloader isolation (each plugin loads in its own classloader; the API JAR is the shared parent)
- Lifecycle management (LOAD → ENABLE → DISABLE → UNLOAD)
- API version compatibility check (reject plugins built for a different API version)
- Plugin context injection (plugins receive access to core services)
- Error isolation (a plugin that throws during onEnable does not crash the core)

**Success Criteria:**
- A "Hello World" plugin JAR placed in the `plugins/` directory has its `onEnable` method called on startup
- Plugin lifecycle transitions are logged (plugin name, state change)
- Removing the plugin JAR and restarting shows it is no longer loaded
- A plugin that throws an exception during `onEnable` produces a logged error and is disabled while the core continues running
- A plugin built against a different API version produces a clear error message and is not loaded

**Out of Scope:**
- Plugin hot-reloading (runtime add/remove without restart)
- Plugin dependency resolution
- Plugin download or marketplace
- Security sandbox beyond classloader isolation

---

### Milestone 5 — Configuration

**Stage:** Core Infrastructure

**Difficulty:** ⭐⭐ Easy

**Depends On:**
- Milestone 1
- Milestone 2
- Milestone 3

**Goal:** Citadel loads configuration from files and provides it to plugins and core subsystems.

**Why:** Almost every subsequent milestone requires configuration — server addresses, account credentials, proxy settings. The configuration system must exist before any of these features are built.

**Deliverables:**
- File-based configuration loading with sensible defaults for every setting
- Configuration format supporting comments and hierarchical structure
- Configuration validation (type checking, required fields produce clear error messages)
- Plugin-namespaced configuration (each plugin reads its own section without parsing global config)
- Programmatic reload API (not file watching — a method call triggers reparse and notification)

**Success Criteria:**
- On startup, the configuration file is loaded and every value is readable through the Configuration API
- An invalid value in the configuration file produces a logged error pointing to the exact field path
- A plugin reads its own configuration section without needing to parse or know about the global config structure
- When a configuration key is absent, the default value is returned instead
- Calling the reload API re-reads the configuration file and notifies registered listeners

**Out of Scope:**
- Configuration UI in the dashboard
- Environment variable overrides
- CLI argument overrides
- Automatic file-watching reload

---

### Milestone 6 — Logging

**Stage:** Core Infrastructure

**Difficulty:** ⭐⭐ Easy

**Depends On:**
- Milestone 1
- Milestone 2
- Milestone 3

**Goal:** Citadel provides structured, configurable logging for the core and plugins.

**Why:** Logging is the primary debugging and monitoring tool. Every subsequent milestone produces log output. The logging system must exist before networking, authentication, or plugins produce their first log messages.

**Deliverables:**
- Centralized logging infrastructure wrapping a standard logging framework
- Console output with configurable log levels (TRACE through FATAL)
- Per-plugin logger assignment (each plugin gets a named logger automatically, namespaced as `citadel.plugin.<name>`)
- Infrastructure for per-account log correlation (log calls accept an optional account parameter)
- Configurable log format (plain text for console, structured for file)
- File output with rotation at configurable size or schedule

**Success Criteria:**
- Core log messages and plugin log messages appear in the console
- Setting log level to ERROR suppresses DEBUG and INFO messages
- Each plugin's logger produces output prefixed with the plugin name
- A log call with an account parameter includes the account name in the output line
- Log output is written to a file and the file is rotated at the configured threshold

**Out of Scope:**
- Dashboard log streaming (added when the dashboard is built)
- Centralized log aggregation (SELDON, Logstash, etc.)
- Per-account log file separation

---

### Milestone 7 — Event Bus

**Stage:** Core Infrastructure

**Difficulty:** ⭐⭐⭐ Moderate

**Depends On:**
- Milestone 1
- Milestone 2
- Milestone 3
- Milestone 6 (event bus operations are logged)

**Goal:** Core systems and plugins communicate through typed events.

**Why:** The event bus is the backbone of internal communication (ARCHITECTURE.md, section 8.2.4). Before networking or account management exist, the event bus must be ready for them to use.

**Deliverables:**
- Type-based event dispatch (subscribers register for event classes and receive only those types)
- Asynchronous event delivery by default (publisher does not block)
- Synchronous delivery for critical-path handlers (opt-in, publisher blocks until all handlers complete)
- Plugin-to-plugin event publishing
- Error isolation (one handler's exception is caught and logged; other handlers still receive the event)
- Event metadata (timestamp, source plugin, optional account ID)

**Success Criteria:**
- A plugin subscribes to an event type and receives the event object when it is published by another plugin or the core
- Multiple subscribers registered for the same event type all receive the event
- An exception thrown in one subscriber does not prevent other subscribers from receiving the event
- A synchronous subscriber blocks the publisher until its handler returns
- Events carry a timestamp and source identifier accessible to subscribers

**Out of Scope:**
- Topic-based routing (type-based dispatch is sufficient for v1.0)
- Event prioritization
- Backpressure (added if profiling demonstrates the need)
- Event persistence or replay

---

### Milestone 8 — Networking Foundation

**Stage:** Networking

**Difficulty:** ⭐⭐⭐⭐ Difficult

**Depends On:**
- Milestone 1
- Milestone 2
- Milestone 3
- Milestone 5 (server address from config)
- Milestone 6
- Milestone 7

**Goal:** Citadel can establish a TCP connection to a Minecraft server, perform the protocol handshake, and emit connection state events.

**Why:** Networking is the primary feature of Citadel. Before authentication or account management exist, the protocol foundation must be built and tested in isolation.

**Deliverables:**
- Non-blocking TCP connection management (single thread manages multiple connections via a selector)
- Minecraft protocol packet encoding and decoding for the HANDSHAKE and LOGIN phases
- Packet registry mapping packet IDs to codecs for the supported Minecraft version
- Connection state machine (HANDSHAKE → LOGIN → PLAY → DISCONNECTED)
- Server List Ping implementation for testing protocol correctness against a real server
- Connection state change events emitted on the event bus

**Success Criteria:**
- Citadel opens a TCP connection to a Minecraft server address from configuration
- A server list ping request succeeds and returns the server's MOTD, player count, and version string
- The handshake and login start packets are correctly encoded, sent, and acknowledged by the server
- The connection state machine transitions through HANDSHAKE and LOGIN states
- Server-initiated disconnection is detected and the state machine transitions to DISCONNECTED
- Every state transition produces a corresponding event on the event bus

**Out of Scope:**
- Encryption and compression (added in milestone 11)
- Online-mode authentication (next milestone)
- PLAY state packet handling beyond position and keep-alive
- Account management

---

### Milestone 9 — Authentication

**Stage:** Networking

**Difficulty:** ⭐⭐⭐ Moderate

**Depends On:**
- Milestone 1
- Milestone 2
- Milestone 3
- Milestone 5 (credential storage path from config)
- Milestone 6
- Milestone 7

**Goal:** Citadel authenticates a Minecraft account through Microsoft's OAuth flow, stores the session securely, and refreshes tokens automatically.

**Why:** Modern Minecraft servers require online-mode authentication. Without it, no account can join a server. This milestone makes authentication an isolated subsystem that the connection layer depends on.

**Deliverables:**
- Microsoft OAuth authentication flow (device code flow)
- Mojang/Yggdrasil token exchange
- Access token and refresh token management
- Automatic token refresh triggered before expiration
- Encrypted credential storage on disk (master key generated on first launch and stored in the configuration directory)
- Rate limiting for authentication requests to Microsoft's servers
- Manual token injection fallback (advanced users can provide tokens directly without a browser)
- Auth lifecycle events emitted on the event bus (AUTH_SUCCEEDED, AUTH_FAILED, TOKEN_REFRESHED)

**Success Criteria:**
- Given valid Microsoft account credentials in the configuration file, Citadel obtains an access token through the device code flow
- The access token and refresh token are stored encrypted on disk in the configuration directory
- On restart, the stored token is loaded and reused without re-authentication (if still within validity period)
- Token refresh is triggered automatically at a configurable interval before expiration
- Failed authentication produces a logged error indicating the specific cause (wrong credentials, network error, Microsoft error response)
- Manual token input works as a fallback via configuration

**Out of Scope:**
- Credential management UI (added with the dashboard)
- Multi-factor authentication beyond what Microsoft OAuth provides natively
- Offline-mode (cracked) server support

---

### Milestone 10 — Account Management

**Stage:** Networking

**Difficulty:** ⭐⭐ Easy

**Depends On:**
- Milestone 1
- Milestone 2
- Milestone 3
- Milestone 5 (account definitions from config)
- Milestone 6
- Milestone 7
- Milestone 9 (credential storage)

**Goal:** Accounts are loaded from configuration, organized into groups, and assigned to proxies.

**Why:** Before accounts can connect to servers, they must be configured with credentials, server addresses, proxy assignments, and grouping. This milestone provides the account model that the connection layer uses.

**Deliverables:**
- Account data model loaded from configuration (username, credentials reference, server address, group, proxy)
- Account grouping (accounts belong to named groups; groups have default settings)
- Account state tracking (ONLINE, OFFLINE, ERROR, CONNECTING) — transitions driven by events from later milestones
- Account-to-proxy assignment (accounts use a group-level proxy or an account-specific override)
- Proxy pool configuration (list of proxies with addresses, protocol, and optional credentials)
- SOCKS5 and HTTP proxy support in the networking layer
- Events emitted on config load: ACCOUNT_REGISTERED, ACCOUNT_REMOVED, ACCOUNT_STATE_CHANGED

**Success Criteria:**
- A configuration file declaring accounts with credentials, group membership, and proxy assignments is loaded on startup
- Accounts are organized into named groups and each group's accounts are enumerable through the API
- Each account can be assigned a specific proxy or inherit the group-level proxy
- Proxy connection parameters are correctly configured in the networking layer (actual proxy connection is tested in milestone 11)
- Account state transitions emit events on the event bus
- An invalid account configuration (missing required field, unknown server) produces a logged error pointing to the specific account

**Out of Scope:**
- Proxy health checking (simple try-connect is sufficient for v1.0)
- Advanced proxy assignment strategies (round-robin, least-loaded)
- Account management UI (added with the dashboard)

---

### Milestone 11 — Minecraft Connections

**Stage:** Networking

**Difficulty:** ⭐⭐⭐⭐⭐ Very Difficult

**Depends On:**
- Milestone 1 through Milestone 10

**Goal:** A single account authenticates, connects to a Minecraft server, and maintains the connection with full encryption, compression, keep-alive handling, and reconnection.

**Why:** This milestone combines networking, authentication, and account management into the first meaningful capability: an account that behaves like a real Minecraft client on a server. This is the hardest milestone in the roadmap.

**Implementation Phases:**

**Phase A — Encryption and Full Login Flow**
- AES/CFB8 encryption handshake as required by the Minecraft protocol
- Compression negotiation (threshold negotiated during login)
- Full login sequence: Handshake → Login Start → Encryption Request/Response → Login Success → PLAY state entry
- Test against an online-mode Minecraft server with a real account

**Phase B — Connection Maintenance**
- Keep-alive packet handling (respond to server keep-alives within timeout)
- Player position and rotation updates sent periodically
- Disconnection detection (socket close, timeout, kicked by server)
- Automatic reconnection with exponential backoff (1s, 2s, 4s, 8s, capped at 60s)

**Phase C — Packet Infrastructure**
- Packet routing from the networking layer to the event bus for plugin consumption
- Connection lifecycle events on the event bus: CONNECTED, DISCONNECTED, RECONNECTING, RECONNECTED
- Basic PLAY state packet handling (keep-alive, player position, chat, disconnect)

**Success Criteria:**
- An account authenticates and joins an online-mode Minecraft server
- The server's player list shows the account as logged in
- Keep-alive packets are exchanged between client and server without timeout on either side
- Manually disconnecting the server or network triggers automatic reconnection attempts with observed backoff
- The event bus emits CONNECTED when the account joins and DISCONNECTED when it leaves
- A plugin subscribed to CONNECTED events receives the event with the account ID and server information
- The account appears in the correct position and rotation on the server

**Out of Scope:**
- Multiple simultaneous connections (next milestone)
- Full PLAY state packet decoding (only essential packets are decoded; unknown packets are forwarded as raw data)
- World state tracking
- Inventory management

---

### Milestone 12 — Multi-Account Management

**Stage:** Networking

**Difficulty:** ⭐⭐⭐ Moderate

**Depends On:**
- Milestone 11

**Goal:** Many accounts connect simultaneously and are monitored centrally.

**Why:** The defining capability of Citadel is managing many accounts at once. This milestone proves the architecture scales beyond a single connection.

**Deliverables:**
- Concurrent connection management (many TCP connections on a shared non-blocking I/O thread)
- Per-account event streams (events are tagged with the originating account ID)
- Connection health tracking (latency measured from keep-alive round trips, connection uptime, disconnection rate)
- Bulk operations (connect all accounts, disconnect all, reconnect a group)
- Resource monitoring (per-account memory estimate, total connection count, active vs idle accounts)

**Success Criteria:**
- 10 accounts from the configuration file connect to the same server simultaneously
- Each account maintains its own independent connection state
- Disconnecting one account through the API leaves the other 9 connections unaffected
- The event bus delivers events tagged with the correct account ID for each account's connection
- A plugin enumerates all connected accounts via the API and receives their individual states
- Memory usage per account is measured and logged (no target number, just measured)

**Out of Scope:**
- Dashboard display (added in the next stage)
- Coordinated actions between accounts (Map Art milestone)
- Scale testing beyond 10 simultaneous accounts (Testing & Performance milestone)

---

### Milestone 13 — Dashboard Backend

**Stage:** Platform

**Difficulty:** ⭐⭐⭐ Moderate

**Depends On:**
- Milestone 1 through Milestone 12

**Goal:** An HTTP API exposes account status, events, logs, and actions for the dashboard frontend.

**Why:** The dashboard is the primary interface for human operators (ARCHITECTURE.md, target user group 1). The backend must exist before the frontend can display anything. The core has no dependency on the dashboard — it is an optional consumer that communicates through the event bus and API.

**Deliverables:**
- Embedded HTTP server in the `citadel-dashboard` module
- Account listing endpoint (returns JSON with status, server, latency, uptime for every account)
- Account action endpoints (POST to connect, disconnect, reconfigure individual accounts)
- Real-time event stream endpoint (server-sent events or WebSocket delivering account state changes)
- Log stream endpoint (returns recent log entries with filtering by level, plugin, and account)
- Plugin listing endpoint (installed plugins, versions, enabled/disabled state)
- Dashboard authentication (disabled by default; optional config-file password to enable)
- Health check endpoint

**Success Criteria:**
- A GET request to `/api/accounts` returns a JSON array where every configured account appears with its current connection state
- A POST request to `/api/accounts/<name>/connect` triggers a connection attempt and the response reflects the new state
- The event stream endpoint delivers real-time account state changes as they happen
- The log stream endpoint returns recent log entries and supports filtering by log level
- An unauthenticated request is rejected with HTTP 401 when authentication is enabled
- The dashboard backend starts and stops independently without affecting running account connections

**Out of Scope:**
- Frontend UI (next milestone)
- Plugin-specific endpoints (dashboard exposes core views only)
- Metrics export (Prometheus, etc.)
- TLS/SSL (reverse proxy is recommended for production deployments)

---

### Milestone 14 — Dashboard Frontend

**Stage:** Platform

**Difficulty:** ⭐⭐⭐ Moderate

**Depends On:**
- Milestone 13

**Goal:** A web UI displays account status in real time and provides basic management actions.

**Why:** The dashboard frontend makes Citadel accessible to non-technical users. Without it, operators must use the API directly.

**Deliverables:**
- Single-page application in the `citadel-dashboard-ui` module
- Account list view with colored status indicators (green for connected, red for disconnected, yellow for error, gray for offline)
- Account detail view showing connection history, recent logs, and configuration
- Real-time status updates via the event stream (no page refresh needed)
- Log viewer with level filtering, plugin filtering, and text search
- Plugin list view showing installed plugins, their versions, and enabled/disabled state
- Basic management actions (connect and disconnect accounts from the UI)

**Success Criteria:**
- Opening the dashboard URL in a browser displays the account list with correct status indicators
- When an account's connection state changes on the server, the UI updates within 2 seconds without manual refresh
- Clicking "Connect" on a disconnected account triggers the connection and the status indicator updates to green
- The log viewer shows log entries and filtering by level removes entries below the selected threshold
- The plugin list displays installed plugins with their version and current state
- The frontend communicates only with the dashboard backend API and makes no direct calls to core internals

**Out of Scope:**
- Plugin-specific UI panels
- Advanced account configuration forms (text editor in the browser or direct config file editing is sufficient)
- Mobile-responsive design (desktop-only for v1.0)
- Dashboard theming or customization

---

### Milestone 15 — Plugin API Stabilization

**Stage:** Platform

**Difficulty:** ⭐⭐ Easy

**Depends On:**
- Milestone 1 through Milestone 14

**Goal:** The `citadel-api` is reviewed, documented, and frozen for external plugin developers.

**Why:** Before external developers can write plugins, the API must be stable, documented, and accompanied by examples. This milestone is the transition from "internal API used by official plugins" to "public SDK."

**Deliverables:**
- Full review of every `citadel-api` interface against feedback from milestones 1-14
- API documentation with usage examples for every public method and class
- Plugin development guide (tutorial-style, from downloading the API artifact to a working plugin)
- At least two complete example plugins (not counting Map Art or Discord)
- API surface annotated with stability guarantees (`@Experimental`, `@Stable`)
- Separately versioned API artifact published as a standalone dependency
- Deprecation policy documented

**Success Criteria:**
- An example plugin provided in the repository compiles and runs successfully against the released API artifact
- Every public API surface has documentation
- A breaking change requires a major API version bump
- Deprecated APIs have a documented migration path to their replacement

**Out of Scope:**
- Plugin hot-reloading
- Plugin marketplace or registry
- IDE plugin or project templates (text-based examples are sufficient)

---

### Milestone 16 — Map Art Plugin (MVP)

**Stage:** Official Plugins

**Difficulty:** ⭐⭐⭐⭐ Difficult

**Depends On:**
- Milestone 1 through Milestone 15

**Goal:** Citadel can produce a simple map art from an input image using coordinated accounts.

**Why:** Map Art is the flagship plugin and the primary use case for v1.0 (ARCHITECTURE.md, section 3). It validates the entire plugin API against a real, complex workflow. If Map Art works, the API is complete.

**Implementation Phases:**

**Phase A — Image Processing**
- Image loading from a file path in the plugin config
- Color quantization to the Minecraft map color palette
- Map splitting (large images divided across multiple map items)
- Block placement calculation (which block type at which coordinates for each pixel)

**Phase B — World Interaction**
- Player movement to target coordinates
- Block placement at calculated positions
- Inventory management (ensuring the account has the required blocks)
- Block placement confirmation (verify the block was placed correctly)

**Phase C — Multi-Account Coordination**
- Account task assignment (dividing the image into regions per account)
- Progress tracking (blocks placed, remaining, estimated time to completion)
- Fault tolerance (reassigning work when an account disconnects)
- Event emission for dashboard display and Discord notification

**Success Criteria:**
- Given a small input image (16x16 pixels) and multiple accounts, the plugin produces a completed map art on a server
- Multiple accounts place blocks simultaneously in their assigned regions
- Progress is reported through the event bus (a dashboard or plugin can subscribe to progress updates)
- When an account disconnects during the build, its remaining work is reassigned to another account
- All configuration is read from the plugin's configuration section
- The completed map's colors match the input image within the map color palette's precision

**Out of Scope:**
- Image dithering or advanced color matching (basic nearest-color quantization is sufficient)
- Multi-server coordination
- Schematic or structure block file support
- Performance optimization for very large images (maps larger than one region)
- Undo or rollback functionality

---

### Milestone 17 — Discord Integration Plugin

**Stage:** Official Plugins

**Difficulty:** ⭐⭐ Easy

**Depends On:**
- Milestone 1 through Milestone 15

**Goal:** Citadel reports account status and accepts basic commands through a Discord bot.

**Why:** Discord integration is a required v1.0 feature (ARCHITECTURE.md, section 3). Server administrators need visibility into Citadel's operation without opening the dashboard.

**Deliverables:**
- Discord bot connecting to a configured guild and channel
- Status reporting (connected account count, server addresses, uptime)
- Command support (`!status`, `!list`, `!reconnect <account>`, `!plugins`)
- Event-driven notifications (account connects or disconnects → Discord message)
- Configuration via plugin config section (bot token, channel ID, command prefix)
- Error handling and automatic reconnection for the bot itself

**Success Criteria:**
- The bot appears online in the configured Discord server
- `!status` responds with the number of connected accounts and total accounts
- `!reconnect <account>` triggers a reconnection for the specified account and confirms in the channel
- An account connecting or disconnecting posts a notification to the configured channel
- If the bot's connection to Discord drops, it reconnects automatically
- Configuration changes in the plugin's config section are picked up on reload

**Out of Scope:**
- Slash commands (text commands are sufficient for v1.0)
- Interactive components (buttons, modals)
- Dashboard-style management through Discord (status and basic commands only)
- Multi-server Discord support

---

### Milestone 18 — Testing & Performance

**Stage:** Release

**Difficulty:** ⭐⭐⭐ Moderate

**Depends On:**
- Milestone 16
- Milestone 17

**Goal:** Citadel is stable under realistic load and the plugin API is validated against real use.

**Why:** Before external users can rely on Citadel, its performance characteristics must be understood and its stability must be demonstrated. This milestone is the quality gate before the public beta.

**Deliverables:**
- Stress test suite (10 simultaneous accounts, sustained operation for 24 hours)
- Memory profiling under load (per-account overhead, world cache memory, event queue depth)
- CPU profiling under load (event bus throughput, packet processing per connection)
- Connection stability tests (network interruptions, server restarts, authentication failures)
- Plugin API integration tests (every public interface exercised end-to-end)
- Performance regression benchmarks running in CI
- Bug fixes discovered during testing
- Documentation review and updates

**Success Criteria:**
- 10 concurrent accounts sustain stable connections for 24 hours without any account dropping unexpectedly
- Per-account memory overhead is measured and documented in the project wiki or README
- Event bus throughput during peak load (all 10 accounts sending position updates simultaneously) shows no message loss or excessive queue buildup
- Plugin API integration tests pass for every public interface in `citadel-api`
- CI benchmarks flag performance regressions before they are merged
- No known critical or high-severity bugs remain in the issue tracker

**Out of Scope:**
- Performance optimization beyond what profiling reveals as necessary (optimizations are done in separate follow-up milestones)
- Scale testing beyond 10 simultaneous accounts
- Third-party security audit

---

### Milestone 19 — Public Beta

**Stage:** Release

**Difficulty:** ⭐ Very Easy

**Depends On:**
- Milestone 18

**Goal:** External users can evaluate Citadel and provide feedback before the stable release.

**Why:** Before the v1.0 release, real users must validate the platform. A beta period catches issues that internal testing misses and provides feedback on documentation, setup, and usability.

**Deliverables:**
- Published beta release (GitHub release with prebuilt artifacts)
- Quickstart guide: download → configure accounts → run → see accounts connect in the dashboard
- Contribution guide with code standards, review process, and issue templates
- Community channels (Discord server, GitHub Discussions)
- Issue tracker configured with labels and templates
- FAQ document covering common setup issues
- Plugin development guide updated with beta feedback

**Success Criteria:**
- Following the quickstart guide, a new user downloads, configures, runs Citadel and sees at least one account connect
- External users can report issues through the issue tracker with labeled templates
- Plugin developers report that they can build plugins against the published API artifact
- Beta feedback is triaged and categorized into the v1.0 milestone
- No critical bugs discovered during the beta period require a change to the architecture or a public API break

**Out of Scope:**
- Plugin marketplace or distribution system
- Commercial support or licensing
- Mobile app or alternative dashboards

---

### Milestone 20 — Version 1.0

**Stage:** Release

**Difficulty:** ⭐ Very Easy

**Depends On:**
- Milestone 19

**Goal:** A stable, documented, and tested release of Citadel.

**Why:** This is the culmination of all previous milestones. The v1.0 release is the point where Citadel transitions from a development project to a usable platform.

**Deliverables:**
- Final API review and freeze (no breaking changes after v1.0 without a major version bump)
- Map Art plugin released as an official plugin
- Discord integration plugin released as an official plugin
- Complete documentation (architecture overview, API reference, plugin development guide, user guide, FAQ)
- Release artifacts published (platform-specific archives on GitHub Releases)
- Release notes covering all changes since the beta
- Known issues and limitations documented

**Success Criteria:**
- v1.0 artifacts are published and downloadable from GitHub Releases
- A user downloads Citadel, configures accounts, and runs the Map Art plugin without writing any code
- A developer creates a new plugin using only the published `citadel-api` artifact and the plugin development guide
- The plugin API is frozen for the v1.x release cycle (breaking changes require a v2.0)
- Documentation covers setup, configuration, plugin development, the API reference, and the system architecture
- Every milestone's success criteria from milestones 1 through 19 are satisfied

**Out of Scope:**
- Features deferred by earlier milestones (hot-reload, clustering, scripting, marketplace)
- Minecraft version updates after the target release (handled in v1.1 or later)

---

## Beyond v1.0

The following ideas are intentionally deferred. They are not part of the v1.0 roadmap but represent the natural evolution of the project.

### Additional Official Plugins

Pearl stasis, kit bots, storage management, farming, and other automation use cases. Each follows the same plugin API. The architecture does not treat any plugin as special.

### Plugin Marketplace

A community-run registry for discovering and distributing plugins. Requires mature APIs, security review processes, and community trust before it is viable.

### Plugin Hot-Reloading

Runtime plugin updates without restarting Citadel. Requires careful state management and is deferred until the community demonstrates demand.

### Advanced Scripting API

A Lua, Python, or JavaScript layer for lightweight automation scripts. Lowers the barrier for non-Java contributors but adds significant surface area. Deferred until the Java plugin API is stable and widely adopted.

### Clustering and Multi-Instance Coordination

Multiple Citadel instances managing accounts across machines, coordinated through a shared control plane. Deferred until single-instance scaling limits are proven in production.

### Performance Improvements

Specific optimizations (event bus backpressure, world cache eviction, zero-copy packet paths, connection pooling) are implemented when real-world profiling demonstrates they are needed, not before.
