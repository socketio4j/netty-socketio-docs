---
icon: webhook
---

# Server API

### Server Start & Stop Listeners

Socketio4j allows you to register listeners for server **start** and **stop** events.\
Listeners are executed safely with **exception isolation**, ensuring failures in one listener do not affect others or the server lifecycle.

#### Start Listener

**Add Start Listener**

```java
ServerStartListener startListener = s -> {
    System.out.println(
        "Server started on " + s.getConfiguration().getPort()
    );
};

server.addStartListener(startListener);
```

**Remove Start Listener**

```java
server.removeStartListener(startListener);
```

#### Stop Listener

**Add Stop Listener**

```java
ServerStopListener stopListener = s -> {
    System.out.println("Server stopped");
};

server.addStopListener(stopListener);
```

**Remove Stop Listener**

```java
server.removeStopListener(stopListener);
```

#### Execution Guarantees

* Start listeners are invoked after the server has successfully started
* Stop listeners are invoked during graceful shutdown
* Each listener is executed with **exception isolation**
* Exceptions thrown by listeners do **not** interrupt:
  * Server start or shutdown
  * Other registered listeners

#### Best Practices

* Keep listener logic lightweight and non-blocking
* Avoid long-running or blocking operations inside listeners
* Use listeners for logging, metrics, and coordination only

### Namespaces

#### Default Namespace

The root namespace (`"/"`) is created automatically.

#### Add Namespace

```java
SocketIONamespace chat = server.addNamespace("/chat");
```

#### Get Namespace

```java
SocketIONamespace chat = server.getNamespace("/chat");
```

#### Remove Namespace

```java
server.removeNamespace("/chat");
```

#### Get All Namespaces

```java
Collection<SocketIONamespace> namespaces = server.getAllNamespaces();
```

### Clients

#### Get All Connected Clients (Default Namespace)

```java
Collection<SocketIOClient> clients = server.getAllClients();
```

#### Get Client by Session ID

```java
SocketIOClient client = server.getClient(uuid);
```

Returns `null` if not found.

### Broadcasting

#### Broadcast to All Clients (All Namespaces)

```java
server.getBroadcastOperations()
      .sendEvent("announcement", "Hello everyone");
```

#### Broadcast to Rooms (Across All Namespaces)

```java
server.getRoomOperations("room1", "room2")
      .sendEvent("message", "Hello rooms");
```

### Event Listeners (Default Namespace)

#### Event Listener (Typed)

```java
server.addEventListener("chat", Message.class, (client, data, ack) -> {
    System.out.println(data);
});
```

#### Multi-Type Event Listener

```java
server.addMultiTypeEventListener(
    "event",
    (client, data, ack) -> {},
    String.class,
    ChatMessage.class
);
```

#### Catch-All Event Listener

```java
CatchAllEventListener anyListener = (client, event, args) -> {
    System.out.println("Event: " + event);
};
// add
server.addOnAnyEventListener(anyListener);

// remove
server.removeOnAnyEventListener(anyListener);

```

Aliases:

* `onAny(...)`
* `offAny(...)`

#### Event Interceptor

```java
server.addEventInterceptor((client, event, args) -> {
    return true; // allow
});
```

Used for validation, filtering, or security checks.

#### Remove All Event Listeners

```java
server.removeAllListeners("eventName");
```

### Connection Lifecycle Listeners

#### Connect Listener

```java
server.addConnectListener(client -> {
    System.out.println("Connected: " + client.getSessionId());
});
```

#### Disconnect Listener

```java
server.addDisconnectListener(client -> {
    System.out.println("Disconnected");
});
```

#### Ping / Pong Listeners

```java
server.addPingListener(client -> {});
server.addPongListener(client -> {});
```

Useful for monitoring connection health.

### Listener Auto-Registration

#### Register Listener Object

```java
server.addListeners(new ChatEventHandler());
```

#### Register with Explicit Class

```java
server.addListeners(new ChatEventHandler(), ChatEventHandler.class);
```

Supports annotation-based listeners.

{% hint style="info" %}
Check [Events](https://app.gitbook.com/o/shMwc485bv7qtDWf0s0D/s/mZhTHTqTlv7AiApMtIxm/) for more details
{% endhint %}
