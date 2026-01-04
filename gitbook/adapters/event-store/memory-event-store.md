---
icon: memory
---

# Memory Event Store

The **MemoryEventStore** is a local, in-process event store used when running a **single socketio4j server instance**.\
All events are handled directly inside the server and **are not shared or replicated across nodes**.\
Because the event lifecycle is already completed locally, **publish and subscribe operations have no effect**, and are intentionally implemented as no-ops.

This store is useful when:

* running only **one server instance**
* developing, debugging, or testing locally
* using it as a **default or fallback** when no distributed adapter is configured

**Not suitable for:**

* multi-server deployments
* horizontal scaling
* distributed messaging or shared event delivery

