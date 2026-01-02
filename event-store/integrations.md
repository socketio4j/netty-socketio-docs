---
icon: list-timeline
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
    - https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/basics/integrations
---

# Kafka/AutoMQ

The **KafkaEventStore** provides a **distributed, streaming event store** for socketio4j using Apache Kafka.\
It enables **horizontal scaling** by broadcasting events across all server instances so that event messages, room joins, leaves, and other internal events are synchronized between nodes.

**Key characteristics**

* **Distributed & fault-tolerant** — events are persisted to Kafka and delivered to every active node
* **Async & non-blocking publishing** — avoids blocking Netty event loops
* **Event replay boundaries** — starts consuming from _newest_ messages only (no history replay)
* **Streaming broadcast semantics** — each node receives **all events**, not load-balanced messages
* **Duplicate prevention** — skips events originating from the same node (`nodeId` filtering)

**How it works**

* Every publish stores the event into a Kafka topic (single or per-type depending on mode)
* Each subscribed event type is polled by a dedicated daemon thread
* Messages are delivered to local listeners only if they **came from a different node**
* Corrupted (“poison pill”) records are skipped to keep consumption alive

**Modes**

| Mode             | Behavior                                  | When to use                                             |
| ---------------- | ----------------------------------------- | ------------------------------------------------------- |
| `MULTI_CHANNEL`  | Each event type gets its own Kafka topic  | Default; parallelism & separation                       |
| `SINGLE_CHANNEL` | All events routed to `ALL_SINGLE_CHANNEL` | When global ordering across all event types is required |

**Advantages**

* 👍 Works in multi-node deployments
* 👍 Synchronizes socketio4j events across servers
* 👍 Safe shutdown & listener cleanup
* 👍 Zero back-pressure on Netty threads

**Limitations**

* ℹ️ Not a point-to-point queue — always broadcast style
* ℹ️ No historical replay — consumes from latest offsets only
* ℹ️ Requires Kafka cluster availability

> **Delivery guarantee:** _At-least-once semantics — duplicate deliveries possible; listeners should be idempotent._
