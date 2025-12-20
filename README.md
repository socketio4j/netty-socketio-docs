---
icon: gear
---

# Server Configuration


## Network & Binding

| Property   | Type     | Default      | Description                                        |
| ---------- | -------- | ------------ | -------------------------------------------------- |
| `hostname` | `String` | `null`       | Bind address. If unset, binds to `0.0.0.0` / `::0` |
| `port`     | `int`    | `-1`         | Server port (must be set)                          |
| `context`  | `String` | `/socket.io` | Socket.IO context path                             |
|            |          |              |                                                    |

```java
config.setHostname("0.0.0.0");
config.setPort(9092);
config.setContext("/socket.io");
```

## Threading Model

| Property        | Type  | Default | Notes                          |
| --------------- | ----- | ------- | ------------------------------ |
| `bossThreads`   | `int` | `0`     | `CPU * 2 when value sets to 0` |
| `workerThreads` | `int` | `0`     | `CPU * 2 when value sets to 0` |

```java
config.setBossThreads(2);
config.setWorkerThreads(16);
```

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

## Heartbeat & Timeouts

| Property           | Default    | Description                     |
| ------------------ | ---------- | ------------------------------- |
| `pingInterval`     | `25000 ms` | Ping interval                   |
| `pingTimeout`      | `60000 ms` | Ping timeout (`0` disables)     |
| `firstDataTimeout` | `5000 ms`  | Prevents silent channel attacks |

```java
config.setPingInterval(20000);
config.setPingTimeout(60000);
config.setFirstDataTimeout(5000);
```

## Payload & Frame Limits

| Property                | Default | Description              |
| ----------------------- | ------- | ------------------------ |
| `maxHttpContentLength`  | `64 KB` | Max HTTP request size    |
| `maxFramePayloadLength` | `64 KB` | Max WebSocket frame size |

```java
config.setMaxHttpContentLength(256 * 1024);
config.setMaxFramePayloadLength(256 * 1024);
```

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

## Compression

| Property               | Default | Description          |
| ---------------------- | ------- | -------------------- |
| `httpCompression`      | `true`  | GZIP / Deflate       |
| `websocketCompression` | `true`  | `permessage-deflate` |

```java
config.setHttpCompression(true);
config.setWebsocketCompression(true);
```

## Buffer & ACK Handling

| Property             | Default             | Description              |
| -------------------- | ------------------- | ------------------------ |
| `preferDirectBuffer` | `true`              | Use Netty direct buffers |
| `ackMode`            | `AUTO_SUCCESS_ONLY` | Auto-ACK behavior        |

```java
config.setPreferDirectBuffer(true);
config.setAckMode(AckMode.AUTO);
```

## Session & Security

| Property         | Default | Description               |
| ---------------- | ------- | ------------------------- |
| `randomSession`  | `false` | Randomize session IDs     |
| `needClientAuth` | `false` | TLS client authentication |

```java
config.setRandomSession(true);
config.setNeedClientAuth(true);
```

## JSON Serialization

| Property      | Default       | Description              |
| ------------- | ------------- | ------------------------ |
| `jsonSupport` | Auto-detected | Jackson-based by default |

```java
config.setJsonSupport(new JacksonJsonSupport());
```

## Authorization

| Property                | Default   | Description             |
| ----------------------- | --------- | ----------------------- |
| `authorizationListener` | Allow all | Handshake authorization |

```java
config.setAuthorizationListener(data -> {
    return AuthorizationResult.SUCCESS;
});
```

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

## Store / Clustering

| Store                   | Use Case      |
| ----------------------- | ------------- |
| `MemoryStoreFactory`    | Single-node   |
| `RedissonStoreFactory`  | Redis cluster |
| `HazelcastStoreFactory` | Hazelcast     |

```java
config.setStoreFactory(new RedissonStoreFactory(redissonClient));
```

## SSL / TLS

```java
SocketSslConfig ssl = new SocketSslConfig();
ssl.setKeyStore("keystore.jks");
ssl.setKeyStorePassword("changeit");
config.setSocketSslConfig(ssl);
```

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

## Full Minimal Example

```java
Configuration config = new Configuration();
config.setPort(9092);
config.setTransports(Transport.WEBSOCKET);
config.setStoreFactory(new MemoryStoreFactory());
```


{% hint style="info" %}
📌 Tip: Configuration is cloned internally for immutability. Treat it as write-once before server start.
{% endhint %}
