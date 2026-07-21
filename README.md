# Citadel

[![Build](https://github.com/qtaura/citadel/actions/workflows/build.yml/badge.svg)](https://github.com/qtaura/citadel/actions/workflows/build.yml)

A protocol-level automation platform for Minecraft Java Edition.

Citadel manages many Minecraft accounts simultaneously from a single
centralized process, eliminating the need to run multiple full game clients.
A web dashboard provides management and monitoring. A plugin system allows
the community to extend the platform without modifying its core.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full design document and
[ROADMAP.md](ROADMAP.md) for the implementation plan.

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

**Prerequisites:** JDK 21 or later (the Gradle wrapper handles the rest)

```shell
./gradlew build
```

Run checks (formatting, static analysis) without full build:

```shell
./gradlew check spotlessCheck
```

## Development

This repository uses a Git Flow-inspired branching model:

- `main` — production-ready releases
- `develop` — integration branch for completed milestones
- `feature/*` — feature branches based on `develop`

All commits follow [Conventional Commits](https://www.conventionalcommits.org/).
Code formatting is enforced by Spotless (googleJavaFormat).

## License

Citadel is licensed under the GNU General Public License v3.0.
See [LICENSE](LICENSE) for details.
