---
icon: image-landscape
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
    - https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/basics/images-and-media
---

# Hazelcast Pub/Sub

The **HazelcastEventStore** provides a distributed event store for socketio4j using **Hazelcast ITopic**.\
It enables horizontal scaling by broadcasting events across cluster nodes so that room joins, leaves, state changes, acknowledgements, and other internal events are synchronized between multiple server instances.

**Key characteristics**

* **Distributed event broadcasting** — all nodes receive all published events
* **Async non-blocking dispatch** — Hazelcast manages delivery outside Netty event loops
* **Topic-per-event support** — allows multi-channel separation of event types
* **Straightforward integration** — uses core Hazelcast features without ring buffer configuration
* **Duplicate prevention** — events from the same node are skipped using `nodeId` filtering

**How it works**

* Events are published into Hazelcast ITopic instances (one or many depending on mode)
* Each subscribed server registers Hazelcast message listeners for event delivery
* Local listeners receive only externally originated events (`nodeId` mismatch)
* Listener registrations are tracked per event type to enable controlled unsubscribe and shutdown

**Modes**

| Mode             | Behavior                                        | When to use                         |
| ---------------- | ----------------------------------------------- | ----------------------------------- |
| `MULTI_CHANNEL`  | Each event type maps to its own Hazelcast topic | Default; isolates event traffic     |
| `SINGLE_CHANNEL` | All events routed to `ALL_SINGLE_CHANNEL`       | When cross-type ordering is desired |

**Advantages**

👍 Works in multi-node deployments\
👍 No extra brokers required if Hazelcast is already used for clustering\
👍 Easy to configure and integrate\
👍 Clean unsubscribe and shutdown handling\
👍 Ideal for applications already using Hazelcast for distributed state

**Limitations**

❌ No message persistence beyond Hazelcast topic buffering — messages may be dropped on restart\
❌ Ordering is per-topic, not global across all event types\
❌ No historical replay — listeners receive only messages after subscription\
❌ Duplicate delivery possible during failover events

> **Delivery guarantee:** _At-least-once semantics — event listeners must be idempotent to safely handle duplicate messages._
