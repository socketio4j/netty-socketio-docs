---
icon: inbox-in
---

# Listening to events

`netty-socketio` provides listeners to react to incoming events.

#### Built-in connection events

**Connect**

```java
server.addConnectListener(client -> {
    System.out.println("Client connected: " + client.getSessionId());
});
```

Called whenever a client connects.

**Disconnect**

```java
server.addDisconnectListener(client -> {
    System.out.println("Client disconnected: " + client.getSessionId());
});
```

#### Custom event listeners

To listen for custom named events:

```java
server.addEventListener("chat", ChatMessage.class,
    (client, data, ackRequest) -> {
        System.out.println("Chat received: " + data.getText());
});
```

* **"chat"**: event name.
* **ChatMessage.class**: class representing the payload.
* **(client, data, ack)**: handler callback.

If you don’t need typed payloads, use `Object.class` or a generic type.

#### Acknowledgements

You can handle acknowledgements if the client expects a response:

```java
server.addEventListener("ask", Request.class, (client, req, ackSender) -> {
    ackSender.sendAckResponse(new Response("done"));
});
```

> Socket.IO protocol supports ack callbacks on both sides.
