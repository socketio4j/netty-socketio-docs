# Store Factory

The **StoreFactory** defines how socketio4j creates and manages per-session stores and shared maps, while also exposing an **EventStore** for distributed event synchronization.\
Storage and event propagation are **decoupled**, allowing any data-store backend to be paired with any compatible event distribution mechanism.

This enables flexible combinations such as:

* Memory storage + Kafka-based event synchronization
* Redis storage + NATS pub/sub
* Hazelcast storage + Redis Streams
* Memory storage + no distribution (MemoryEventStore)\
  …or any other deployment-specific pairing.

**Key characteristics**

* **Per-session Store creation** — each client connection gets its own data store
* **Shared map creation** — provides named maps for namespaces, adapters, and plugins
* **EventStore exposure** — supplies an event backend, independent of storage choice
* **Composable design** — storage and event propagation can come from different systems
* **Configurable behavior** — swap event distribution without replacing storage

**How it works**

* `createStore(sessionId)` → returns a session-scoped `Store` tied to the storage backend
* `createMap(name)` → returns a named, shared map for inter-session state
* `eventStore()` → returns the configured `EventStore` (default or custom)
* `init(...)` → prepares storage and event synchronization before server startup
* `shutdown()` → cleans up allocated resources during teardown

**Mixing and matching**

| Storage backend (`StoreFactory`) | Event backend (`EventStore`) | Valid | Example deployment                                    |
| -------------------------------- | ---------------------------- | ----- | ----------------------------------------------------- |
| Memory                           | Kafka                        | ✔️    | local session data + global event propagation         |
| Redis                            | NATS                         | ✔️    | Redis maps + low-latency messaging                    |
| Hazelcast                        | Redis Streams                | ✔️    | Hazelcast clustering + durable stream sync            |
| Memory                           | Memory                       | ✔️    | single-node, no distribution                          |
| Hazelcast                        | Kafka                        | ✔️    | Hazelcast session replication + Kafka synchronization |

**Design summary**

* **StoreFactory** decides **where per-session metadata lives**
* **EventStore** decides **how events are distributed across servers**
* Both are **independent and replaceable** at runtime

**Advantages**

👍 Hybrid deployments and gradual migration paths\
👍 Storage and event layers can evolve independently\
👍 Avoids coupling distributed state with event transport\
👍 Works for single-node and clustered setups alike
