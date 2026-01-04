---
icon: memory
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
    - https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/getting-started/quickstart
---

# Memory Store

The **MemoryStore** is an in-memory implementation of the `Store` interface that provides per-session key-value storage backed by a thread-safe `ConcurrentHashMap`.\
It is lightweight, fast, and suitable for **single-node deployments**, development environments, and scenarios where session data does **not** need to be shared across multiple server instances.

**Key characteristics**

* **Local per-session storage** — each connection receives its own store instance
* **Thread-safe access** — supports concurrent reads/writes using `ConcurrentHashMap`
* **Ephemeral data** — values exist only in JVM memory and are lost on restart or failure
* **No cross-node sharing** — data is not distributed between servers
* **Zero external dependencies** — simplest possible `Store` implementation

**How it works**

* `set(key, value)` stores a value in memory scoped to the session
* `get(key)` retrieves the stored value, or `null` if none exists
* `has(key)` checks key presence without reading the value
* `del(key)` removes a single entry
* `destroy()` clears all entries, typically called when the client disconnects

**Advantages**

👍 Fast, low-latency access\
👍 No external services required\
👍 Ideal for development and local testing\
👍 Works well with _any_ `EventStore` for hybrid deployments\
(e.g., _MemoryStore + KafkaEventStore_ or _MemoryStore + RedisStreamEventStore_)

**Limitations**

ℹ️ Not distributed — values are not synchronized across nodes\
ℹ️ Not persistent — data vanishes on server restart or crash\
ℹ️ Not suitable for horizontal scaling unless combined with a distributed `StoreFactory` implementation

***

#### Summary

> **MemoryStore provides local, ephemeral per-session data storage, ideal for single-node setups and development.**\
> **Can be combined with any distributed EventStore when distribution of events is required without distributed storage.**
