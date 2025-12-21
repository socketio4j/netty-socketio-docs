---
icon: gear
---

# Server Configuration

## Full Minimal Example

```java
Configuration config = new Configuration();
config.setHostname("127.0.0.1");
config.setPort(9092);
config.setContext("/socket.io");
config.setTransports(Transport.POLLING, Transport.WEBSOCKET);
config.setStoreFactory(new RedissonStoreFactory(redissonClient));
```

{% hint style="info" %}
Defaults work for most use cases; additional overrides are listed below.
{% endhint %}

{% hint style="warning" %}
For connection auth check [here](./#authorization).
{% endhint %}

## Network & Binding

| Property   | Type     | Default      | Description                                        |
| ---------- | -------- | ------------ | -------------------------------------------------- |
| `hostname` | `String` | `null`       | Bind address. If unset, binds to `0.0.0.0` / `::0` |
| `port`     | `int`    | `-1`         | Server port (must be set)                          |
| `context`  | `String` | `/socket.io` | Socket.IO context path                             |

```java
config.setHostname("0.0.0.0");
config.setPort(9092);
config.setContext("/socket.io");
```



***

## Threading Model

| Property        | Type  | Default | Notes                          |
| --------------- | ----- | ------- | ------------------------------ |
| `bossThreads`   | `int` | `0`     | `CPU * 2 when value sets to 0` |
| `workerThreads` | `int` | `0`     | `CPU * 2 when value sets to 0` |

```java
config.setBossThreads(2);
config.setWorkerThreads(16);
```

{% hint style="info" %}
**Boss vs Worker threads**\
Boss threads accept connections; worker threads handle all I/O.\
Add boss threads **only** to increase **connection accept rate**; use **1 for most cases**.\
Scale worker threads for throughput—too many cause context switching.
{% endhint %}



***

## Transport Configuration

| Property         | Type              | Default              | Description                                            |
| ---------------- | ----------------- | -------------------- | ------------------------------------------------------ |
| `transports`     | `List<Transport>` | `WEBSOCKET, POLLING` | Enabled transports                                     |
| `transportType`  | `TransportType`   | `AUTO`               | Native IO selection (EPOLL / KQUEUE / IO\_URING / NIO) |
| `upgradeTimeout` | `int (ms)`        | `10000`              | Polling → WebSocket upgrade timeout                    |

```java
config.setTransports(Transport.WEBSOCKET);
config.setTransportType(TransportType.EPOLL);
```

{% hint style="info" %}
**In AUTO transport mode,** socketio4j automatically selects the best available transport at startup in the following order: **IO\_URING → EPOLL → KQUEUE → NIO**.

If the selected transport is not available on the current platform, socketio4j **safely falls back to NIO** without failing startup.
{% endhint %}



***

## Heartbeat & Timeouts

| Property           | Default    | Description                     |
| ------------------ | ---------- | ------------------------------- |
| `pingInterval`     | `25000 ms` | Ping interval                   |
| `pingTimeout`      | `60000 ms` | Ping timeout (`0` disables)     |
| `firstDataTimeout` | `5000 ms`  | Prevents silent channel attacks |

> ℹ️ **Ping interval vs ping timeout**\
> &#xNAN;**`pingInterval`** defines how often the server sends heartbeat pings to keep the connection alive (NAT keep-alive).\
> &#xNAN;**`pingTimeout`** defines how long the server waits **without a pong** before considering the client disconnected.
>
> In short:\
> **interval = how often to check**,\
> **timeout = how long to wait before giving up**.

```java
config.setPingInterval(20000);
config.setPingTimeout(60000);
config.setFirstDataTimeout(5000);
```

{% hint style="info" %}
**NAT timeout & keep-alive hint**\
`pingInterval` must be **shorter than typical NAT idle timeouts** (usually 30–60s) to keep connections alive behind routers and mobile networks.

Lower values improve NAT survivability and faster dead-peer detection, but **increase network and CPU overhead**.\
Higher values reduce overhead, but risk **silent disconnects** on NATs and load balancers.
{% endhint %}



***

## Payload & Frame Limits

| Property                | Default | Description              |
| ----------------------- | ------- | ------------------------ |
| `maxHttpContentLength`  | `64 KB` | Max HTTP request size    |
| `maxFramePayloadLength` | `64 KB` | Max WebSocket frame size |

```java
config.setMaxHttpContentLength(256 * 1024);
config.setMaxFramePayloadLength(256 * 1024);
```



***

## CORS & HTTP Behavior

| Property              | Default | Description                    |
| --------------------- | ------- | ------------------------------ |
| `enableCors`          | `true`  | Enable CORS                    |
| `origin`              | `null`  | `Access-Control-Allow-Origin`  |
| `allowHeaders`        | `null`  | `Access-Control-Allow-Headers` |
| `addVersionHeader`    | `true`  | Adds `Server` header           |
| `allowCustomRequests` | `false` | Allow non-Socket.IO requests   |

```java
config.setEnableCors(true);
config.setOrigin("https://example.com");
config.setAllowHeaders("Authorization,Content-Type");
```



***

## Compression

| Property               | Default | Description          |
| ---------------------- | ------- | -------------------- |
| `httpCompression`      | `true`  | GZIP / Deflate       |
| `websocketCompression` | `true`  | `permessage-deflate` |

```java
config.setHttpCompression(true);
config.setWebsocketCompression(true);
```



***

## Buffer & ACK Handling

| Property             | Default             | Description              |
| -------------------- | ------------------- | ------------------------ |
| `preferDirectBuffer` | `true`              | Use Netty direct buffers |
| `ackMode`            | `AUTO_SUCCESS_ONLY` | Auto-ACK behavior        |

<pre class="language-java"><code class="lang-java"><strong>config.setPreferDirectBuffer(true);
</strong>config.setAckMode(AckMode.AUTO);
</code></pre>

{% hint style="info" %}
**Ack behavior**

* Acks are sent **at most once** and **only if requested**
* **Manual ack** always suppresses auto-ack
* **`AUTO`** → always auto-acknowledges with `[]` (even on exception)
* **`AUTO_SUCCESS_ONLY`** → auto-acknowledges with `[]` **only on success**
* **`MANUAL`** → developer is fully responsible for sending the ack
{% endhint %}



***

## Session & Security

| Property         | Default | Description               |
| ---------------- | ------- | ------------------------- |
| `randomSession`  | `false` | Randomize session IDs     |
| `needClientAuth` | `false` | TLS client authentication |

```java
config.setRandomSession(true);
config.setNeedClientAuth(true);
```



***

## JSON Serialization

| Property      | Default       | Description              |
| ------------- | ------------- | ------------------------ |
| `jsonSupport` | Auto-detected | Jackson-based by default |

```java
config.setJsonSupport(new JacksonJsonSupport());
```



***

## Authorization

| Property                | Default   | Description             |
| ----------------------- | --------- | ----------------------- |
| `authorizationListener` | Allow all | Handshake authorization |

```java
config.setAuthorizationListener(data -> {
    return AuthorizationResult.SUCCESS;
});
```



***

## Exception Handling

| Property            | Default                    | Description           |
| ------------------- | -------------------------- | --------------------- |
| `exceptionListener` | `DefaultExceptionListener` | Global exception hook |

```java
config.setExceptionListener(new ExceptionListener() {
    @Override
    public void onEventException(Exception e, Object... args) {
        log.error("Socket.IO error", e);
    }
});
```



***

## Store / Clustering

```java
config.setStoreFactory(new RedissonStoreFactory(redissonClient));
```

{% hint style="info" %}
Please check [Adapters](https://app.gitbook.com/o/shMwc485bv7qtDWf0s0D/s/vM0fEesNQnh9fdpchiWm/) Page for detailed explanation.
{% endhint %}



***

## SSL / TLS

```java
SocketSslConfig ssl = new SocketSslConfig();
ssl.setKeyStore("keystore.jks");
ssl.setKeyStorePassword("changeit");
/**
//only uses when mTLS/zero-trust scenarios not usually for wss
ssl.setTrustStore("truststore.jks");
ssl.setTrustStorePassword("changeit");
*/
config.setSocketSslConfig(ssl);
```



***

## HTTP Decoder Tuning

| Property               | Default       |
| ---------------------- | ------------- |
| `maxInitialLineLength` | Netty default |
| `maxHeaderSize`        | Netty default |
| `maxChunkSize`         | Netty default |

```java
HttpRequestDecoderConfiguration http = new HttpRequestDecoderConfiguration();
http.setMaxHeaderSize(16 * 1024);
config.setHttpRequestDecoderConfiguration(http);
```



***

{% hint style="info" %}
📌 Tip: Configuration is cloned internally for immutability. Treat it as write-once before server start.
{% endhint %}
