---
icon: paper-plane-top
---

# Emitting events

#### Server → Client

You **emit events from the server** using `sendEvent()`.

```java
client.sendEvent("message", "Hello from server");
```

* **client**: `SocketIOClient` instance.
* **"message"**: event name.
* **payload**: object that will be serialized to the client (JSON allowed).

Emitting with **multiple objects**:

```java
client.sendEvent("update", user, metadata);
```

You can emit from anywhere you hold a `SocketIOClient`, for example inside a listener.

#### Client → Server

From the client (JavaScript):

```js
socket.emit("chat", { text:"Hi world" });
```
