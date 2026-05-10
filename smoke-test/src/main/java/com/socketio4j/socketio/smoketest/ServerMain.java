/**
 * Copyright (c) 2025 The Socketio4j Project
 * Parent project : Copyright (c) 2012-2025 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.socketio4j.socketio.smoketest;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.socketio4j.socketio.Configuration;
import com.socketio4j.socketio.SocketIOServer;
import com.socketio4j.socketio.listener.DataListener;
import com.socketio4j.socketio.store.event.EventStoreMode;
import com.socketio4j.socketio.store.redis_pubsub.RedisPubSubEventStore;
import com.socketio4j.socketio.store.redis_pubsub.RedisStoreFactory;

/**
 * SocketIO Server for smoke testing.
 */
public class ServerMain {
    public static final int DEFAULT_PORT = 8899;

    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);

    private SocketIOServer server;
    private RedissonClient redisson;
    private final SystemInfo systemInfo = new SystemInfo();

    public void start(int port) throws Exception {
        start(port, SmokeTestMode.STANDALONE, null, "node");
    }

    public void start(int port, SmokeTestMode mode, String redisUrl, String nodeName) throws Exception {
        systemInfo.printSystemInfo();
        log.info("Starting SocketIO server {} with port: {} and mode: {}", nodeName, port, mode);

        Configuration serverConfig = new Configuration();
        serverConfig.setHostname("127.0.0.1");
        serverConfig.setPort(port);
        if (mode == SmokeTestMode.DISTRIBUTED) {
            configureRedisStore(serverConfig, redisUrl);
        }
        server = new SocketIOServer(serverConfig);
        setupEventListeners();

        server.start();
        log.info("SocketIO server started at port: {}", port);
    }

    private void setupEventListeners() {
        // Echo listener - echoes back all received messages
        server.addEventListener("echo", String.class, new DataListener<String>() {
            @Override
            public void onData(com.socketio4j.socketio.SocketIOClient client, String data,
                               com.socketio4j.socketio.AckRequest ackRequest) throws Exception {
                client.sendEvent("echo-response", data);
            }
        });
        server.addEventListener("join-room", String.class, (client, room, ackRequest) -> {
            client.joinRoom(room);
            client.sendEvent("join-ok", room);
        });
        server.addEventListener("relay", String.class, (client, data, ackRequest) -> {
            int separator = data.indexOf(':');
            if (separator <= 0 || separator == data.length() - 1) {
                return;
            }
            String targetRoom = data.substring(0, separator);
            String payload = data.substring(separator + 1);
            server.getRoomOperations(targetRoom).sendEvent("relay-response", payload);
        });
    }

    private void configureRedisStore(Configuration serverConfig, String redisUrl) {
        if (redisUrl == null || redisUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Distributed smoke test requires redisUrl, for example redis://127.0.0.1:6379");
        }

        Config config = new Config();
        config.useSingleServer().setAddress(redisUrl);
        redisson = Redisson.create(config);
        serverConfig.setStoreFactory(new RedisStoreFactory(
                redisson,
                new RedisPubSubEventStore.Builder(redisson)
                        .eventStoreMode(EventStoreMode.MULTI_CHANNEL)
                        .build()
        ));
    }

    public void stop() {
        if (server != null) {
            log.info("Stopping SocketIO server...");
            server.stop();
        }
        if (redisson != null) {
            redisson.shutdown();
        }
    }

    public static void main(String[] args) {
        ServerMain server = new ServerMain();
        CountDownLatch stopLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            stopLatch.countDown();
        }, "smoke-server-shutdown"));

        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
            SmokeTestMode mode = args.length > 1 ? SmokeTestMode.from(args[1]) : SmokeTestMode.STANDALONE;
            String redisUrl = args.length > 2 ? args[2] : null;
            String nodeName = args.length > 3 ? args[3] : "node";
            server.start(port, mode, redisUrl, nodeName);
            stopLatch.await();
        } catch (Exception e) {
            log.error("SocketIO smoke test server failed", e);
            System.exit(1);
        }
    }
}
