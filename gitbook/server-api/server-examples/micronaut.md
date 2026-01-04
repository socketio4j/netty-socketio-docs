---
icon: m
---

# Micronaut

{% tabs %}
{% tab title="application.properties" %}
```properties
netty-socket-io.hostname=localhost
netty-socket-io.port=9092
netty-socket-io.ping-timeout=60000
netty-socket-io.ping-interval=25000
```
{% endtab %}

{% tab title="application.yml" %}
```yml
netty-socket-io:
  hostname: localhost
  port: 9092
  ping-timeout: 60000
  ping-interval: 25000

```
{% endtab %}
{% endtabs %}

## Main Application

```
@Singleton
public class MicronautMainApplication {
    
    @Inject
    SocketIOServer server;
    
    @EventListener
    void onStartup(ServerStartupEvent event) {
        // Add event listeners
        server.addEventListener("chatevent", ChatMessage.class, (client, data, ackRequest) -> {
            server.getBroadcastOperations().sendEvent("chatevent", data);
        });
        
        server.start();
        log.info("Socket.IO server started on port {}", server.getConfiguration().getPort());
    }
}

```

## Event Handlers

```
@Singleton
public class ChatEventHandler {
    
    @Inject
    SocketIOServer server;
    
    @OnConnect
    public void onConnect(SocketIOClient client) {
        log.info("Client connected: {}", client.getSessionId());
    }
    
    @OnEvent("chatevent")
    public void onChatEvent(SocketIOClient client, ChatMessage data) {
        server.getBroadcastOperations().sendEvent("chatevent", data);
    }
}

```

## Full Example

See the complete example in the [netty-socketio-examples-micronaut-base](https://github.com/socketio4j/netty-socketio/tree/main/netty-socketio-examples/netty-socketio-examples-micronaut-base) module.

<br>
