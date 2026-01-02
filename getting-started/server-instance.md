---
icon: server
---

# Server Instance

## Creating a Server

To start a Socket.IO server, create a `Configuration`, set the bind address and port, and then start the server.

```java
Configuration config = new Configuration();
config.setHostname("localhost");
config.setPort(9092);

SocketIOServer server = new SocketIOServer(config);
```

{% hint style="warning" %}
When using frameworks such as **Spring Boot**, ensure you import `com.socketio4j.socketio.Configuration` and not framework classes with the same name (for example, `org.springframework.context.annotation.Configuration`), to avoid import conflicts.
{% endhint %}

## Starting the Server

{% tabs %}
{% tab title="Sync" %}
```java
server.start()
```
{% endtab %}

{% tab title="Async" %}
```java
server.startAsync().addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("Server started on " + config.getPort());
            } else {
                System.out.println("Error " + future.cause().getLocalizedMessage());
            }
        });
```
{% endtab %}
{% endtabs %}



## Stopping the Server

Always stop the server gracefully during shutdown.

```java
server.stop();
```

## Complete Example

{% code title="Server.java" %}
```java
import com.socketio4j.socketio.Configuration;
import com.socketio4j.socketio.SocketIOServer;

public class SocketIoServerMain {

    public static void main(String[] args) throws Exception {

        // Create configuration
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(9092);

        // Create server
        SocketIOServer server = new SocketIOServer(config);

        // Start server
        server.start();
        System.out.println("Socket.IO server started on port 9092");

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping Socket.IO server...");
            server.stop();
        }));

        // Keep JVM alive
        Thread.currentThread().join();
    }
}

```
{% endcode %}

{% hint style="info" %}
Check [Events](https://app.gitbook.com/o/shMwc485bv7qtDWf0s0D/s/mZhTHTqTlv7AiApMtIxm/ "mention")for event handling related documentation&#x20;
{% endhint %}

## Notes

* `hostname` is optional. If not set, the server binds to all interfaces (`0.0.0.0` / `::0`).
* `port` **must** be set before starting the server.
* Threading, transports, and other advanced options can be customized via `Configuration` before calling `start()`.
