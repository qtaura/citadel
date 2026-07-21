# Contributing to Citadel

## Philosophy

Citadel is a protocol-level Minecraft automation platform. Its architecture has been designed before implementation began. The module boundaries, dependency rules, and API contracts are documented in ARCHITECTURE.md. All implementation follows that design.

Contributors should work within the existing design, not redesign it. If the design seems wrong, open an issue before writing code. Design discussions happen before pull requests, not during them.

## Source of Truth

Read both of these before contributing anything:

- **ARCHITECTURE.md** — How the project is structured. Module boundaries, dependency rules, guiding principles, and what belongs where.
- **ROADMAP.md** — What should be implemented and in what order. Every milestone's scope, deliverables, success criteria, and out-of-scope items.

ARCHITECTURE.md is the authoritative design document. ROADMAP.md is the implementation plan. Neither should be changed casually. If you think either needs updating, propose it as a separate discussion or issue before mixing it into implementation work.

## Development Workflow

The repository uses a Git Flow-inspired branching model:

- `main` — Always stable. Production-ready releases only.
- `develop` — Integration branch. Completed milestones are merged here. May be unstable between milestones.
- Feature branches — All work happens here. Branch off `develop`, merge back into `develop`.

Branch naming:

- `feature/<milestone-or-feature-name>` — New features or roadmap milestones.
- `fix/<short-description>` — Bug fixes.
- `docs/<short-description>` — Documentation changes.
- `refactor/<short-description>` — Refactoring (only when necessary; see ROADMAP.md principles).

Examples:

- `feature/milestone-3-bootstrap`
- `feature/event-system`
- `fix/plugin-loader-npe`
- `docs/contributing`
- `refactor/extract-service-registry`

Feature branches must be based on `develop`, not on other feature branches. If two features depend on each other, one should be merged first.

## Commit Messages

Use Conventional Commits. Every commit message must start with a type:

- `feat:` — A new feature (matches a milestone deliverable or a defined scope item).
- `fix:` — A bug fix.
- `refactor:` — Code restructuring without behavior change.
- `docs:` — Documentation changes.
- `build:` — Build system, dependencies, or tooling changes.
- `test:` — Adding or fixing tests.
- `style:` — Formatting, linting, or code style changes (no behavior change).
- `chore:` — Maintenance tasks, gitignore, CI config, etc.

The commit message body should explain why the change was made, not what was changed (the diff shows that). If the commit is small and obvious, the subject line is sufficient.

Keep commits small and focused. A commit should represent one logical change. If you need to say "and" in the subject, split the commit.

## Code Style

Follow the existing project conventions. The codebase uses googleJavaFormat (enforced by Spotless in CI). Run `./gradlew spotlessApply` before committing.

General guidelines:

- Readability over cleverness. Write code that is easy to understand, not code that is short.
- Avoid premature optimization. Profile first, optimize second.
- Write Javadoc for every public API type and method. Internal code needs documentation where its purpose is not obvious from the code itself.
- Minimize dependencies. Every dependency is a maintenance burden. Prefer standard library solutions.
- Prefer interfaces over concrete types for public API. Prefer immutable objects. Prefer composition over inheritance.
- Avoid global mutable state. If you need shared state, make it explicit through dependency injection.

## Architecture Rules

These rules are enforced during code review. Violations will block merging.

### Do Not

- Expose internal implementation through `citadel-api`. The API is the contract between the core and plugins. If it's not in `citadel-api`, plugins must not access it.
- Create circular dependencies between modules. The dependency graph is: `citadel-api` → everything else. `citadel-core` does not depend on `citadel-dashboard`. Plugins depend only on `citadel-api`.
- Bypass the plugin system. If a feature could be a plugin, it must be a plugin. Adding features directly to the core requires strong justification.
- Couple modules unnecessarily. Internal modularity is achieved through packages, not build modules. Keep package boundaries clean.
- Introduce global mutable state. Configuration, service registries, and shared state must be explicitly managed, not hidden in static fields.
- Implement infrastructure for hypothetical future use cases. Build for what is known. Defer speculative generality.

### Prefer

- Interfaces for public contracts.
- Immutable value objects (records, final classes with no setters).
- `Optional` over null returns.
- Unmodifiable collections over mutable ones.
- Dependency injection through service interfaces.
- Composition over inheritance.

## Milestone Workflow

Work follows ROADMAP.md. Each milestone has a defined scope, deliverables, and success criteria.

- Do not implement future milestones before their dependencies are complete. The roadmap order exists for a reason.
- If a milestone's scope is too large to review comfortably, split it into smaller phases. Each phase must still produce working, testable software.
- A milestone is not done until its success criteria are met and CI passes.
- If a milestone reveals missing API or design gaps, fix them within that milestone's scope, not by deferring to a later milestone.

## Pull Requests

Every pull request should include:

- **Purpose** — What problem does this solve? Reference the roadmap milestone if applicable.
- **Summary** — What changed, at a high level. Focus on the approach, not a file-by-file listing.
- **Testing performed** — What tests were run? Manual steps if not automated.
- **Screenshots** — For dashboard UI changes only. Do not include screenshots of code.
- **Breaking changes** — If the PR changes a public API, document exactly what changed and why.
- **Roadmap milestone** — Which milestone this PR implements or supports.

Small PRs are reviewed faster. If a change touches multiple unrelated areas, split it into separate PRs.

## Reviews

Reviewers evaluate:

- Architecture compliance — Does the change follow the rules in ARCHITECTURE.md?
- Maintainability — Will this be easy to change in two years?
- API stability — Does this change the public API? If so, is the change justified and well-documented?
- Readability — Can another contributor understand the code without asking the author?
- Documentation — Are public APIs documented? Is non-obvious internal logic explained?

Reviewers should verify more than whether the code compiles. Every review is an investment in the project's long-term health.

If a reviewer does not understand a change, ask the author to clarify with better naming, documentation, or a simpler approach. Do not approve changes that are hard to understand.

## Testing

- Every new subsystem includes tests covering its public contract.
- Bug fixes include a regression test that reproduces the bug before the fix and passes after it.
- Tests must be deterministic. No test should depend on timing, network access, or external state.
- The build must be reproducible: `./gradlew clean build` produces the same result every time on every machine.
- Prefer unit tests over integration tests. Integration tests go in `tests/`.

---

Citadel is intended to be a long-lived open-source project. We value maintainability, clarity, and thoughtful design over rapid feature development. Code written today will be read and modified by people we have never met. Write it accordingly.
