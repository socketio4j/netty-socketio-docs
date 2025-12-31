---
icon: 'n'
---

# NATS Pub/Sub

The **NatsEventStore** provides an event store for socketio4j using **NATS Core Pub/Sub**.\
It allows event synchronization across multiple server instances by broadcasting messages, but events are **ephemeral and not persisted**, making it suitable for lightweight, low-latency deployments where durable replay is not required.

**Key characteristics**

* **Low latency broadcast** — NATS Core delivers messages quickly to all connected nodes
* **Ephemeral events** — messages are not stored; subscribers only receive events while online
* **Async and non-blocking** — dispatchers run outside Netty event loops
* **Subject-per-event routing** — event types map directly to subjects
* **Duplicate prevention** — events originating from the same node are skipped via `nodeId`

**How it works**

* `publish0` serializes the event and sends it to a NATS subject
* `subscribe0` registers a dispatcher that listens to the relevant subject
* Messages are delivered to listeners **only if they originated from a different node**
* Unsubscribe cleans up both subscriptions and dispatchers
* Shutdown removes all active subscriptions but does **not** close the shared NATS connection

**Modes**

| Mode             | Behavior                                  | When to use                                          |
| ---------------- | ----------------------------------------- | ---------------------------------------------------- |
| `MULTI_CHANNEL`  | Each event type maps to its own subject   | Default; smaller subjects, better separation         |
| `SINGLE_CHANNEL` | All events routed to `ALL_SINGLE_CHANNEL` | When unified ordering across event types is required |

**Advantages**

👍 Ultra-low latency delivery\
👍 Ideal when events do **not** require durability or replay\
👍 Very lightweight — minimal dependencies and configuration\
👍 Good fit when NATS is already used for messaging

**Limitations**

❌ Events are **not persisted** — missed messages cannot be replayed\
❌ Subscribers must be connected to receive events\
❌ Duplicates may occur during dispatcher recreation or reconnection\
❌ Does not support consumer offsets, backpressure, or stream trimming (JetStream required for that which is currently planned for development)

> **Delivery guarantee:** At-most-once semantics. _Best-effort delivery with possible message loss — event listeners should tolerate missing or out-of-order events._
