# Event Store

The **EventStore** interface defines socketio4j’s distributed event synchronization layer.\
It provides a unified abstraction for publishing and subscribing to internal events — such as room joins, leaves, acknowledgements, session changes, and node-to-node synchronization — across one or more server instances.

**Key characteristics**

* **Unified event API** — same publish/subscribe model for all backends
* **Typed dispatch** — listeners receive strongly typed `EventMessage` objects
* **Node-aware filtering** — implementations typically ignore self-originated events via `nodeId`
* **Pluggable transports** — Kafka, Redis Streams, Hazelcast, NATS, in-memory fallback, etc.
* **Lifecycle control** — standardized `publish`, `subscribe`, `unsubscribe`, `shutdown`

**How it works**

* `publish` → backend-specific send via `publish0`
* `subscribe` → register event listener via `subscribe0`
* `unsubscribe` → remove listener and cleanup via `unsubscribe0`
* `shutdown` → release backend resources via `shutdown0`
* Implementations supply transport behavior through the `*0` methods; the interface standardizes flow and error handling

**Event routing properties**

| Property           | Meaning                                                                          |
| ------------------ | -------------------------------------------------------------------------------- |
| **EventStoreMode** | `MULTI_CHANNEL` (per-event streams/topics) or `SINGLE_CHANNEL` (unified channel) |
| **EventStoreType** | Declares transport family: `STREAM`, `PUBSUB`, `LOCAL`, etc.                     |
| **PublishMode**    | Describes expected reliability: `RELIABLE` or `UNRELIABLE`                       |
| **nodeId**         | Identifies the node; used to avoid reprocessing local events                     |

> **Note:** `getNodeId()` provides a random ID by default.\
> For distributed deployments, configure a **stable node identity** via `StoreFactory`.

**Advantages**

👍 Swap distributed transports without code changes\
👍 Centralized error logging and flow semantics\
👍 Clear extension hooks for custom backends\
👍 Abstracts away transport differences

**Limitations**

ℹ️ Delivery guarantees depend on implementation — not enforced by the interface\
ℹ️ Persistence, ordering, deduplication are backend responsibilities\
ℹ️ `nodeId`-based local filtering must be respected to avoid duplicates

***

#### Delivery Guarantees

> The **EventStore interface does not define delivery guarantees**.\
> Reliability, replay, and ordering depend entirely on the chosen backend (e.g., Redis Streams = at-least-once; Redis Pub/Sub = best-effort; Kafka = at-least-once; Memory = local only).
