# Event Store

{% hint style="info" %}
You don’t need to deal with the internals of `Store` or `EventStore`.\
Their implementations are fully abstracted — you **only choose and control behavior through `StoreFactory`configuration**.
{% endhint %}

The **EventStore** interface defines the abstraction for socketio4j’s _distributed event synchronization layer_. It provides a uniform API for publishing, subscribing, and propagating socketio4j internal events such as room joins, leaves, acknowledgements, and node-to-node synchronization messages across one or more server instances.

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

PropertyMeaning

`EventStoreMode`

Determines whether events are multiplexed (`MULTI_CHANNEL`) or unified (`SINGLE_CHANNEL`)

`EventStoreType`

Identifies transport family (`STREAM`, `PUBSUB`, `LOCAL`, etc.)

`PublishMode`

Abstracts reliability: `RELIABLE` or `UNRELIABLE` depending on backend guarantees

`nodeId`

Uniquely identifies a node; used to avoid delivering locally-originated events twice

> **Note:** `getNodeId()` generates a random node ID by default. Distributed setups should override it to provide a stable node identity.

**Advantages**

👍 Abstract interface unifies multiple event backends 👍 Enables drop-in replacement of distributed transports 👍 Centralized error logging and failure transparency 👍 Clear extension points for custom event stores

**Limitations**

❌ Does not enforce delivery semantics — guarantees depend on implementation ❌ No built-in persistence, ordering, or deduplication — handled per backend ❌ Local filtering behavior (`nodeId`) must be respected by stores to avoid duplication

***

**Delivery Guarantees**

> **The EventStore interface does not define reliability or ordering semantics.** **Delivery guarantees depend entirely on the concrete implementation.**
