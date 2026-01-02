---
icon: bullhorn
---

# Emergency broadcast app

## Server

```java
import com.socketio4j.socketio.Configuration;
import com.socketio4j.socketio.SocketIOServer;
import com.socketio4j.socketio.SocketIOClient;

public class EmergencyServer {
    public static void main(String[] args) {
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(9092);

        SocketIOServer server = new SocketIOServer(config);

        // Event: broadcast emergency messages
        server.addEventListener("emergency", String.class,
            (SocketIOClient client, String message, var ack) -> {
            // print locally
            log.info("EMERGENCY message received: {}", message);

            // send to all clients
            server.getBroadcastOperations().sendEvent("emergency", message);
        });

        server.start();
        log.info("Emergency server started on :9092");
    }
}

```

## Client

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Emergency Alert Client</title>
</head>

<body>
  <h1>🚨 Emergency Alert Listener</h1>
  <p>Status: <span id="status">Connecting...</span></p>

  <!-- Socket.IO client -->
  <script src="https://cdn.socket.io/4.7.5/socket.io.min.js"></script>

  <script>
    const statusEl = document.getElementById("status");
    const socket = io("http://localhost:9092");

    socket.on("connect", () => {
      statusEl.innerText = "Connected";
      console.log("Connected to emergency server");
    });

    // listen for emergency alert broadcasts
    socket.on("emergency", msg => {
      console.log("Emergency alert received:", msg);
      alert("🚨 Emergency: " + msg);
    });

    socket.on("disconnect", () => {
      statusEl.innerText = "Disconnected";
    });
  </script>

</body>
</html>

```
