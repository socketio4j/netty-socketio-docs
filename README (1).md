---
icon: hand-wave
layout:
  width: default
  title:
    visible: true
  description:
    visible: false
  tableOfContents:
    visible: true
  outline:
    visible: true
  pagination:
    visible: true
  metadata:
    visible: true
metaLinks:
  alternates:
    - https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/
---

# Introduction

## Store

The **Store** interface defines a per-session key-value storage abstraction for socketio4j.\
It allows transports, namespaces, and user code to persist small pieces of session-scoped metadata such as user IDs, authentication tokens, connection state, or room membership hints—independent of the actual backing storage implementation.

**Key characteristics**

* **Session-scoped storage** — one store instance exists per connected client session
* **Key-value semantic** — arbitrary objects associated with string keys
* **Backend-agnostic** — implementations may use memory, Hazelcast, Redis, or other data stores
* **Lifecycle-aware** — destroyed when the underlying client disconnects
* **Type-safe retrieval** — returned values can be cast or generically typed

**How it works**

* `set` associates a value with a key for the lifetime of the session
* `get` returns a stored value, or `null` if not present
* `has` checks for key existence without loading the value
* `del` removes a single key-value entry
* `destroy` removes all entries, invalidating the store instance

**Usage scenarios**

| Case                        | Example                                             |
| --------------------------- | --------------------------------------------------- |
| Authentication              | store `"userId"`, `"tenant"`, `"tokenClaims"`       |
| Reconnection hints          | store `"rooms"` or custom metadata                  |
| Custom handshake parameters | persist user metadata from upgrade/handshake        |
| Namespaced logic            | attach state needed only during the current session |

**Advantages**

👍 Works uniformly across clustered and standalone modes\
👍 Keeps session metadata isolated to each connection\
👍 Allows switching storage backend without user code changes\
👍 Supports lightweight in-memory operation for single-node deployments

**Limitations**

❌ Not intended for large objects or binary storage\
❌ Not a distributed data model by itself — distribution depends on implementation\
❌ No built-in TTL or expiration beyond session lifecycle\
❌ Not shared across sessions unless backed by shared storage

***

#### Backend behavior

| Backend                           | Persistence                                     | Visibility              | Characteristics                        |
| --------------------------------- | ----------------------------------------------- | ----------------------- | -------------------------------------- |
| **In-memory**                     | Ephemeral, cleared on disconnect or JVM restart | Local only              | Fastest, best for single node          |
| **Hazelcast / Redisson / others** | Distributed (based on backend config)           | Accessible across nodes | Recommended for multi-node deployments |

***

#### Lifecycle guarantee

> **A Store instance lives for exactly one client session and is destroyed when the session ends.**\
> After calling `destroy()`, the store must not be accessed again.

## EventStore

The **EventStore** interface defines the abstraction for socketio4j’s _distributed event synchronization layer_.\
It provides a uniform API for publishing, subscribing, and propagating socketio4j internal events such as room joins, leaves, acknowledgements, and node-to-node synchronization messages across one or more server instances.

Concrete implementations (Kafka, Redis Streams, Hazelcast, NATS, etc.) supply transport-specific behavior, while the interface standardizes event flow, error handling, and lifecycle semantics.

**Key characteristics**

* **Unified event API** — consistent publish/subscribe model for all backends
* **Typed event dispatch** — listeners receive strongly typed `EventMessage` objects
* **Node-aware filtering** — implementations typically ignore self-originating messages using `nodeId`
* **Pluggable backends** — supports streaming systems, pub/sub, or in-memory fallback
* **Lifecycle management** — publish, subscribe, unsubscribe, and shutdown operations are standardized

**How it works**

* `publish` wraps backend publishing and ensures errors are logged and propagated
* `subscribe` registers event listeners for a given event type
* `unsubscribe` deregisters listeners and cleans up backend state
* `shutdown` terminates backend resources and closes connections where applicable
* Implementations supply the actual logic through `publish0`, `subscribe0`, `unsubscribe0`, and `shutdown0`

**Event routing models**

| Property         | Meaning                                                                                   |
| ---------------- | ----------------------------------------------------------------------------------------- |
| `EventStoreMode` | Determines whether events are multiplexed (`MULTI_CHANNEL`) or unified (`SINGLE_CHANNEL`) |
| `EventStoreType` | Identifies transport family (`STREAM`, `PUBSUB`, `LOCAL`, etc.)                           |
| `PublishMode`    | Abstracts reliability: `RELIABLE` or `UNRELIABLE` depending on backend guarantees         |
| `nodeId`         | Uniquely identifies a node; used to avoid delivering locally-originated events twice      |

> **Note:** `getNodeId()` generates a random node ID by default.\
> Distributed setups should override it to provide a stable node identity.

**Advantages**

👍 Abstract interface unifies multiple event backends\
👍 Enables drop-in replacement of distributed transports\
👍 Centralized error logging and failure transparency\
👍 Clear extension points for custom event stores

**Limitations**

❌ Does not enforce delivery semantics — guarantees depend on implementation\
❌ No built-in persistence, ordering, or deduplication — handled per backend\
❌ Local filtering behavior (`nodeId`) must be respected by stores to avoid duplication

***

#### Delivery Guarantees

> **The EventStore interface does not define reliability or ordering semantics.**\
> **Delivery guarantees depend entirely on the concrete implementation.**

## StoreFactory

The **StoreFactory** defines how socketio4j creates and manages _session data stores_ and _shared maps_, while also exposing an **EventStore** used for distributed synchronization.\
Unlike traditional coupled designs, **storage and event propagation are decoupled**, allowing applications to freely combine a data-store backend with any compatible EventStore implementation.

This means you can run:

* **Memory storage + Kafka event propagation**
* **Redis storage + NATS event propagation**
* **Hazelcast storage + Redis Streams event propagation**
* **Memory storage + no distribution (MemoryEventStore)**
* …or any other combination your deployment requires.

**Key characteristics**

* **Per-session Store creation** — stores metadata scoped to a single client connection
* **Shared map creation** — provides named maps usable by namespaces, adapters, and plugins
* **EventStore exposure** — supplies an event synchronization mechanism, which may be independent of the storage backend
* **Composition-friendly design** — storage and event propagation do _not_ have to come from the same backend
* **Configurable runtime behavior** — users can plug in a different EventStore without replacing the entire storage layer

**How it works**

* `createStore(sessionId)` → creates a session-specific Store instance tied to the selected storage backend
* `createMap(name)` → returns a named map for sharing data between sessions or namespaces
* `eventStore()` → returns the EventStore implementation associated with this factory (user-defined or default)
* `init(...)` → prepares both storage and event mechanisms before the server starts
* `shutdown()` → cleans up all allocated resources

**Mixing and matching components**

| Storage Backend (StoreFactory) | Event Backend (EventStore) | Valid | Example Deployment                                    |
| ------------------------------ | -------------------------- | ----- | ----------------------------------------------------- |
| Memory                         | Kafka                      | ✔️    | local session data + global broadcast events          |
| Redis                          | NATS                       | ✔️    | Redis maps + low-latency pubsub                       |
| Hazelcast                      | Redis Streams              | ✔️    | Hazelcast clustering + Redis durability               |
| Memory                         | Memory                     | ✔️    | single-node, no distribution                          |
| Hazelcast                      | Kafka                      | ✔️    | Hazelcast session replication + Kafka synchronization |

> **Design summary:**\
> &#xNAN;_&#x53;toreFactory chooses **where per-session metadata lives**_\
> &#xNAN;_&#x45;ventStore chooses **how events are distributed across servers**_\
> **Both can be swapped independently.**

**Advantages**

👍 Allows hybrid deployments and gradual migration between backends\
👍 Enables choosing the best storage and event infrastructure independently\
👍 Avoids coupling distributed state with distributed event delivery\
👍 Backwards compatible with single-node or clustered setups
