---
icon: money-bill-trend-up
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
    - >-
      https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/getting-started/publish-your-docs
---

# Stock market app

## Server

```java
import com.socketio4j.socketio.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Stock Market Server (room-based)
 *
 * Each stockId is a room.
 * Clients subscribe to a stock to join that room.
 * Periodic price updates are sent only to subscribers.
 *
 * Example stocks: AAPL, GOOG, TSLA, MSFT
 */
public class StockMarketServer {
    public static void main(String[] args) {
        Configuration cfg = new Configuration();
        cfg.setHostname("localhost");
        cfg.setPort(9092);

        SocketIOServer server = new SocketIOServer(cfg);

        // subscribe to stock & join stock room
        server.addEventListener("stock:subscribe", String.class, (client, stockId, ack) -> {
            client.joinRoom(stockId);
            log.info("Client subscribed to => {}", stockId);
        });

        server.start();
        log.info("Stock Market Server running on :9092");

        // simulated real-time prices
        String[] stocks = { "AAPL", "GOOG", "TSLA", "MSFT" };
        Random random = new Random();
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

        exec.scheduleAtFixedRate(() -> {
            for (String s : stocks) {
                double price = 100 + random.nextDouble() * 50;
                server.getRoomOperations(s).sendEvent("stock:price", s, price);
            }
        }, 0, 1, TimeUnit.SECONDS);
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
  <title>Stock Market Live</title>
  <style>
    body { font-family: sans-serif; margin: 20px; }
    #prices { border:1px solid #ccc; padding:10px; height:250px; overflow-y:auto; }
  </style>
</head>

<body>

  <h2>📈 Live Stock Market Feed</h2>
  
  <label>Stock Symbol</label><br>
  <input id="stockId" placeholder="TSLA or AAPL or MSFT"><br><br>

  <button onclick="subscribeStock()">Subscribe</button>

  <hr>

  <div id="status">Not connected</div>

  <div id="prices"></div>

  <!-- socket.io client -->
  <script src="https://cdn.socket.io/4.7.5/socket.io.min.js"></script>

  <script>
    let socket;
    let subscribed = false;

    function subscribeStock() {
      const stockId = document.getElementById("stockId").value.trim().toUpperCase();
      if (!stockId) {
        alert("Enter a stock symbol");
        return;
      }

      socket = io("http://localhost:9092");

      socket.on("connect", () => {
        document.getElementById("status").innerText = `Connected - subscribed to ${stockId}`;
        
        // join stock room
        socket.emit("stock:subscribe", stockId);
        subscribed = true;
      });

      socket.on("stock:price", (stock, price) => {
        appendPrice(`${stock}: ${price.toFixed(2)}`);
      });

      socket.on("disconnect", () => {
        document.getElementById("status").innerText = "Disconnected";
      });
    }

    function appendPrice(line) {
      const div = document.getElementById("prices");
      div.innerHTML += `<div>${new Date().toLocaleTimeString()} — ${line}</div>`;
      div.scrollTop = div.scrollHeight;
    }
  </script>
</body>
</html>

```
