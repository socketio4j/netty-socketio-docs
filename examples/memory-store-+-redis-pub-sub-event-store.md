# Memory Store + Redis Pub/Sub Event Store

```
import com.socketio4j.socketio.Configuration;
import com.socketio4j.socketio.SocketIOServer;
import com.socketio4j.socketio.store.memory.MemoryStoreFactory;
import com.socketio4j.socketio.store.redis_pubsub.RedisPubSubEventStore;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class Application {

    private SocketIOServer server;

    public void start() {
        // --- Redis event propagation setup ---
        Config redissonCfg = new Config();
        redissonCfg.useSingleServer()
                .setAddress("redis://127.0.0.1:6379"); // DragonflyDB / Valkey also works

        RedissonClient redisson = Redisson.create(redissonCfg);

        // event store → redis pub/sub
        RedisPubSubEventStore eventStore =
                new RedisPubSubEventStore(redisson, redisson, null, null);

        // store factory → memory session store + Redis event propagation
        MemoryStoreFactory storeFactory = new MemoryStoreFactory(eventStore);

        // --- Socket.IO Server config ---
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(8080);
        config.setStoreFactory(storeFactory);

        server = new SocketIOServer(config);

        // optional: listeners
        server.addConnectListener(client -> {
            log.info("Client connected: {}", client.getSessionId());
        });

        server.addDisconnectListener(client -> {
            log.info("Client disconnected: {}", client.getSessionId());
        });

        server.start();
        log.info("Socket.IO server started on port 8080");
    }

    public void stop() {
        if (server != null) {
            log.info("Stopping server...");
            server.stop();
            server.getConfiguration().getStoreFactory().shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        Application app = new Application();
        app.start();
        log.info("Press Enter to stop");
        System.in.read();
        app.stop();
    }
}
```
