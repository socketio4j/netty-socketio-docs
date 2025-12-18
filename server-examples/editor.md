---
icon: java
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

# Core Java

## Java Server

{% code title="Server.java" %}
```java

//package com.socketio4j.examples.core;

import com.socketio4j.socketio.Configuration;
import com.socketio4j.socketio.SocketIOServer;
import com.socketio4j.socketio.SocketIOClient;

public final class BasicServer {

    public static void main(String[] args) {

        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(9092);

        SocketIOServer server = new SocketIOServer(config);

        server.addConnectListener(client -> {
            System.out.println("Connected: " + client.getSessionId());

            // Join room via query param: ?room=room1
            String room = client.getHandshakeData()
                    .getSingleUrlParam("room");

            if (room != null) {
                client.joinRoom(room);
                System.out.println("Joined room: " + room);
            }
        });

        server.addDisconnectListener(client ->
                System.out.println("Disconnected: " + client.getSessionId())
        );

        server.addEventListener(
                "message",
                String.class,
                (SocketIOClient client, String data, var ack) -> {

                    System.out.println("Received: " + data);

                    // Broadcast to all clients
                    server.getBroadcastOperations()
                          .sendEvent("message", data);

                    ack.sendAckData("ok");
                }
        );

        server.start();
        System.out.println("SocketIO4J server started on :9092");

        Runtime.getRuntime().addShutdownHook(
                new Thread(server::stop)
        );
    }
}

```
{% endcode %}

## Client Examples

{% tabs %}
{% tab title="Java" %}
{% code title="Client.java" %}
```java

//package com.socketio4j.examples.core;

import com.socketio4j.socketio.Configuration;
import com.socketio4j.socketio.SocketIOServer;
import com.socketio4j.socketio.SocketIOClient;

public final class BasicServer {

    public static void main(String[] args) {

        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(9092);

        SocketIOServer server = new SocketIOServer(config);

        server.addConnectListener(client -> {
            System.out.println("Connected: " + client.getSessionId());

            // Join room via query param: ?room=room1
            String room = client.getHandshakeData()
                    .getSingleUrlParam("room");

            if (room != null) {
                client.joinRoom(room);
                System.out.println("Joined room: " + room);
            }
        });

        server.addDisconnectListener(client ->
                System.out.println("Disconnected: " + client.getSessionId())
        );

        server.addEventListener(
                "message",
                String.class,
                (SocketIOClient client, String data, var ack) -> {

                    System.out.println("Received: " + data);

                    // Broadcast to all clients
                    server.getBroadcastOperations()
                          .sendEvent("message", data);

                    ack.sendAckData("ok");
                }
        );

        server.start();
        System.out.println("SocketIO4J server started on :9092");

        Runtime.getRuntime().addShutdownHook(
                new Thread(server::stop)
        );
    }
}

```
{% endcode %}
{% endtab %}

{% tab title="Html" %}
{% code title="client.html" %}
```html
<!DOCTYPE html>
<html>
<head>
  <script src="https://cdn.socket.io/4.7.5/socket.io.min.js"></script>
</head>
<body>
<script>
  const socket = io("http://localhost:9092", {
    query: { room: "room1" }
  });

  socket.on("connect", () => {
    console.log("Connected:", socket.id);
    socket.emit("message", "Hello from Browser");
  });

  socket.on("message", (msg) => {
    console.log("Received:", msg);
  });
</script>
</body>
</html>

```
{% endcode %}
{% endtab %}

{% tab title="Node.js" %}
```
import { io } from "socket.io-client";

const socket = io("http://localhost:9092", {
  query: { room: "room1" }
});

socket.on("connect", () => {
  console.log("Connected:", socket.id);
  socket.emit("message", "Hello from Node.js");
});

socket.on("message", (data) => {
  console.log("Received:", data);
});

```
{% endtab %}
{% endtabs %}
