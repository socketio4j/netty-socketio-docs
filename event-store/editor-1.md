---
icon: r
layout:
  width: default
  title:
    visible: true
  description:
    visible: true
  tableOfContents:
    visible: true
  outline:
    visible: true
  pagination:
    visible: true
  metadata:
    visible: true
---

# Redis Reliable Pub/Sub

{% hint style="info" %}
SocketIO4J uses the Redisson client to connect to a Redis server. Alternative Redis-compatible backends such as Valkey and DragonflyDB may also be supported.
{% endhint %}

The **RedissonPubSubReliableEventStore** provides a distributed, _replay-capable_ event store for socketio4j using **Redis Reliable Topics backed by Redis Streams**.\
It supports multi-node synchronization with optional stream trimming to control memory footprint, enabling durability across server restarts while still delivering events in near real time.

> Redis Reliable Pub/Sub uses Streams internally for durability, but does not expose offsets; Redis Streams exposes offsets directly, enabling controlled replay and connection-state recovery feature(planned for development).

**Key characteristics**

* **Reliable delivery via Redis Streams** — retains recent messages for controlled replay
* **Stream-backed topics** — `RReliableTopic` ensures messages are persisted and delivered to late subscribers
* **Optional auto-trimming** — prevents unbounded stream growth using scheduled `XTRIM`
* **Asynchronous dispatch** — delivery callbacks occur outside Netty event loops
* **Duplicate prevention** — filters self-originated events via `nodeId`

**How it works**

* `publish0` writes events to a **reliable topic**, which internally appends to a Redis stream
* `subscribe0` registers a listener on the reliable topic; events are pulled from the underlying stream even after reconnect
* Stream entries are periodically trimmed based on `streamMaxLength` and `trimEvery`
* Trimming is performed asynchronously in a daemon thread to avoid blocking I/O
* Stream metadata (`activePubTopics`, `activeSubTopics`, `trimTopics`) is tracked per event type

**Modes**

| Mode             | Behavior                                      | When to use                                                     |
| ---------------- | --------------------------------------------- | --------------------------------------------------------------- |
| `MULTI_CHANNEL`  | Per-event-type streams + reliable topics      | Default; isolates traffic and maintains ordering per event type |
| `SINGLE_CHANNEL` | All events routed to a single reliable stream | When strict global ordering is required                         |

**Advantages**

👍 Messages persisted in Redis stream — recoverable up to trim limits\
👍 Suitable for **multi-node synchronization with durability**\
👍 Optional trimming prevents unbounded memory use\
👍 Can replay recent messages after reconnects (within trimmed range)\
👍 Familiar Redis deployment model, no Kafka/NATS needed

**Limitations**

❌ **Not fully exactly-once** — duplicates may occur after restart or reconnections\
❌ **Ordering is per-stream** — cross-event ordering requires single-channel mode\
❌ Requires Redis Streams support (Redis 5+)\
❌ Reliability depends on stream retention configuration and trimming strategy

> **Delivery guarantee:** _At-least-once semantics — listeners must be idempotent to handle potential duplicate deliveries._
