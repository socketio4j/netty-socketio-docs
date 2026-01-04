---
icon: person-shelter
---

# Rooms

**Rooms** are groups of clients that can be targeted specifically.

#### Add a client to a room

```java
client.joinRoom("room42");
```

#### Remove a client from a room

```java
client.leaveRoom("room42");
```

Clients automatically leave all rooms on disconnect.

#### Broadcast to a room

Get the operations for a room then emit:

```java
BroadcastOperations room = server.getRoomOperations("room42");
room.sendEvent("roomEvent", data);
```

This sends `roomEvent` only to clients currently in `"room42"`.

#### Rooms per Namespace

Rooms live within a namespace:

```java
server.getNamespace("/chat")
      .getRoomOperations("room42")
      .sendEvent("msg", msg);
```

#### List clients in a room

```java
Collection<SocketIOClient> clientsInRoom =
    server.getRoomOperations("room42").getClients();
```

This gives you the current snapshot of clients in that room.
