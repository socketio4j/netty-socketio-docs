# Store Factory

The **StoreFactory** defines how socketio4j creates and manages _session data stores_ and _shared maps_, while also exposing an **EventStore** used for distributed synchronization. Unlike traditional coupled designs, **storage and event propagation are decoupled**, allowing applications to freely combine a data-store backend with any compatible EventStore implementation.

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

Storage Backend (StoreFactory)Event Backend (EventStore)ValidExample Deployment

Memory

Kafka

✔️

local session data + global broadcast events

Redis

NATS

✔️

Redis maps + low-latency pubsub

Hazelcast

Redis Streams

✔️

Hazelcast clustering + Redis durability

Memory

Memory

✔️

single-node, no distribution

Hazelcast

Kafka

✔️

Hazelcast session replication + Kafka synchronization

> **Design summary:** _StoreFactory chooses **where per-session metadata lives**_ _EventStore chooses **how events are distributed across servers**_ **Both can be swapped independently.**

**Advantages**

👍 Allows hybrid deployments and gradual migration between backends 👍 Enables choosing the best storage and event infrastructure independently 👍 Avoids coupling distributed state with distributed event delivery 👍 Backwards compatible with single-node or clustered setups
