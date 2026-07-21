# Citadel — Architecture & Design Document

**Status:** Pre-Implementation Design
**License:** GPLv3

---

## 1. Vision

Citadel is an open-source, protocol-level automation platform for Minecraft Java Edition. It manages many Minecraft accounts simultaneously from a single centralized process, eliminating the need to run multiple full Minecraft clients.

Citadel exists because the current approaches to multi-account automation are inadequate:

- Running N full Minecraft clients consumes enormous resources (RAM, GPU, CPU) for what are essentially headless tasks.
- Bot scripts and libraries are fragile, lack management interfaces, and do not scale past a handful of accounts.
- Proxy-based solutions assume a real client is on the other end, which misses the point.

Citadel solves these problems by operating at the Minecraft protocol level. Each account is a lightweight virtual connection — no rendering, no game engine, no GUI. A single instance can manage many connections simultaneously while consuming a fraction of the resources that would be required by the equivalent number of game clients. A web dashboard provides management, monitoring, and control. A plugin system allows the community to extend the platform without modifying its core.

Citadel is not a hacked client, a mod, a proxy, or a general bot framework. It is a platform — infrastructure on which automation tools are built.

---

## 2. Guiding Principles

Every architectural decision in this document is guided by the following principles, ordered by priority.

### 2.1 Simplicity over Complexity

The simplest solution that meets the requirements is the right one. Complexity is not a sign of sophistication — it is a maintenance burden, a barrier to contribution, and a source of bugs. When a simple approach works adequately, prefer it over a more elegant but more complex one.

**Application:** If there is doubt about whether to add a feature, an abstraction, or a configuration option, do not add it. Wait until the need is proven.

### 2.2 Plugin-First by Default

The core provides infrastructure. Plugins provide features. This is not ceremonial — it is the single most important architectural decision in this document. If a feature could be implemented as a plugin without modifying the core, it must be.

**Application:** The plugin API is a first-class citizen, designed before the internal implementation it abstracts.

### 2.3 Solve Real Problems Before Theoretical Ones

Build for what is known, not for what is imagined. The architecture should accommodate future growth, but it should not implement infrastructure for hypothetical use cases. Premature generality is a form of overengineering.

**Application:** The world cache starts minimal. The event bus starts simple. Clustering, hot-reload, scripting APIs — these are deferred until real users demonstrate they need them.

### 2.4 Architecture Before Implementation

Module boundaries, API contracts, and data flow are designed before code is written within those boundaries. The interfaces between systems are more important than the implementations behind them.

**Application:** `citadel-api` is designed and reviewed before `citadel-core` is implemented. Internal module structure emerges from the API, not the other way around.

### 2.5 Stable APIs Over Rapid Feature Growth

The plugin API, once released, changes slowly and deliberately. Breaking changes are coordinated across major versions with migration paths. A small, stable API is better than a large, changing one.

**Application:** The public API has separate semantic versioning from the core. Deprecated APIs remain functional for at least one minor version cycle.

### 2.6 Performance as a Design Constraint, Not a Feature

Resource efficiency is a fundamental requirement, not a future optimization. The architecture must not preclude efficient operation at scale. However, specific optimizations (backpressure, caching strategies, zero-copy paths) are implemented when profiling demonstrates they are necessary, not before.

**Application:** Thread-per-connection is avoided from the start. But event bus backpressure, world cache eviction policies, and connection pooling are implemented when real-world load proves they are needed.

---

## 3. Goals

### Required (v1.0)

- **Multi-account management.** Support many accounts simultaneously from a single instance, with per-account state tracking and lifecycle management.
- **Protocol-level networking.** Accounts connect to Minecraft servers as real clients, implementing the full protocol stack (encryption, compression, authentication handshake).
- **Plugin system with public API.** All features beyond infrastructure are implemented as plugins against a documented, stable API.
- **Web-based dashboard.** A management interface for account overview, log viewing, and plugin configuration, with real-time status updates.
- **Microsoft/Mojang authentication.** Full authentication flow including token acquisition and automatic refresh.
- **Per-account proxy support.** Accounts can be assigned individual proxies (SOCKS5, HTTP) to distribute connections across IPs.
- **Account grouping.** Logical organization of accounts for coordinated operations.
- **Event bus for internal communication.** Decoupled, event-driven communication between core subsystems and plugins.
- **Secure credential storage.** Account credentials are encrypted at rest.
- **Centralized configuration.** Hierarchical configuration at global, group, account, and plugin levels.
- **Map Art plugin.** The flagship plugin; validates the entire plugin API against a real, complex use case.
- **Discord integration plugin.** Status reporting and basic control via Discord.
- **Comprehensive documentation.** Plugin development guide, API reference, and architecture overview.

### Future (v1.x+)

- **Additional official plugins.** Pearl stasis, kit bots, storage management, farming.
- **Plugin ecosystem growth.** Community-developed plugins enabled by a stable API.
- **Plugin hot-reloading.** Runtime plugin updates without restarting the platform.
- **Advanced scripting API.** Lower barrier for non-Java contributors.
- **Clustering / multi-instance coordination.** Horizontal scaling across machines.
- **Enhanced world state tracking.** Configurable chunk and entity caching for advanced automation use cases.
- **Metrics and monitoring export.** Integration with external monitoring systems.

---

## 4. Non-Goals

The following are intentionally excluded. Violating these boundaries would fundamentally change the project's identity.

- **Not a hacked client.** No aim assistance, kill aura, speed modifications, anti-knockback, or features designed to provide unfair PvP advantage. No bypassing anti-cheat systems.

- **Not a Forge/Fabric/Liteloader mod.** Citadel does not integrate with Minecraft's mod loading infrastructure. It does not modify game memory, inject bytecode, or hook into the game client.

- **Not a general Minecraft proxy.** Citadel does not route traffic between arbitrary clients and servers. It is not a replacement for BungeeCord, Velocity, or similar network proxies.

- **Not a general bot framework.** Citadel is specialized for Minecraft Java Edition automation. Its core does not accumulate abstractions for non-Minecraft use cases.

- **Not a playable client.** Citadel does not render graphics, process input, or provide an interactive gameplay experience. It manages virtual connections.

- **Not a tool for abuse.** The project discourages use for server griefing, economy manipulation, Terms of Service violations, or harassment. This is a governance stance, not an architectural constraint, but it influences which plugin features the project officially supports.

- **Not a library.** The core is a runtime platform. The internal implementation is not published as a distributable library. Only the plugin API is a published artifact.

- **Not a compatibility layer for all Minecraft versions.** The platform targets the current major Minecraft release and optionally one previous release. Supporting every version indefinitely is a maintenance trap that is explicitly avoided.

---

## 5. Target Users

Citadel is designed for four overlapping groups, listed in order of priority for v1.0:

1. **Technical Minecraft players** who manage multiple accounts for automation (map art, large-scale building, item sorting).

2. **Plugin developers** who want to build Minecraft automation tools without implementing networking, authentication, and account management from scratch.

3. **Server administrators** who operate automated systems across their networks.

4. **Content creators** who require coordinated multi-account setups for recording or events.

The v1.0 release should be immediately useful to groups 1 and 3. Groups 2 and 4 benefit from community plugins post-launch.

---

## 6. Plugin Philosophy

The decision to put nearly all functionality in plugins rather than the core is the single most important architectural choice in this document. Here is why:

### 6.1 Core Stability

The core should change slowly. Once stable, it provides a reliable foundation. Plugins can be updated, added, and removed without touching the core. A bug in a plugin should never require a core release to fix.

### 6.2 Independent Lifecycles

Map Art, Discord integration, and future plugins have different maintainers, release cadences, and testing requirements. Separate plugins allow each to evolve independently.

### 6.3 Community Contribution

If contributing requires understanding the entire codebase, few people will contribute. If contributing means writing a plugin against a clean API, many more will. The plugin boundary is the project's primary contribution surface.

### 6.4 Opt-In Distribution

An administrator who only needs Map Art should not be required to install Pearl Management. Plugins are opt-in. Plugin-specific vulnerabilities do not affect users who do not install that plugin.

### 6.5 What Belongs in the Core

The core includes only what every plugin needs and what cannot be implemented as a plugin:

- Application lifecycle and bootstrapping
- Plugin discovery, loading, and lifecycle management
- Event bus (internal communication infrastructure)
- Configuration system (loading, hierarchy, validation)
- Logging infrastructure
- Core service registries (plugins access core services through the API)

### 6.6 What Belongs in Plugins

Everything else. If there is doubt about whether something belongs in the core or a plugin, it belongs in a plugin. Feature creep into the core is the greatest long-term risk to the project's maintainability.

---

## 7. Module Structure

### 7.1 Repository Layout

A monorepo is the appropriate structure for Citadel at this stage. Monorepos provide single-version releases, atomic cross-module commits, shared build configuration, simplified CI, and easier onboarding. If the plugin ecosystem grows very large, plugins can be extracted to their own repositories later.

The project starts with four build modules:

```
citadel/
├── citadel-api/              # Public plugin API (pure abstractions, zero internal dependencies)
├── citadel-core/             # Everything internal (networking, auth, accounts, events,
│                             #   configuration, logging, scheduling, proxy management,
│                             #   world cache, metrics, plugin loader)
├── citadel-dashboard/        # Dashboard backend (optional at runtime)
├── citadel-dashboard-ui/     # Dashboard frontend
├── plugins/                  # Official plugins
│   ├── map-art/
│   ├── discord/
│   └── ...
├── tests/                    # Integration and stress tests
├── docs/                     # Documentation
├── examples/                 # Example plugins
├── scripts/                  # Build and utility scripts
└── .github/                  # CI, issue templates, contributing guide
```

### 7.2 Rationale for Merged Internal Modules

The original design proposed 13 separate internal modules (`citadel-networking`, `citadel-auth`, `citadel-config`, `citadel-event-bus`, etc.). This is unnecessarily granular for an early-stage project:

- **Separation without a team.** When a single team (or person) maintains all internal code, separate build modules add coordination overhead without providing meaningful independence.
- **Friction for contributors.** 13+ modules means 13+ build configurations, 13+ dependency declarations, and a mental model that requires understanding cross-module boundaries before making simple changes.
- **Premature artifact publishing.** Separate modules imply separately versioned, independently releasable artifacts. In practice, internal modules are always released together as part of the same Citadel version.

**Internal modularity is achieved through package boundaries, not build modules.** The `citadel-core` module uses well-defined packages (`io.citadel.core.net`, `io.citadel.core.auth`, `io.citadel.core.event`, etc.) with clear internal interfaces and no circular dependencies between packages. If a subsystem later needs independent versioning or is used by an external project, it can be extracted into its own module at that time.

### 7.3 Module Dependency Rules

- **`citadel-api`** depends on nothing. It is the pure abstraction layer.
- **`citadel-core`** depends on `citadel-api`. It contains all internal implementation.
- **`citadel-dashboard`** depends on `citadel-api`. The core does NOT depend on the dashboard — the dashboard is an optional consumer of core services.
- **`citadel-dashboard-ui`** depends only on the dashboard backend API.
- **Plugins** depend on `citadel-api` only. Plugins must NOT depend on internal packages.
- **Tests** may depend on any module.

These rules should be enforced by the build system but kept simple — no inter-module dependency resolution, no classloader hierarchy beyond what the build system provides by default.

---

## 8. Major Systems

This section describes every major responsibility area within the platform. Systems are organized by build module, with their responsibilities and architectural characteristics described. Implementation details are intentionally omitted.

### 8.1 citadel-api — Public Plugin API

The API module is the contract between the core and plugins. It defines interfaces, events, and data types that plugins implement and consume.

**Responsibilities:**
- Plugin lifecycle interface (load, enable, disable)
- Service accessor (plugins request core services by type)
- Event type hierarchy (base event types, connection events, packet events, account events)
- Core service interfaces (what plugins can call, not how they are implemented)
- Data transfer objects (account representation, connection state, configuration views)
- Metadata annotations (plugin name, version, dependencies)

**Architectural constraints:**
- Zero dependencies on any internal implementation
- Semantic versioning independent of the core version
- Breaking changes only on major version bumps
- Fully documented from day one

### 8.2 citadel-core — Internal Implementation

The core module contains all internal subsystems. They are separated by package, not by build module. Each subsystem is described below.

#### 8.2.1 Networking

Implements the Minecraft protocol and manages TCP connections for every account.

**Responsibilities:**
- Minecraft protocol implementation (packet encoding/decoding for the supported version)
- TCP connection management (connect, maintain, reconnect with exponential backoff)
- Encryption and compression as required by the Minecraft protocol
- Connection lifecycle event emission (connected, disconnected, error)
- Packet routing to event bus for plugin consumption

**Architectural notes:**
- Uses non-blocking I/O from the start. Thread-per-connection does not scale to many accounts, and retrofitting an async model later would be costly.
- Protocol versioning is centralized. Packet structures change between Minecraft versions; this versioning concern lives in one place, not scattered across the codebase.
- The connection state machine (HANDSHAKE → LOGIN → PLAY → DISCONNECTED) is explicit and testable.

#### 8.2.2 Account Management and Authentication

Stores account information, manages credentials, and handles the Microsoft/Mojang authentication flow.

**Responsibilities:**
- Encrypted credential storage on disk (master key generated on first launch)
- Microsoft OAuth authentication and automatic token refresh
- Account state tracking (online, offline, error)
- Account metadata (friendly names, groups, tags)
- Account-to-proxy assignment

**Architectural notes:**
- Authentication logic is isolated behind a clear interface. Microsoft's authentication flow changes periodically; localization of this code limits the blast radius of those changes.
- Token refresh runs before tokens expire, not after. Rate-limited to avoid Microsoft throttling.
- A manual token injection path exists as a fallback for advanced users.

#### 8.2.3 Proxy Management

Handles per-account proxy assignment and lifecycle.

**Responsibilities:**
- Proxy pool management (list of proxies with addresses and credentials)
- SOCKS5 and HTTP proxy support
- Per-account proxy assignment
- Failure detection and rotation

**Architectural notes:**
- Proxy failure does not cascade. If one proxy fails, only its assigned accounts are affected.
- v1.0 uses simple assignment strategies (round-robin, manual). Advanced strategies (latency-based, least-loaded) can be added as the proxy pool feature matures.

#### 8.2.4 Event Bus

The internal communication backbone. Decouples subsystems and enables plugin reactivity.

**Responsibilities:**
- Event type registration and dispatch
- Asynchronous event delivery by default
- Synchronous delivery available for critical-path handlers
- Plugin-to-plugin event communication

**Architectural notes:**
- v1.0 starts with a simple type-based dispatcher: subscribers register for event classes and are notified when events of those types are published. Topic-based routing, priorities, and backpressure are added only when profiling demonstrates they are needed.
- Event dispatch is asynchronous by default so that slow handlers do not block publishers. Synchronous dispatch exists for cases where the publisher must wait for handler completion (e.g., packet modification).
- Exceptions in event handlers are caught and logged. One handler's failure does not prevent other handlers from receiving the event.

#### 8.2.5 Plugin Loader

Discovers, validates, loads, and manages plugins.

**Responsibilities:**
- Plugin discovery (directory scanning for plugin artifacts)
- Plugin metadata parsing (name, version, API version requirement, dependencies)
- Lifecycle management (load → enable → disable → unload)
- Classloader isolation (each plugin loads in its own classloader)
- API version compatibility checking

**Architectural notes:**
- Each plugin gets its own classloader to prevent class conflicts and provide basic isolation. The plugin API JAR is loaded by a shared parent classloader.
- Hot-reload is not a v1.0 requirement, but the architecture should not prevent it. Plugin state should be serializable where practical.
- If a plugin's required API version is incompatible with the running core, the plugin is disabled with a clear error message, not silently broken.

#### 8.2.6 Configuration

Provides hierarchical, extensible configuration for the core and plugins.

**Responsibilities:**
- File-based configuration loading with sensible defaults
- Hierarchical merge: global defaults → group overrides → account overrides → plugin overrides
- Configuration validation (type checking, required fields)
- Plugin-namespaced configuration sections

**Architectural notes:**
- The configuration format is human-readable and supports comments and hierarchical structure. The specific format (HOCON, YAML, TOML) is an implementation choice that can be decided during Phase 0.
- Configuration changes are observable. Plugins receive a notification when their configuration is reloaded.
- v1.0 supports file-based configuration. Environment variable and CLI argument overrides are valuable additions but not required at launch.

#### 8.2.7 Scheduler

Provides delayed and periodic task execution for plugins and internal use.

**Responsibilities:**
- One-shot and repeating task scheduling
- Task cancellation
- Per-account task context (plugins associate tasks with specific accounts)

**Architectural notes:**
- Tasks execute on a dedicated thread pool, not the networking event loop.
- v1.0 provides a shared thread pool. Per-account serialization (tasks for the same account execute in order) is deferred until a plugin demonstrates it requires ordering guarantees.
- Long-running tasks are detectable and logged as warnings.

#### 8.2.8 World Cache

Tracks minimal world state for each connected account. This is the system most at risk for memory bloat, and its scope is deliberately conservative in v1.0.

**Responsibilities:**
- Player position and rotation tracking
- Chunk load/unload awareness
- Block change tracking (at configurable resolution)
- Inventory snapshot support
- Entity tracking at minimal level (presence, type, approximate position)

**Architectural notes:**
- v1.0 world cache is minimal — just enough for Map Art automation (player position, block placement confirmation, inventory state).
- Plugins subscribe to specific world events rather than receiving all updates. No plugin receives data it did not request.
- Cache eviction is essential. Unloaded chunks are discarded unless a plugin has explicitly registered interest.
- The world cache is explicitly a best-effort view. It will never be as complete as a real client's world state. Plugins must handle missing or stale data gracefully.
- As new plugins require more state (e.g., entity tracking for pearl management), the world cache is extended in a targeted way, not pre-built for hypothetical needs.

#### 8.2.9 Logging

Centralized logging infrastructure for the core and plugins.

**Responsibilities:**
- Structured logging with machine-parseable output option
- Per-account and per-plugin log namespacing
- Multiple output targets (console, file, dashboard)
- Configurable log levels
- Log rotation and retention

**Architectural notes:**
- Plugins are automatically assigned a named logger. They do not configure their own logging.
- Log entries carry account context when applicable, enabling log filtering by account.
- v1.0 delivers logs to console and file. Dashboard log streaming is added when the dashboard is built.

#### 8.2.10 Metrics

Performance instrumentation and monitoring.

**Responsibilities:**
- Memory, connection count, and event throughput tracking
- Per-plugin execution time and error rate tracking
- Networking latency tracking per account

**Architectural notes:**
- v1.0 metrics are collected in-process and exposed through the dashboard and logs.
- Metrics export to external systems (Prometheus, Grafana, etc.) is deferred. The internal collection infrastructure should be compatible with later export, but no export format is designed or committed to in v1.0.
- Per-account metrics are opt-in to avoid high-cardinality overhead.

### 8.3 citadel-dashboard — Dashboard Backend

An optional web backend that exposes core data and actions through an API.

**Responsibilities:**
- Account overview and management endpoints
- Plugin listing and configuration endpoints
- Real-time event stream for dashboard updates
- Log streaming for the dashboard log viewer
- Dashboard authentication

**Architectural notes:**
- The dashboard backend communicates with the core through the plugin API and event bus. The core has no dependency on the dashboard — it is an optional consumer.
- The API supports both request-response operations and real-time event streaming. The specific protocols (HTTP, WebSocket, or others) are implementation choices made during dashboard development.
- v1.0 dashboard authentication is simple (config-file password or disabled for local-only deployments). More sophisticated auth is deferred.
- Plugin-specific dashboard UI is not a v1.0 feature. The dashboard shows core-provided views only.

### 8.4 citadel-dashboard-ui — Dashboard Frontend

A single-page web application that provides the dashboard interface.

**Responsibilities:**
- Account status display and management
- Real-time account state visualization
- Log viewer with filtering and search
- Plugin management interface

**Architectural notes:**
- The frontend is built with a modern web framework. The specific framework is an implementation choice that does not affect any other module.
- The frontend communicates only with the dashboard backend API. It has no direct access to the core.
- v1.0 provides core dashboard views only. Plugin-specific UI extensions are deferred.

---

## 9. Development Phases

The project is built in phases, each delivering a usable milestone. Phases build on each other, but the architecture ensures that earlier phases do not need to be reworked when later phases are added.

### Phase 0 — Foundation

**Goal:** Citadel starts, loads plugins, and provides basic infrastructure.

**Deliverables:**
- Repository structure and build system
- `citadel-api` with core service interfaces (initially empty contracts)
- `citadel-core` with bootstrap, lifecycle, and shutdown handling
- Event bus with basic type-based dispatch
- Configuration system with file loading and hierarchical merge
- Logging system with console output
- Plugin loader with directory scanning and classloader isolation
- Minimal "hello world" example plugin

**Verification:** Citadel starts, loads a test plugin, the plugin receives lifecycle events and can log messages. No networking exists yet.

### Phase 1 — Networking

**Goal:** One account can connect to a Minecraft server.

**Deliverables:**
- Minecraft protocol implementation for the current release version
- TCP connection management with non-blocking I/O
- Microsoft OAuth authentication flow
- Encrypted credential storage
- Account state tracking
- Connection lifecycle events on the event bus

**Verification:** A configured account authenticates, connects to a Minecraft server, and the event bus emits connection lifecycle events. Disconnection and reconnection are tested.

### Phase 2 — Management

**Goal:** A human can manage multiple accounts through a web interface.

**Deliverables:**
- Dashboard backend with account status API and event streaming
- Dashboard frontend with account overview, status display, and log viewer
- Scheduler with basic task execution
- Proxy manager with SOCKS5 and HTTP support
- Configuration UI (read-only in v1.0)

**Verification:** Multiple accounts connect and are visible in the dashboard. Logs stream to the dashboard. Proxy assignment works and failures are handled gracefully.

### Phase 3 — Plugins

**Goal:** The platform is useful for its primary use case.

**Deliverables:**
- `citadel-api` stabilization (based on feedback from Phases 0-2)
- Map Art plugin (complete workflow: image input, account coordination, block placement)
- Discord integration plugin (status updates, basic commands)
- Minimal world cache (position, block changes, inventory)
- Plugin development guide and API reference documentation
- Stress testing with many simultaneous accounts

**Verification:** Map Art is demonstrated end-to-end. Discord integration works. Documentation exists for third-party plugin developers.

### Phase 4 — Community

**Goal:** Public release with community contribution infrastructure.

**Deliverables:**
- Public GitHub repository with CI
- Contribution guide and issue templates
- Example plugins and plugin templates
- Performance optimization based on real-world usage
- Additional official plugins based on community demand

---

## 10. Architectural Risks

### 10.1 Core Bloat (Highest Risk)

**Risk:** The core gradually absorbs functionality that belongs in plugins. This happens incrementally — "this is small enough for the core" — until the core is a tangled monolith.

**Mitigation:**
- Enforce the rule: if a feature could be a plugin, it must be a plugin.
- Review every API addition for scope creep.
- Maintain a living document that explicitly lists what the core contains and why.

### 10.2 Tight Networking-to-Account Coupling

**Risk:** The networking layer and account management become interdependent, making either difficult to replace or test independently.

**Mitigation:**
- The connection lifecycle is event-driven. The networking layer emits events; account management and plugins consume them.
- Account metadata and connection state are separate concerns. Account management tracks credentials and metadata; networking manages the TCP connection. They communicate through defined interfaces and events.

### 10.3 Plugin Isolation Failure

**Risk:** One plugin crashes another or reads its internal state.

**Mitigation:**
- Each plugin loads in its own classloader (standard Java practice, not custom infrastructure).
- Exceptions in event handlers are caught and logged. A failing handler does not propagate the failure.
- Plugin API methods receive copies or immutable views of data where practical.

### 10.4 Memory Bloat at Scale

**Risk:** Per-account state (world cache, event queues, connection buffers) consumes more memory than running real clients would, defeating the project's primary value proposition.

**Mitigation:**
- World cache depth is conservative by default and configurable per account.
- Plugins register interest in specific data rather than receiving all updates.
- Every new per-account data structure is justified against real usage.
- Profile under realistic load before each release.

### 10.5 Authentication Fragility

**Risk:** Microsoft's authentication flow changes, blocking all account connections.

**Mitigation:**
- Auth logic is isolated behind a clear interface in a single package.
- Integration tests against Microsoft's auth endpoints detect changes quickly.
- A manual token injection fallback exists for advanced users.

### 10.6 Event Bus Bottleneck

**Risk:** The event bus becomes a throughput bottleneck as accounts and plugins grow.

**Mitigation:**
- Start with simple type-based async dispatch. This handles v1.0 requirements.
- If profiling reveals a bottleneck, targeted improvements (topic routing, backpressure, batching) are added based on measured need, not speculation.
- Slow subscribers are isolated by default (async dispatch prevents them from blocking publishers).

### 10.7 Minecraft Version Churn

**Risk:** Minecraft updates change protocol details frequently, creating a continuous maintenance burden.

**Mitigation:**
- Target the current major release. Optionally support one previous release.
- Protocol versioning is centralized in a single package, not scattered across the codebase.
- Official plugins document their supported version range.
- Migration guides accompany version changes.

### 10.8 Plugin API Instability

**Risk:** Breaking API changes discourage community plugin development.

**Mitigation:**
- The API is semantically versioned independently from the core.
- Breaking changes are restricted to major versions.
- Deprecated APIs remain functional for at least one minor version cycle.
- The API is designed and reviewed for stability before the first public release.

### 10.9 GPLv3 Plugin Boundary Ambiguity

**Risk:** Uncertainty about whether plugins must also be GPLv3 discourages community contributions.

**Mitigation:**
- The plugin API is explicitly the "interface" to the GPL'd work. Plugins that depend only on the API (not on internal packages) can be distributed under any license. This is documented clearly in the project README and plugin development guide.
- This is an intentional tradeoff. GPLv3 protects the core from proprietary appropriation while enabling a permissive plugin ecosystem.

---

## 11. Future Vision

### Year 1 — Foundation

v1.0 is stable and usable. Map Art is a complete workflow: load an image, assign accounts, execute the build. The Discord plugin keeps administrators informed. The dashboard provides full visibility. The first community plugins begin appearing.

### Year 2 — Ecosystem

The plugin ecosystem has grown. The API has iterated to v2 with improvements learned from real use. Performance has been optimized based on production deployments. The project has multiple core maintainers and a documented governance process.

### Year 3+ — Maturity

Clustering support enables multiple Citadel instances to coordinate. Plugin hot-reload and a scripting API lower the barrier for non-Java contributors. The project is recognized as the standard open-source platform for Minecraft account automation — not the only option, but the one that values clean architecture, developer experience, and long-term maintainability over rapid feature accumulation.

### The North Star

A server administrator should download Citadel, install the Map Art plugin, point it at an image and a server, and have a map art built within minutes — without ever touching code.

A plugin developer should be able to create a new automation tool by writing a single file against the plugin API — without understanding the Minecraft protocol, authentication flows, or network concurrency.

These two outcomes define Citadel's success.
