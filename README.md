# Store

{% hint style="info" %}
You don’t need to deal with the internals of `Store` or `EventStore`.\
Their implementations are fully abstracted — you **only choose and control behavior through `StoreFactory`configuration**.
{% endhint %}

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
