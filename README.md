---
description: Welcome to your team’s developer platform
layout:
  width: wide
  title:
    visible: false
  description:
    visible: false
  tableOfContents:
    visible: false
  outline:
    visible: false
  pagination:
    visible: false
  metadata:
    visible: true
metaLinks:
  alternates:
    - https://app.gitbook.com/s/2AwfWOGBWBxQmyvHedqW/
---

# Socketio4j Documentation

<h2 align="center">The Socketio4j Project</h2>

<p align="center">Socket.IO server implemented on Java. Realtime java framework</p>



<table data-view="cards"><thead><tr><th></th><th></th><th></th><th data-hidden data-card-target data-type="content-ref"></th><th data-hidden data-card-cover data-type="files"></th></tr></thead><tbody><tr><td><h4><i class="fa-leaf">:leaf:</i></h4></td><td><strong>Getting Started</strong></td><td>Get started with the socketio server in 5 minutes.</td><td><a href="https://app.gitbook.com/o/shMwc485bv7qtDWf0s0D/s/5O7XGb1rb649GUgJpO86/">Installation</a></td><td><a href=".gitbook/assets/no-code.jpg">no-code.jpg</a></td></tr><tr><td><h4><i class="fa-server">:server:</i></h4></td><td><strong>Server API</strong></td><td>Learn more about Socketio Server API</td><td><a href="https://app.gitbook.com/o/shMwc485bv7qtDWf0s0D/s/gDAhA63iEtgGxfDWw39u/">Server API - 4.0 - EN</a></td><td><a href=".gitbook/assets/hosted.jpg">hosted.jpg</a></td></tr><tr><td><h4><i class="fa-terminal">:terminal:</i></h4></td><td><strong>Examples</strong></td><td>Examples to explore socketio server in core java, spring boot, quarkus and micronaut</td><td><a href="https://app.gitbook.com/o/shMwc485bv7qtDWf0s0D/s/nb5yCyMnIBiOSj9Xvef8/">Examples</a></td><td><a href=".gitbook/assets/api-reference.jpg">api-reference.jpg</a></td></tr></tbody></table>

### Get started in 5 minutes

Your first server should be the easiest step. With well-defined endpoints and copy-paste-ready examples, setup is fast and predictable.\
From zero to a working connection in minutes.

<a href="https://app.gitbook.com/o/shMwc485bv7qtDWf0s0D/s/5O7XGb1rb649GUgJpO86/" class="button primary" data-icon="rocket-launch">Get started</a> <a href="https://app.gitbook.com/o/shMwc485bv7qtDWf0s0D/s/gDAhA63iEtgGxfDWw39u/" class="button secondary" data-icon="terminal">API reference</a>

### Server

{% tabs %}
{% tab title="Core Java" %}
{% code title="Server.java" overflow="wrap" expandable="true" %}
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

            // Join room via query param: ?room=room1, verify room membership if needed
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
{% endtabs %}

### Client

{% tabs %}
{% tab title="Java" %}
{% code title="Client.java
" overflow="wrap" expandable="true" %}
```java
//package com.socketio4j.examples.core;

import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;
import java.util.Collections;

public class SocketIoClient {

    public static void main(String[] args) throws URISyntaxException {

        IO.Options options = new IO.Options();
        options.query = "room=room1";
        options.forceNew = true;

        Socket socket = IO.socket("http://localhost:9092", options);

        socket.on(Socket.EVENT_CONNECT, args1 -> {
            System.out.println("Connected: " + socket.id());
            socket.emit("message", "Hello from Java");
        });

        socket.on("message", args1 ->
                System.out.println("Received: " + args1[0])
        );

        socket.connect();
    }
}

```
{% endcode %}
{% endtab %}

{% tab title="node.js" %}
{% code title="client.js" overflow="wrap" expandable="true" %}
```javascript
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
{% endcode %}
{% endtab %}

{% tab title="Dart / Flutter" %}
{% code overflow="wrap" expandable="true" %}
```dart
import 'package:socket_io_client/socket_io_client.dart' as IO;

void main() {
  IO.Socket socket = IO.io(
    'http://localhost:9092',
    IO.OptionBuilder()
        .setQuery({'room': 'room1'})
        .setTransports(['websocket'])
        .build(),
  );

  socket.onConnect((_) {
    print('Connected: ${socket.id}');
    socket.emit('message', 'Hello from Dart');
  });

  socket.on('message', (data) {
    print('Received: $data');
  });
}

```
{% endcode %}
{% endtab %}

{% tab title="Swift (iOS)" %}
{% code overflow="wrap" expandable="true" %}
```swift
import SocketIO

let manager = SocketManager(
    socketURL: URL(string: "http://localhost:9092")!,
    config: [.log(true), .connectParams(["room": "room1"])]
)

let socket = manager.defaultSocket

socket.on(clientEvent: .connect) { _, _ in
    print("Connected:", socket.sid ?? "")
    socket.emit("message", "Hello from Swift")
}

socket.on("message") { data, _ in
    print("Received:", data[0])
}

socket.connect()

```
{% endcode %}
{% endtab %}

{% tab title="Browser - Vanilla JS" %}
{% code overflow="wrap" expandable="true" %}
```html
<script src="https://cdn.socket.io/4.7.5/socket.io.min.js"></script>
<script>
  const socket = io("http://localhost:9092", {
    query: { room: "room1" }
  });

  socket.on("connect", () => {
    console.log("Connected:", socket.id);
    socket.emit("message", "Hello from Browser");
  });

  socket.on("message", (data) => {
    console.log("Received:", data);
  });
</script>

```
{% endcode %}
{% endtab %}

{% tab title="Python" %}
{% code title="client.py" overflow="wrap" expandable="true" %}
```py
import socketio

sio = socketio.Client()

@sio.event
def connect():
    print("Connected:", sio.sid)
    sio.emit("message", "Hello from Python")

@sio.on("message")
def on_message(data):
    print("Received:", data)

sio.connect("http://localhost:9092?room=room1")
sio.wait()

```
{% endcode %}
{% endtab %}

{% tab title="C# (.NET)" %}
{% code overflow="wrap" expandable="true" %}
```csharp
using SocketIOClient;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

class Program
{
    static async Task Main()
    {
        var client = new SocketIO("http://localhost:9092", new SocketIOOptions
        {
            Query = new[] {
                new KeyValuePair<string, string>("room", "room1")
            }
        });

        client.OnConnected += async (sender, e) =>
        {
            Console.WriteLine($"Connected: {client.Id}");
            await client.EmitAsync("message", "Hello from C#");
        };

        client.On("message", response =>
        {
            Console.WriteLine("Received: " + response.GetValue<string>());
        });

        await client.ConnectAsync();
        await Task.Delay(-1);
    }
}

```
{% endcode %}
{% endtab %}

{% tab title="Go" %}
{% code overflow="wrap" expandable="true" %}
```go
package main

import (
	"fmt"
	socketio "github.com/zhouhui8915/go-socket.io-client"
)

func main() {

	opts := &socketio.Options{
		Query: map[string]string{
			"room": "room1",
		},
	}

	client, err := socketio.NewClient("http://localhost:9092", opts)
	if err != nil {
		panic(err)
	}

	client.On("connect", func() {
		fmt.Println("Connected")
		client.Emit("message", "Hello from Go")
	})

	client.On("message", func(msg string) {
		fmt.Println("Received:", msg)
	})

	select {}
}

```
{% endcode %}
{% endtab %}

{% tab title="Rust" %}
{% code overflow="wrap" expandable="true" %}
```rs
use rust_socketio::{ClientBuilder, Payload};

fn main() {
    let client = ClientBuilder::new("http://localhost:9092")
        .query("room", "room1")
        .on("connect", |_, _| {
            println!("Connected");
        })
        .on("message", |payload, _| {
            if let Payload::String(msg) = payload {
                println!("Received: {}", msg);
            }
        })
        .connect()
        .expect("Connection failed");

    client.emit("message", "Hello from Rust").unwrap();
    loop {}
}

```
{% endcode %}
{% endtab %}
{% endtabs %}

<h2 align="center">Join a growing community, built on a foundation trusted by <strong>7,000+ developers</strong></h2>

<p align="center">Join our Discord community or create your first PR in just a few steps.</p>

<table data-card-size="large" data-view="cards"><thead><tr><th></th><th></th><th></th><th></th><th data-hidden data-card-cover data-type="image">Cover image</th></tr></thead><tbody><tr><td><h4><i class="fa-discord">:discord:</i></h4></td><td><strong>Discord community</strong></td><td>Join our Discord community to post questions, get help, and share resources with over 7,000+ like-minded developers.</td><td><a href="https://discord.gg/5TFTQJXR" class="button secondary" data-icon="discord">Join Discord</a></td><td></td></tr><tr><td><h4><i class="fa-github">:github:</i></h4></td><td><strong>GitHub</strong></td><td>Our product is 100% open source and built by developers just like you. Head to our GitHub repository to learn how to submit your first PR.</td><td><a href="https://github.com/socketio4j/netty-socketio/pulls" class="button secondary" data-icon="sandwich">Submit a PR</a></td><td></td></tr></tbody></table>
