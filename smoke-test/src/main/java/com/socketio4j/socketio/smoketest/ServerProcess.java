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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerProcess {

    private final String name;
    private final int port;
    private final int jmxPort;
    private final SmokeTestMode mode;
    private final String redisUrl;
    private final String version;
    private final File logFile;
    private Process process;

    public ServerProcess(String name, int port, int jmxPort, SmokeTestMode mode,
                         String redisUrl, String version, File logFile) {
        this.name = name;
        this.port = port;
        this.jmxPort = jmxPort;
        this.mode = mode;
        this.redisUrl = redisUrl;
        this.version = version;
        this.logFile = logFile;
    }

    public void start() throws IOException {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.addAll(serverJvmArgs());
        command.add("-Dnetty.socketio.version=" + version);
        command.add("-Dcom.sun.management.jmxremote");
        command.add("-Dcom.sun.management.jmxremote.port=" + jmxPort);
        command.add("-Dcom.sun.management.jmxremote.rmi.port=" + jmxPort);
        command.add("-Dcom.sun.management.jmxremote.local.only=true");
        command.add("-Dcom.sun.management.jmxremote.authenticate=false");
        command.add("-Dcom.sun.management.jmxremote.ssl=false");
        command.add("-Djava.rmi.server.hostname=127.0.0.1");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(ServerMain.class.getName());
        command.add(String.valueOf(port));
        command.add(mode.name().toLowerCase());
        command.add(redisUrl == null ? "" : redisUrl);
        command.add(name);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        process = builder.start();
    }

    public void waitUntilReady(long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!isAlive()) {
                throw new IllegalStateException(name + " exited before it became ready. See " + logFile.getAbsolutePath());
            }
            if (canConnect(port)) {
                return;
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException(name + " did not listen on port " + port + " within " + timeoutMs + " ms");
    }

    public void stop() {
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public int getPort() {
        return port;
    }

    public int getJmxPort() {
        return jmxPort;
    }

    public String getName() {
        return name;
    }

    public long getPid() {
        if (process == null) {
            return -1;
        }
        try {
            Method pidMethod = Process.class.getMethod("pid");
            Object pid = pidMethod.invoke(process);
            return pid instanceof Long ? (Long) pid : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private static boolean canConnect(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 250);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String javaExecutable() {
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }

    private static List<String> serverJvmArgs() {
        String args = System.getProperty("smoke.server.jvm.args", "-Xms256m -Xmx256m -XX:+UseG1GC");
        if (args.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(args.trim().split("\\s+")));
    }
}
