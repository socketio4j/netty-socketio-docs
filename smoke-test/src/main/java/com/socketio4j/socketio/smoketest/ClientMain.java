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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.awaitility.core.ConditionTimeoutException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import static org.awaitility.Awaitility.await;

/**
 * Netty-based Socket.IO load client. It speaks the small Engine.IO/Socket.IO
 * subset needed by the smoke tests and avoids one blocking reader thread per
 * connection, which is required for 10k local connections.
 */
@SuppressWarnings("deprecation")
public class ClientMain {

    private static final Logger log = LoggerFactory.getLogger(ClientMain.class);

    private final List<Channel> channels = new ArrayList<>();
    private final List<String> clientRooms = new ArrayList<>();
    private final ClientMetrics metrics;
    private final AtomicInteger connectedCount = new AtomicInteger(0);
    private final AtomicInteger joinedCount = new AtomicInteger(0);
    private final SystemInfo systemInfo = new SystemInfo();
    private final List<Integer> ports;
    private final int clientCount;
    private final int eachMsgCount;
    private final int eachMsgSize;
    private final SmokeTestMode mode;
    private CountDownLatch connectLatch;
    private CountDownLatch joinLatch;
    private EventLoopGroup group;

    public ClientMain(int port, int clientCount, int eachMsgCount, int eachMsgSize, ClientMetrics metrics) throws Exception {
        this(Arrays.asList(port), clientCount, eachMsgCount, eachMsgSize, SmokeTestMode.STANDALONE, metrics);
    }

    public ClientMain(List<Integer> ports, int clientCount, int eachMsgCount, int eachMsgSize,
                      SmokeTestMode mode, ClientMetrics metrics) throws Exception {
        this.ports = ports;
        this.clientCount = clientCount;
        this.eachMsgCount = eachMsgCount;
        this.eachMsgSize = eachMsgSize;
        this.mode = mode;
        this.metrics = metrics;
        if (mode == SmokeTestMode.DISTRIBUTED && clientCount < 2) {
            throw new IllegalArgumentException("Distributed smoke test requires at least 2 clients");
        }
    }

    public void start() throws Exception {
        systemInfo.printSystemInfo();
        group = new NioEventLoopGroup(Integer.getInteger("smoke.client.eventloop.threads",
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2)));

        try {
            connectClients();
            if (!connectLatch.await(connectTimeoutSeconds(), TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to connect all clients. Connected: " + connectedCount.get() + "/" + clientCount);
            }
            if (mode == SmokeTestMode.DISTRIBUTED && !joinLatch.await(connectTimeoutSeconds(), TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to join all clients to relay rooms. Joined: " + joinedCount.get() + "/" + clientCount);
            }

            log.info("All {} clients connected", clientCount);
            startMessageSending();
        } finally {
            cleanup();
        }
    }

    public ClientMetrics getMetrics() {
        return metrics;
    }

    private void connectClients() throws Exception {
        connectLatch = new CountDownLatch(clientCount);
        joinLatch = new CountDownLatch(mode == SmokeTestMode.DISTRIBUTED ? clientCount : 0);

        for (int i = 0; i < clientCount; i++) {
            final int clientIndex = i;
            String room = "smoke-client-" + i;
            int port = ports.get(i % ports.size());
            URI uri = new URI("ws://127.0.0.1:" + port + "/socket.io/?EIO=4&transport=websocket");
            clientRooms.add(room);

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(8192));
                            ch.pipeline().addLast(new WebSocketClientProtocolHandler(
                                    uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders(), 65536));
                            ch.pipeline().addLast(new SocketIoLoadClientHandler(clientIndex, room));
                        }
                    });

            Channel channel = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();
            channels.add(channel);
            maybePauseConnectBatch(i + 1);
        }
    }

    private void startMessageSending() throws InterruptedException {
        metrics.start();
        String message = generateMessage(eachMsgSize);
        long durationSeconds = Long.getLong("smoke.test.duration.seconds", 0L);
        if (durationSeconds > 0) {
            int roundsPerSecond = Integer.getInteger("smoke.client.rounds.per.second", 1);
            long intervalNanos = roundsPerSecond > 0
                    ? TimeUnit.SECONDS.toNanos(1) / roundsPerSecond
                    : 0;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(durationSeconds);
            long round = 0;
            long nextRound = System.nanoTime();
            while (System.nanoTime() < deadline) {
                sendRound(message);
                round++;
                if (intervalNanos > 0) {
                    nextRound += intervalNanos;
                    sleepUntil(nextRound);
                }
                if (round % progressEvery() == 0) {
                    log.info("Message rounds sent: {}, total messages: {}", round, metrics.getTotalMessagesSent());
                }
            }
        } else {
            for (int round = 0; round < eachMsgCount; round++) {
                sendRound(message);
            }
        }

        long drainTimeoutSeconds = Long.getLong("smoke.client.drain.timeout.seconds", 600L);
        try {
            await().atMost(drainTimeoutSeconds, TimeUnit.SECONDS).until(() ->
                    metrics.getTotalMessagesSent() == metrics.getTotalMessagesReceived()
            );
        } catch (ConditionTimeoutException e) {
            log.warn("Timed out waiting for message responses after {} seconds. Sent: {}, received: {}",
                    drainTimeoutSeconds, metrics.getTotalMessagesSent(), metrics.getTotalMessagesReceived());
        }
        metrics.stop();
        log.info(metrics.toString());
    }

    private void sleepUntil(long deadlineNanos) throws InterruptedException {
        long remaining;
        while ((remaining = deadlineNanos - System.nanoTime()) > 0) {
            TimeUnit.NANOSECONDS.sleep(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(10)));
        }
    }

    private void sendRound(String message) {
        for (int i = 0; i < channels.size(); i++) {
            sendMessage(i, channels.get(i), message);
        }
    }

    private void sendMessage(int clientIndex, Channel channel, String message) {
        if (!channel.isActive()) {
            metrics.recordError();
            return;
        }

        long startTime = System.currentTimeMillis();
        String payload = startTime + ":" + message;
        String frame;
        if (mode == SmokeTestMode.DISTRIBUTED) {
            frame = socketIoEvent("relay", targetRoom(clientIndex) + ":" + payload);
        } else {
            frame = socketIoEvent("echo", payload);
        }
        metrics.recordMessageSent(message.length());
        channel.writeAndFlush(new TextWebSocketFrame(frame));
    }

    private String generateMessage(int size) {
        char[] chars = new char[size];
        Arrays.fill(chars, 'x');
        return new String(chars);
    }

    private String targetRoom(int clientIndex) {
        int targetIndex;
        if (clientIndex % 2 == 0) {
            targetIndex = clientIndex + 1 < clientCount ? clientIndex + 1 : 1;
        } else {
            targetIndex = clientIndex - 1;
        }
        return clientRooms.get(targetIndex);
    }

    private void recordResponse(String data) {
        int separator = data.indexOf(':');
        if (separator <= 0) {
            metrics.recordError();
            return;
        }
        try {
            long startTime = Long.parseLong(data.substring(0, separator));
            metrics.recordLatency(System.currentTimeMillis() - startTime);
            metrics.recordMessageReceived(data.length());
        } catch (NumberFormatException e) {
            metrics.recordError();
        }
    }

    private String socketIoEvent(String event, String data) {
        return "42[\"" + event + "\",\"" + data + "\"]";
    }

    private long connectTimeoutSeconds() {
        return Math.max(60, clientCount / 5);
    }

    private void logProgress(String label, int count) {
        if (count == clientCount || count == 0 || count % progressEvery() == 0) {
            log.info("{}: {}/{}", label, count, clientCount);
        }
    }

    private int progressEvery() {
        return Math.max(1, Integer.getInteger("smoke.client.progress.every", 1000));
    }

    private void maybePauseConnectBatch(int connectedAttempt) throws InterruptedException {
        int batchSize = Integer.getInteger("smoke.client.connect.batch.size", 1000);
        int batchPauseMs = Integer.getInteger("smoke.client.connect.batch.pause.ms", 100);
        if (batchSize > 0 && batchPauseMs > 0 && connectedAttempt % batchSize == 0) {
            Thread.sleep(batchPauseMs);
        }
    }

    private void cleanup() {
        log.info("Cleaning up clients...");
        for (Channel channel : channels) {
            if (channel.isActive()) {
                channel.writeAndFlush(new CloseWebSocketFrame());
            }
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    private class SocketIoLoadClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
        private final int clientIndex;
        private final String room;
        private final AtomicBoolean namespaceConnected = new AtomicBoolean(false);

        private SocketIoLoadClientHandler(int clientIndex, String room) {
            this.clientIndex = clientIndex;
            this.room = room;
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                ctx.writeAndFlush(new TextWebSocketFrame("40"));
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
            if (!(frame instanceof TextWebSocketFrame)) {
                return;
            }

            String text = ((TextWebSocketFrame) frame).text();
            if ("2".equals(text)) {
                ctx.writeAndFlush(new TextWebSocketFrame("3"));
                return;
            }
            if (text.startsWith("40") && namespaceConnected.compareAndSet(false, true)) {
                int count = connectedCount.incrementAndGet();
                logProgress("Connected clients", count);
                connectLatch.countDown();
                if (mode == SmokeTestMode.DISTRIBUTED) {
                    ctx.writeAndFlush(new TextWebSocketFrame(socketIoEvent("join-room", room)));
                }
                return;
            }
            if (text.startsWith("42[\"join-ok\"")) {
                int count = joinedCount.incrementAndGet();
                logProgress("Joined clients", count);
                joinLatch.countDown();
                return;
            }
            if (text.startsWith("42[\"echo-response\"") || text.startsWith("42[\"relay-response\"")) {
                String data = extractSecondString(text);
                if (data == null) {
                    metrics.recordError();
                } else {
                    recordResponse(data);
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!(cause instanceof WebSocketClientHandshakeException)) {
                log.debug("Client {} channel error", clientIndex, cause);
            }
            metrics.recordError();
            ctx.close();
        }

        private String extractSecondString(String text) {
            int firstComma = text.indexOf(',');
            if (firstComma < 0) {
                return null;
            }
            int start = text.indexOf('"', firstComma);
            int end = text.indexOf('"', start + 1);
            if (start < 0 || end < 0) {
                return null;
            }
            return text.substring(start + 1, end);
        }
    }
}
