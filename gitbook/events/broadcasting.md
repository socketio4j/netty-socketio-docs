---
icon: bullhorn
---

# Broadcasting

Broadcasting means sending an event to **multiple/all clients at once**.

#### Broadcast to all connected clients

```java
server.getBroadcastOperations()
      .sendEvent("news", "Hello everyone!");
```

This sends the `"news"` event to all active clients.

#### Broadcasting within a namespace

If you’re using namespaces (custom logical channel), get the namespace first:

```java
server.getNamespace("/chat")
      .getBroadcastOperations()
      .sendEvent("globalMessage", payload);
```

Namespaces partition clients; clients must connect to that namespace to receive those events.
