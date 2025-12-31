---
icon: markdown
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
metaLinks:
  alternates:
    - https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/basics/markdown
---

# Redis Stream

The **RedisStreamEventStore** provides a distributed, streaming event store for socketio4j using **Redis Streams (XADD / XREAD)**.\
It enables horizontal scaling by synchronizing events across multiple server instances so that internal state, room membership, and other distributed operations remain consistent across nodes.

> Redis Reliable Pub/Sub uses Streams internally for durability, but does not expose offsets; Redis Streams exposes offsets directly, enabling controlled replay and connection-state recovery feature(planned for development).

**Key characteristics**

* **Distributed streaming** — uses Redis Streams to deliver events to all active nodes
* **Event replay continuity** — stores recent events in stream history, allowing controlled replay
* **Offset tracking per-event-type** — each event type maintains its own last-seen `StreamMessageId`
* **Streaming dispatch** — events processed asynchronously without blocking Netty event loops
* **Duplicate prevention** — events produced by the same node are filtered with `nodeId`

**How it works**

* Every publish performs `XADD` to a Redis stream (single or per-type depending on mode)
* Each event type maintains its own read offset using `StreamMessageId`
* Polling is performed in dedicated scheduled executor threads per event type
* For each read (`XREAD`), only messages with offsets newer than the last processed id are handled
* Events are delivered to listeners only if they **came from another node**
* On errors or stream timeouts, retries are scheduled automatically

**Modes**

| Mode             | Behavior                                 | When to use                                                      |
| ---------------- | ---------------------------------------- | ---------------------------------------------------------------- |
| `MULTI_CHANNEL`  | Separate Redis stream per event type     | Default; isolates event traffic and reduces contention           |
| `SINGLE_CHANNEL` | All events fed into `ALL_SINGLE_CHANNEL` | When global ordering of all event types is required across nodes |

**Advantages**

👍 Works well for multi-node deployments\
👍 Messages can survive temporary subscriber downtime depending on stream length\
👍 Provides **offset-based delivery**\
👍 Supports controlled replay of new events after restarts (connection state recovery feature is in development, for now you will the get the message offset)\
👍 Uses core Redis Streams primitives without external brokers

**Limitations**

❌ Consumers process messages independently — ordering is per-stream, not global\
❌ Potential duplicate delivery on restarts or offset boundary races\
❌ Requires Redis Streams support (not compatible with legacy Redis Pub/Sub mode)

> **Delivery guarantee:** _At-least-once semantics — event listeners must be idempotent to handle possible duplicate messages._
