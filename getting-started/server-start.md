---
icon: server
---

# Server Start

## Creating a Server

To start a Socket.IO server, create a `Configuration`, set the bind address and port, and then start the server.

```java
Configuration config = new Configuration();
config.setHostname("localhost");
config.setPort(9092);

SocketIOServer server = new SocketIOServer(config);
server.start();
```

{% hint style="warning" %}
When using frameworks such as **Spring Boot**, ensure you import `com.socketio4j.socketio.Configuration` and not framework classes with the same name (for example, `org.springframework.context.annotation.Configuration`), to avoid import conflicts.
{% endhint %}

## Stopping the Server

Always stop the server gracefully during shutdown.

{% code lineNumbers="true" %}
```java
server.stop();
```
{% endcode %}

## Notes

* `hostname` is optional. If not set, the server binds to all interfaces (`0.0.0.0` / `::0`).
* `port` **must** be set before starting the server.
* Threading, transports, and other advanced options can be customized via `Configuration` before calling `start()`.
