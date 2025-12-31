---
icon: h
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

# Hazelcast Store

The **HazelcastStore** is a distributed implementation of the `Store` interface backed by Hazelcast’s `IMap`.\
Each session receives its own dedicated distributed map keyed by its `sessionId`, enabling session data to be shared across nodes in a Hazelcast cluster. This makes the store suitable for **horizontally scaled deployments**, where per-session metadata must remain consistent across multiple JVM instances.

**Key characteristics**

* **Distributed per-session storage** — session data is stored in Hazelcast’s partitioned, replicated map
* **Cluster-wide visibility** — values can be accessed and modified from any node in the Hazelcast cluster
* **Resilient to node failure** — depends on Hazelcast configuration for partition redundancy
* **No external broker required** — ideal when Hazelcast is already used for clustering or data grids
* **Composable with any EventStore** — event distribution backend may differ (e.g. Kafka, Redis Streams, etc.)

**How it works**

* `set(key, val)` updates the distributed map entry for this session
* `get(key)` retrieves the value from the cluster
* `has(key)` checks key existence
* `del(key)` removes a session key from distributed storage
* `destroy()` removes the entire IMap for the session, releasing all associated data

**Advantages**

👍 Transparent data sharing across the cluster\
👍 Works naturally with Hazelcast-backed adapters or distributed state systems\
👍 Supports hybrid deployments when paired with external EventStores\
👍 Provides higher durability and resiliency compared to pure in-memory storage\
(e.g. _HazelcastStore + KafkaEventStore_, _HazelcastStore + RedisStreamEventStore_)

**Limitations**

❌ Storage durability is tied to Hazelcast configuration (replicas, persistence, etc.)\
❌ Higher latency than local memory due to distributed access\
❌ Session cleanup requires calling `destroy()` — unused session maps may remain if not cleaned\
❌ Not suitable when Redis or another system is the authoritative source of session state

***

#### Summary

> **HazelcastStore provides distributed, session-scoped storage backed by Hazelcast IMap,**\
> **making per-session metadata visible and resilient across nodes in a clustered deployment.**
