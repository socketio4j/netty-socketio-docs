---
icon: sliders
---

# GitBook space variables

Published pages resolve placeholders such as `{$socketio.core.version}` using **Space variables** in the GitBook product (not this Git repo). After each GA release, update those variables so installation snippets match Maven Central.

## Where to edit

In GitBook: open the **socketio4j** space → **Space settings** (or **Site settings**) → **Variables** / **Custom variables** (wording depends on GitBook edition), then set or update the names below.

## Maven code blocks (`{$...}`)

Use the **same GA version** for every row when all artifacts ship on one release tag (typical for `4.0.0`):

| Variable | Example value |
| -------- | ------------- |
| `socketio.core.version` | `4.0.0` |
| `socketio.spring.version` | `4.0.0` |
| `socketio.spring-boot-starter.version` | `4.0.0` |
| `socketio.quarkus.version` | `4.0.0` |
| `socketio.micronaut.version` | `4.0.0` |

## Gradle tabs (`${...}`)

If your space uses separate Gradle variable names (see [Getting Started](README.md)), align them to the same GA:

| Variable (example) | Example value |
| ------------------ | ------------- |
| `socketioCoreVersion` | `4.0.0` |
| `socketioSpringVersion` | `4.0.0` |
| `socketioSpringBootStarterVersion` | `4.0.0` |
| `socketioQuarkusVersion` | `4.0.0` |
| `socketioMicronautVersion` | `4.0.0` |

Exact Gradle variable keys must match what is configured in your GitBook space; duplicate the Maven value unless you intentionally document a split release.

## After changing variables

Republish or sync the space so cached builds pick up new values. Optionally run the smoke test against the same version (see `smoke-test/README.md` in this monorepo).
