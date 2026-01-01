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
    - https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/basics/editor
---

# Redis Pub/Sub

{% hint style="info" %}
SocketIO4J uses the Redisson client to connect to a Redis server. Alternative Redis-compatible backends such as Valkey and DragonflyDB may also be supported.
{% endhint %}

The **RedissonPubSubEventStore** provides an event store for socketio4j using **Redis Pub/Sub**.\
It enables basic event forwarding between server instances by broadcasting messages across Redis channels, allowing distributed room operations and internal synchronization — but without durability or replay guarantees.

**Key characteristics**

* **Ephemeral broadcast messaging** — events are delivered only to currently connected subscribers
* **Async listener callbacks** — Redis Pub/Sub dispatch runs outside Netty event loops
* **Lightweight multi-node sync** — minimal configuration when Redis is already present
* **Subject-per-event routing** — event types map directly to Pub/Sub channels
* **Duplicate prevention** — filters events originating from the same node (`nodeId`)

**How it works**

* `publish0` sends the event to a Redis Pub/Sub channel based on the event type
* `subscribe0` registers a type-safe listener on the corresponding channel
* Delivery is **push-only** — subscribers must be online to receive messages
* On unsubscribe or shutdown, topic listeners are removed but Redis clients remain open

**Modes**

| Mode             | Behavior                                   | When to use                                      |
| ---------------- | ------------------------------------------ | ------------------------------------------------ |
| `MULTI_CHANNEL`  | Each event maps to its own Pub/Sub channel | Default; reduces event cross-talk                |
| `SINGLE_CHANNEL` | All events routed to `ALL_SINGLE_CHANNEL`  | When full ordering of all event types is desired |

**Advantages**

👍 Extremely simple to configure\
👍 Zero persistence overhead — pure broadcast semantics\
👍 Ideal for Redis-centric deployments or prototypes\
👍 Works out-of-the-box for basic multi-node communication

**Limitations**

❌ Events are **not persisted** — subscribers miss events while disconnected\
❌ No replay mechanism — reconnecting nodes start from “now”\
❌ Duplicate messages possible during reconnection\
❌ Ordering only applies per-channel; global ordering requires `SINGLE_CHANNEL` mode

> **Delivery guarantee:** At-most-once semantics. _Best-effort, ephemeral delivery — listeners must tolerate message loss and out-of-order delivery._
