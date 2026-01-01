---
icon: circle
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
    - https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/basics/interactive-blocks
---

# Hazelcast Ring Buffer

The **HazelcastRingBufferEventStore** provides a distributed event store for socketio4j using **Hazelcast ReliableTopic / RingBuffer**.\
It enables horizontal scaling by broadcasting events across cluster nodes so that state changes, joins, leaves, acknowledgements, and other internal events are synchronized across multiple servers.

**Key characteristics**

* **Distributed & replicated** — events are delivered to every active cluster node via Hazelcast
* **Async & non-blocking dispatch** — ReliableTopic handles delivery outside Netty event loops
* **RingBuffer-backed reliability** — recent messages survive node restarts depending on Hazelcast configuration
* **Streaming broadcast semantics** — each node receives all events (not load-balanced)
* **Duplicate prevention** — events originating from the same node are filtered using `nodeId`

**How it works**

* Each publish writes the event into a Hazelcast ReliableTopic (single or per-type depending on mode)
* Subscribed event types attach message listeners to the backing ring buffer
* Messages are delivered to local listeners **only if they originated from a different node**
* Listener registrations are tracked per event type to allow clean unsubscribe and shutdown

**Modes**

| Mode             | Behavior                                   | When to use                                                     |
| ---------------- | ------------------------------------------ | --------------------------------------------------------------- |
| `MULTI_CHANNEL`  | Each event type gets its own ReliableTopic | Default; reduces contention and avoids cross-event interference |
| `SINGLE_CHANNEL` | All events routed to `ALL_SINGLE_CHANNEL`  | When strict ordering across all event types is required         |

**Advantages**

👍 Works in multi-node deployments\
👍 Synchronizes socketio4j servers without external brokers\
👍 Uses Hazelcast primitives for clustering, no Kafka/Redis infrastructure required\
👍 Simple to configure when Hazelcast is already part of the system

**Limitations**

❌ RingBuffer capacity determines how long messages persist — overflow drops oldest data\
❌ No transactional delivery or deduplication for exactly-once semantics\
❌ Delivery ordering is per-partition / per-topic, not global across topics\
❌ Cluster health or partition migrations may cause temporary duplicate delivery

> **Delivery guarantee:** _At-least-once semantics — event listeners should be idempotent to safely handle duplicates._
