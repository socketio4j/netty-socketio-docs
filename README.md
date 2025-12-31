---
icon: object-ungroup
---

# Namespace

## Namespaces

Namespaces provide logical separation of features and event handling within the SocketIO server.\
They allow different parts of an application to operate independently using a **single physical connection**.

{% hint style="info" %}
**Namespaces do not create separate socket connections.**\
A client connects once and can join multiple namespaces over the same underlying WebSocket/TCP session.
{% endhint %}

***

### Default Namespace

If a client connects without specifying a namespace, it is attached to the default namespace (`""`).

```java
Configuration config = new Configuration();
config.setPort(9092);

SocketIOServer server = new SocketIOServer(config);
// default namespace exists implicitly
server.start();
```

### Custom Namespace

Custom namespaces separate application concerns and event scopes.

```java
SocketIOServer server = new SocketIOServer(config);

Namespace chat = server.addNamespace("/chat");
Namespace admin = server.addNamespace("/auth");

server.start();
```

Each namespace defines:

* its own event listeners
* its own connection lifecycle
* its own authorization logic
* its own broadcast operations

***

### Public Namespace Example (`/chat`)

The `/chat` namespace is open and allows general messaging without authentication.

#### Server

```java
Configuration config = new Configuration();
config.setPort(9092);

SocketIOServer server = new SocketIOServer(config);
Namespace chat = server.addNamespace("/chat");

// new client connection
chat.addConnectListener(client -> {
    System.out.println("[/chat] connected -> " + client.getSessionId());
});

// message event
chat.addEventListener("message", String.class, (client, data, ack) -> {
    // broadcast to all clients in /chat
    chat.getBroadcastOperations().sendEvent("message", data);
});

server.start();
```

#### Client

```javascript
const chat = io("http://localhost:9092/chat");

chat.on("connect", () => console.log("connected to /chat"));
chat.emit("message", "hello everyone");
```

***

### Authenticated Namespace Example (`/auth`)

The `/auth` namespace restricts access using authorization logic executed during connection.

#### Server

```java
Configuration config = new Configuration();
config.setPort(9092);

SocketIOServer server = new SocketIOServer(config);
Namespace admin = server.addNamespace("/auth");

// verify token on namespace connect
admin.setAuthorizationListener(data -> {
    String token = data.getSingleUrlParam("token");
    return "secret123".equals(token);
});

// privileged alert event
admin.addEventListener("alert", String.class, (client, data, ack) -> {
    admin.getBroadcastOperations().sendEvent("alert", data);
});

server.start();
```

#### Authorized Client

```javascript
const auth = io("http://localhost:9092/auth", {
    query: { token: "secret123" }
});

admin.emit("alert", "restart service");
```

#### Unauthorized Client

```javascript
io("http://localhost:9092/auth", {
    query: { token: "invalid" }
});
```

Expected server output:

```
[/auth] authorization failed -> connection rejected
```

***
