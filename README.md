# Citadel

A protocol-level automation platform for Minecraft Java Edition.

Citadel manages many Minecraft accounts simultaneously from a single
centralized process, eliminating the need to run multiple full game clients.
A web dashboard provides management and monitoring. A plugin system allows
the community to extend the platform without modifying its core.

## Modules

| Module | Description |
|--------|-------------|
| `citadel-api` | Public plugin API (pure abstractions, zero internal dependencies) |
| `citadel-core` | Internal implementation (networking, auth, accounts, events, config, logging, scheduling, plugins) |
| `citadel-dashboard` | Dashboard backend (optional HTTP API for management) |
| `citadel-dashboard-ui` | Dashboard frontend (single-page web application) |
| `plugins/map-art` | Map Art automation plugin |
| `plugins/discord` | Discord integration plugin |

## Building

**Prerequisites:** JDK 21 or later

```shell
./gradlew build
```

To run checks without building:

```shell
./gradlew check
```

## License

Citadel is licensed under the GNU General Public License v3.0.
See [LICENSE](LICENSE) for details.
