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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketIoServerMain {
    private static final Logger log = LoggerFactory.getLogger(SocketIoServerMain.class);
    public static void main(String[] args) throws Exception {
        // Create configuration
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(9092);

        // Create server
        SocketIOServer server = new SocketIOServer(config);
        
        server.addConnectListener(client -> {
            log.info("[/] connected -> " + client.getSessionId());
        });
        server.addDisconnectListener(client -> {
            log.info("[/] disconnected -> " + client.getSessionId());
        });
        
        server.addEventListener("hi", String.class, (client, data, ack) -> {
            //listen to "reply" in client
            log.info("received data : " + data);
            client.sendEvent("reply", "hello");
        });
        // Start server
        server.start();
        log.info("Socket.IO server started on port 9092");

        // Keep JVM alive
        Thread.currentThread().join();
    }
}

```
{% endcode %}

{% hint style="info" %}
Check [Events](https://app.gitbook.com/o/shMwc485bv7qtDWf0s0D/s/mZhTHTqTlv7AiApMtIxm/) for event handling related documentation&#x20;
{% endhint %}

## Notes

* `hostname` is optional. If not set, the server binds to all interfaces (`0.0.0.0` / `::0`).
* `port` **must** be set before starting the server.
* Threading, transports, and other advanced options can be customized via `Configuration` before calling `start()`.
