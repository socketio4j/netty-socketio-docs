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
metaLinks:
  alternates:
    - >-
      https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/getting-started/publish-your-docs
---

# Redis Store

The **RedisStore** is a Redis-backed implementation of the `Store` interface that provides _per-session, distributed key–value storage_ using Redisson’s `RMap`.\
Each connected client receives a dedicated Redis map identified by its `sessionId`, allowing session data to be accessed consistently across multiple nodes in a clustered environment.

**Key characteristics**

* **Distributed per-session storage** — session data is stored in Redis, not local memory
* **Cross-node visibility** — values are accessible across all servers sharing the same Redis instance
* **Persistent by backend configuration** — data survives JVM/node restarts while Redis is running
* **Dedicated keyspace per session** — each session creates its own `RMap` using the session identifier
* **Pluggable with any EventStore** — can pair with Kafka, Redis Streams, Hazelcast, NATS, etc.

**How it works**

* `set(key, value)` writes a value into a Redis map for this session
* `get(key)` retrieves the value from Redis
* `has(key)` checks key presence in the distributed map
* `del(key)` removes a single key from the map
* `destroy()` deletes the entire session map, typically when the client disconnects

**Advantages**

👍 Session data is available to all nodes — supports horizontal scaling\
👍 Survives server restarts as long as Redis is running\
👍 Aligns naturally with distributed event propagation setups\
👍 Enables mixing storage with any EventStore backend\
(e.g., _RedisStore + KafkaEventStore_, _RedisStore + HazelcastRingBufferEventStore_)



***

#### Summary

> **RedisStore provides distributed, per-session storage backed by Redis,**\
> **making session data visible and persistent across multiple socketio4j nodes.**\
> **Ideal for clustered deployments when session state must be shared.**
