---
icon: puzzle-piece-simple
---

# Example: Putting It Together

##

```java
SocketIOServer server = new SocketIOServer(config);

// connection
server.addConnectListener(client ->
    log.info("Connected: {}", client.getSessionId())
);

// listen for chat
server.addEventListener("chat", ChatPayload.class,
    (client, payload, ack) -> {
        // broadcast to everyone
        server.getBroadcastOperations().sendEvent("chat", payload);
    }
);

// join room
server.addEventListener("join", String.class,
    (client, roomName, ack) -> {
        client.joinRoom(roomName);
    }
);

server.start();
```

***

### References

* [https://socket.io/docs/v4/emitting-events/](https://socket.io/docs/v4/emitting-events/)
