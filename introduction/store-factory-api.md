# Store Factory API

A `StoreFactory` selects **where session data lives** and **which EventStore distributes events**.\
Storage and event synchronization are **independent**, so you can freely combine backends.

***

### MemoryStoreFactory

#### `MemoryStoreFactory()`

Creates a factory using **in-memory storage** and the default **MemoryEventStore**.\
Use for **single-node** or **no-distribution** setups.

```java
StoreFactory sf = new MemoryStoreFactory();
// in-memory storage + local only events
```

***

#### `MemoryStoreFactory(EventStore eventStore)`

Creates a factory using **in-memory storage** and a **custom EventStore** for distributed sync.

```java
EventStore es = new KafkaEventStore(...);
StoreFactory sf = new MemoryStoreFactory(es);
// local storage + distributed events (Kafka)
```

***

### HazelcastStoreFactory

#### `HazelcastStoreFactory(HazelcastInstance hazelcastClient)`

Creates a factory using **Hazelcast storage** and the default **HazelcastEventStore**.

```java
HazelcastInstance hz = Hazelcast.newHazelcastInstance();
StoreFactory sf = new HazelcastStoreFactory(hz);
// storage + events both via Hazelcast
```

***

#### `HazelcastStoreFactory(HazelcastInstance hazelcastClient, EventStore eventStore)`

Creates a factory using **Hazelcast storage** and a **custom EventStore** backend.

```java
HazelcastInstance hz = Hazelcast.newHazelcastInstance();
EventStore es = new RedisStreamEventStore(...);
StoreFactory sf = new HazelcastStoreFactory(hz, es);
// Hazelcast for storage, Redis Streams for events
```

***

### RedissonStoreFactory

#### `RedissonStoreFactory(RedissonClient redissonClient)`

Creates a factory using **Redis storage** and the default **RedissonEventStore** (Redis Pub/Sub).

```java
RedissonClient redis = Redisson.create();
StoreFactory sf = new RedissonStoreFactory(redis);
// Redis storage + Redis Pub/Sub events
```

***

#### `RedissonStoreFactory(RedissonClient redissonClient, EventStore eventStore)`

Creates a factory using **Redis storage** and a **custom EventStore**.

```java
RedissonClient redis = Redisson.create();
EventStore es = new NatsEventStore(...);
StoreFactory sf = new RedissonStoreFactory(redis, es);
// Redis maps + NATS for events
```

***

## Mixing storage and event backends

| Storage (`StoreFactory`) | Event (`EventStore`) | Example                                                         |
| ------------------------ | -------------------- | --------------------------------------------------------------- |
| Memory                   | Kafka                | `new MemoryStoreFactory(new KafkaEventStore(...))`              |
| Redis                    | NATS                 | `new RedissonStoreFactory(redis, new NatsEventStore(...))`      |
| Hazelcast                | Redis Streams        | `new HazelcastStoreFactory(hz, new RedisStreamEventStore(...))` |
| Memory                   | Memory               | `new MemoryStoreFactory()`                                      |
| Hazelcast                | Kafka                | `new HazelcastStoreFactory(hz, new KafkaEventStore(...))`       |

***

## Minimal server usage

```java
Configuration cfg = new Configuration();
// other configs
cfg.setStoreFactory(new HazelcastStoreFactory(hz, new KafkaEventStore(...)));
SocketIOServer server = new SocketIOServer(cfg);
server.start();
```

***

## Concept summary

> **`StoreFactory` decides where per-session data lives.**\
> &#xNAN;**`EventStore` decides how events propagate across servers.**\
> Both are **configurable and swappable**, without changing application code.
