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
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Performance test runner that keeps Socket.IO servers in separate JVMs and
 * samples server memory through JMX while clients generate load.
 */
public class PerformanceTestRunner {

    private static final Logger log = LoggerFactory.getLogger(PerformanceTestRunner.class);
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemInfo systemInfo;
    private final int port;
    private final int clientCount;
    private final int eachMsgCount;
    private final int eachMsgSize;
    private final SmokeTestMode mode;
    private final String redisUrl;
    private final String javaVersion;
    private final String jvmArgs;
    private final String version;

    public PerformanceTestRunner(int port, int clientCount, int eachMsgCount, int eachMsgSize,
                                 SmokeTestMode mode, String redisUrl) {
        this.systemInfo = new SystemInfo();
        this.port = port;
        this.clientCount = clientCount;
        this.eachMsgCount = eachMsgCount;
        this.eachMsgSize = eachMsgSize;
        this.mode = mode;
        this.redisUrl = redisUrl;
        this.javaVersion = System.getProperty("java.version");
        this.jvmArgs = String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments());
        this.version = System.getProperty("netty.socketio.version", "unknown");
    }

    public void runTest() throws Exception {
        log.info("Starting {} performance test with servers in separate JVMs", mode);
        log.info("Java Version: {}", javaVersion);
        log.info("Runner JVM Args: {}", jvmArgs);
        log.info("Server JVM Args: {}", System.getProperty("smoke.server.jvm.args", "-Xms256m -Xmx256m -XX:+UseG1GC"));
        log.info("Netty-SocketIO Version: {}", version);

        File resultsDir = ensureResultsDir();
        List<ServerProcess> servers = new ArrayList<>();
        List<ServerMemoryMonitor> monitors = new ArrayList<>();
        List<Integer> serverPorts = new ArrayList<>();
        ClientMetrics clientMetrics = new ClientMetrics();

        try {
            startServers(resultsDir, servers, monitors, serverPorts);

            ClientMain client = new ClientMain(serverPorts, clientCount, eachMsgCount, eachMsgSize, mode, clientMetrics);
            client.start();

            for (ServerMemoryMonitor monitor : monitors) {
                monitor.requestGcAndSample();
            }

            PerformanceResult result = collectPerformanceResult(client.getMetrics(), monitors);
            saveResults(result, monitors, resultsDir);
        } finally {
            for (ServerMemoryMonitor monitor : monitors) {
                try {
                    monitor.close();
                } catch (IOException e) {
                    log.warn("Failed to close memory monitor", e);
                }
            }
            for (ServerProcess server : servers) {
                server.stop();
            }
        }
    }

    private void startServers(File resultsDir, List<ServerProcess> servers,
                              List<ServerMemoryMonitor> monitors, List<Integer> serverPorts) throws Exception {
        int serverCount = mode == SmokeTestMode.DISTRIBUTED ? 2 : 1;
        if (mode == SmokeTestMode.DISTRIBUTED && (redisUrl == null || redisUrl.trim().isEmpty())) {
            throw new IllegalArgumentException("Distributed mode requires redisUrl, for example redis://127.0.0.1:6379");
        }

        long sampleIntervalMs = Long.getLong("smoke.memory.sample.ms", 1000L);
        for (int i = 0; i < serverCount; i++) {
            String nodeName = "server-" + (i + 1);
            int serverPort = i == 0 ? port : findAvailablePort();
            int jmxPort = findAvailablePort();
            File logFile = new File(resultsDir, nodeName + ".log");
            ServerProcess server = new ServerProcess(nodeName, serverPort, jmxPort, mode, redisUrl, version, logFile);
            server.start();
            server.waitUntilReady(30000);

            ServerMemoryMonitor monitor = new ServerMemoryMonitor(nodeName, jmxPort, server.getPid(), sampleIntervalMs);
            monitor.start();

            servers.add(server);
            monitors.add(monitor);
            serverPorts.add(serverPort);
            log.info("{} ready on port {} with JMX port {}", nodeName, serverPort, jmxPort);
        }
    }

    private PerformanceResult collectPerformanceResult(ClientMetrics metrics, List<ServerMemoryMonitor> monitors) {
        PerformanceResult result = new PerformanceResult();

        result.timestamp = LocalDateTime.now().format(FORMATTER);
        result.javaVersion = javaVersion;
        result.jvmArgs = jvmArgs;
        result.serverJvmArgs = System.getProperty("smoke.server.jvm.args", "-Xms256m -Xmx256m -XX:+UseG1GC");
        result.version = version;
        result.mode = mode.name().toLowerCase();
        result.serverCount = monitors.size();
        result.operatingSystem = System.getProperty("os.name") + " " + System.getProperty("os.version");
        result.architecture = System.getProperty("os.arch");
        result.cpuCount = systemInfo.getAvailableProcessors();
        result.totalMemory = systemInfo.getTotalPhysicalMemory();
        result.freeMemory = systemInfo.getFreePhysicalMemory();

        result.port = port;
        result.clientCount = clientCount;
        result.eachMsgCount = eachMsgCount;
        result.eachMsgSize = eachMsgSize;

        result.messagesSent = metrics.getTotalMessagesSent();
        result.messagesReceived = metrics.getTotalMessagesReceived();
        result.bytesSent = metrics.getTotalBytesSent();
        result.bytesReceived = metrics.getTotalBytesReceived();
        result.errors = metrics.getTotalErrors();
        result.minLatency = metrics.getMinLatency();
        result.maxLatency = metrics.getMaxLatency();
        result.avgLatency = metrics.getAverageLatency();
        result.p50Latency = metrics.getLatencyP50();
        result.p90Latency = metrics.getLatencyP90();
        result.p99Latency = metrics.getLatencyP99();
        result.testDuration = metrics.getTestDuration();
        result.messagesPerSecond = metrics.getMessagesPerSecond();
        result.bytesPerSecond = metrics.getBytesPerSecond();
        result.errorRate = metrics.getErrorRate();
        result.messageLossRate = metrics.getMessageLossRate();

        for (ServerMemoryMonitor monitor : monitors) {
            ServerMemoryMonitor.MemorySummary summary = monitor.summary();
            result.memorySummaries.add(summary);
            result.heapUsed = Math.max(result.heapUsed, summary.lastHeapUsed);
            result.heapMax = Math.max(result.heapMax, summary.heapMax);
            result.heapCommitted = Math.max(result.heapCommitted, summary.maxHeapUsed);
        }
        return result;
    }

    private void saveResults(PerformanceResult result, List<ServerMemoryMonitor> monitors, File resultsDir) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String baseName = String.format("performance-result-%s-%s-%s",
                result.mode, javaVersion.replace(".", "_"), timestamp);

        File memoryCsv = new File(resultsDir, baseName + "-memory.csv");
        writeMemoryCsv(monitors, memoryCsv);
        result.memoryCsv = memoryCsv.getName();

        File jsonFile = new File(resultsDir, baseName + ".json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, toJson(result));
        log.info("Results saved to: {}", jsonFile.getAbsolutePath());
        log.info("Server memory samples saved to: {}", memoryCsv.getAbsolutePath());

        regenerateMarkdownReportFromJson(resultsDir);
    }

    private ObjectNode toJson(PerformanceResult result) {
        ObjectNode json = mapper.createObjectNode();
        json.put("timestamp", result.timestamp);
        json.put("javaVersion", result.javaVersion);
        json.put("jvmArgs", result.jvmArgs);
        json.put("serverJvmArgs", result.serverJvmArgs);
        json.put("version", result.version);
        json.put("mode", result.mode);
        json.put("serverCount", result.serverCount);
        json.put("operatingSystem", result.operatingSystem);
        json.put("architecture", result.architecture);
        json.put("cpuCount", result.cpuCount);
        json.put("totalMemory", result.totalMemory);
        json.put("freeMemory", result.freeMemory);
        json.put("port", result.port);
        json.put("clientCount", result.clientCount);
        json.put("eachMsgCount", result.eachMsgCount);
        json.put("eachMsgSize", result.eachMsgSize);
        json.put("messagesSent", result.messagesSent);
        json.put("messagesReceived", result.messagesReceived);
        json.put("bytesSent", result.bytesSent);
        json.put("bytesReceived", result.bytesReceived);
        json.put("errors", result.errors);
        json.put("minLatency", result.minLatency);
        json.put("maxLatency", result.maxLatency);
        json.put("avgLatency", result.avgLatency);
        json.put("p50Latency", result.p50Latency);
        json.put("p90Latency", result.p90Latency);
        json.put("p99Latency", result.p99Latency);
        json.put("testDuration", result.testDuration);
        json.put("messagesPerSecond", result.messagesPerSecond);
        json.put("bytesPerSecond", result.bytesPerSecond);
        json.put("errorRate", result.errorRate);
        json.put("messageLossRate", result.messageLossRate);
        json.put("heapUsed", result.heapUsed);
        json.put("heapMax", result.heapMax);
        json.put("heapCommitted", result.heapCommitted);
        json.put("memoryCsv", result.memoryCsv);

        ArrayNode memory = json.putArray("memorySummaries");
        for (ServerMemoryMonitor.MemorySummary summary : result.memorySummaries) {
            ObjectNode node = memory.addObject();
            node.put("nodeName", summary.nodeName);
            node.put("samples", summary.samples);
            node.put("firstHeapUsed", summary.firstHeapUsed);
            node.put("lastHeapUsed", summary.lastHeapUsed);
            node.put("maxHeapUsed", summary.maxHeapUsed);
            node.put("postGcHeapUsed", summary.postGcHeapUsed);
            node.put("heapMax", summary.heapMax);
            node.put("firstDirectMemoryUsed", summary.firstDirectMemoryUsed);
            node.put("lastDirectMemoryUsed", summary.lastDirectMemoryUsed);
            node.put("maxDirectMemoryUsed", summary.maxDirectMemoryUsed);
            node.put("postGcDirectMemoryUsed", summary.postGcDirectMemoryUsed);
            node.put("firstMappedMemoryUsed", summary.firstMappedMemoryUsed);
            node.put("lastMappedMemoryUsed", summary.lastMappedMemoryUsed);
            node.put("maxMappedMemoryUsed", summary.maxMappedMemoryUsed);
            node.put("firstProcessRss", summary.firstProcessRss);
            node.put("lastProcessRss", summary.lastProcessRss);
            node.put("maxProcessRss", summary.maxProcessRss);
            node.put("postGcProcessRss", summary.postGcProcessRss);
            node.put("lastMinusFirst", summary.lastMinusFirst);
            node.put("postGcMinusFirst", summary.postGcMinusFirst);
            node.put("directLastMinusFirst", summary.directLastMinusFirst);
            node.put("directPostGcMinusFirst", summary.directPostGcMinusFirst);
            node.put("rssLastMinusFirst", summary.rssLastMinusFirst);
            node.put("rssPostGcMinusFirst", summary.rssPostGcMinusFirst);
            node.put("gcCount", summary.gcCount);
            node.put("gcTimeMs", summary.gcTimeMs);
        }
        return json;
    }

    private void writeMemoryCsv(List<ServerMemoryMonitor> monitors, File memoryCsv) throws IOException {
        try (FileWriter writer = new FileWriter(memoryCsv)) {
            writer.write("node,phase,timestamp,heapUsed,heapCommitted,heapMax,nonHeapUsed,nonHeapCommitted,nonHeapMax,directMemoryUsed,mappedMemoryUsed,processRss,gcCount,gcTimeMs\n");
        }
        for (ServerMemoryMonitor monitor : monitors) {
            File nodeCsv = new File(memoryCsv.getParentFile(), memoryCsv.getName() + "." + monitor.summary().nodeName + ".tmp");
            monitor.writeCsv(nodeCsv);
            appendWithoutHeader(memoryCsv, nodeCsv);
            if (!nodeCsv.delete()) {
                log.debug("Failed to delete temporary memory CSV {}", nodeCsv.getAbsolutePath());
            }
        }
    }

    private void appendWithoutHeader(File target, File source) throws IOException {
        List<String> lines = java.nio.file.Files.readAllLines(source.toPath());
        try (FileWriter writer = new FileWriter(target, true)) {
            for (int i = 1; i < lines.size(); i++) {
                writer.write(lines.get(i));
                writer.write('\n');
            }
        }
    }

    private void regenerateMarkdownReportFromJson(File resultsDir) throws IOException {
        List<PerformanceResult> results = new ArrayList<>();
        File[] jsonFiles = resultsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            return;
        }
        for (File jsonFile : jsonFiles) {
            try {
                PerformanceResult result = mapper.readValue(jsonFile, PerformanceResult.class);
                result.resultJson = jsonFile.getName();
                results.add(result);
            } catch (Exception e) {
                log.warn("Failed to read JSON file: {}", jsonFile.getName(), e);
            }
        }
        results.sort(Comparator.comparing((PerformanceResult r) -> r.timestamp).reversed());
        generateMarkdownReport(results);
    }

    private void generateMarkdownReport(List<PerformanceResult> results) throws IOException {
        File reportFile = reportFile();
        StringBuilder report = new StringBuilder();
        report.append("# Netty Socket.IO Performance Test Report\n\n");
        report.append("This report contains smoke-test results with Socket.IO servers running in separate JVM processes. ");
        report.append("Current tests sample heap, non-heap, `java.nio` direct/mapped buffer memory, process RSS, and GC statistics from the server JVMs.\n\n");

        report.append("## Latest Result Charts\n\n");
        int chartCount = 0;
        for (PerformanceResult result : results) {
            String chart = chartPath(result);
            if (chart.isEmpty()) {
                continue;
            }
            report.append(String.format("### %s %s\n\n", result.timestamp, result.mode == null ? "standalone" : result.mode));
            report.append(String.format("![%s %s memory chart](%s)\n\n",
                    result.timestamp,
                    result.mode == null ? "standalone" : result.mode,
                    chart));
            chartCount++;
            if (chartCount >= 4) {
                break;
            }
        }

        report.append("## Historical Results\n\n");
        report.append("| Date | Mode | Clients | Msg/Client | Messages/sec | Avg Latency (ms) | P99 Latency (ms) | Error Rate (%) | Loss Rate (%) | Server Heap Peak MB | Server Post-GC Delta MB | Chart | Memory CSV | Version |\n");
        report.append("|------|------|---------|------------|--------------|------------------|------------------|----------------|---------------|--------------------|-------------------------|-------|------------|---------|\n");
        for (PerformanceResult result : results) {
            MemoryAggregate memory = aggregateMemory(result);
            report.append(String.format("| %s | %s | %d | %d | %,.2f | %.2f | %d | %.4f | %.4f | %d | %d | %s | %s | %s |\n",
                    result.timestamp,
                    result.mode == null ? "standalone" : result.mode,
                    result.clientCount,
                    result.eachMsgCount,
                    result.messagesPerSecond,
                    result.avgLatency,
                    result.p99Latency,
                    result.errorRate * 100,
                    result.messageLossRate * 100,
                    memory.maxHeapUsed / (1024 * 1024),
                    memory.postGcDelta / (1024 * 1024),
                    chartLink(result),
                    result.memoryCsv == null ? "" : memoryCsvLink(result.memoryCsv),
                    result.version));
        }
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(report.toString());
        }
        log.info("Markdown report regenerated: {}", reportFile.getAbsolutePath());
    }

    private String chartPath(PerformanceResult result) {
        String chartFile = chartFile(result);
        return chartFile.isEmpty() ? "" : "../../smoke-test/performance-results/" + chartFile;
    }

    private String chartLink(PerformanceResult result) {
        String chart = chartPath(result);
        return chart.isEmpty() ? "" : "[png](" + chart + ")";
    }

    private String memoryCsvLink(String memoryCsv) {
        return "[csv](../../smoke-test/performance-results/" + memoryCsv + ")";
    }

    private String chartFile(PerformanceResult result) {
        if (result.resultJson != null && result.resultJson.endsWith(".json")) {
            return result.resultJson.substring(0, result.resultJson.length() - ".json".length()) + "-memory-chart.png";
        }
        if (result.memoryCsv != null && result.memoryCsv.endsWith("-memory.csv")) {
            return result.memoryCsv.substring(0, result.memoryCsv.length() - "-memory.csv".length()) + "-memory-chart.png";
        }
        return "";
    }

    private MemoryAggregate aggregateMemory(List<ServerMemoryMonitor.MemorySummary> summaries) {
        MemoryAggregate aggregate = new MemoryAggregate();
        for (ServerMemoryMonitor.MemorySummary summary : summaries) {
            aggregate.maxHeapUsed = Math.max(aggregate.maxHeapUsed, summary.maxHeapUsed);
            aggregate.postGcDelta = Math.max(aggregate.postGcDelta, summary.postGcMinusFirst);
        }
        return aggregate;
    }

    private MemoryAggregate aggregateMemory(PerformanceResult result) {
        MemoryAggregate aggregate = aggregateMemory(result.memorySummaries);
        if (aggregate.maxHeapUsed == 0 && result.heapMax > 0) {
            aggregate.maxHeapUsed = result.heapMax;
        }
        return aggregate;
    }

    private File reportFile() {
        File report = new File("../gitbook/performance/performance-report.md");
        if (report.getParentFile() != null) {
            report.getParentFile().mkdirs();
        }
        return report;
    }

    private File ensureResultsDir() {
        File resultsDir = new File("performance-results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
        return resultsDir;
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8899;
            int clientCount = args.length > 1 ? Integer.parseInt(args[1]) : 10;
            int eachMsgCount = args.length > 2 ? Integer.parseInt(args[2]) : 50000;
            int eachMsgSize = args.length > 3 ? Integer.parseInt(args[3]) : 32;
            SmokeTestMode mode = args.length > 4 ? SmokeTestMode.from(args[4]) : SmokeTestMode.STANDALONE;
            String redisUrl = args.length > 5 ? args[5] : null;

            PerformanceTestRunner runner = new PerformanceTestRunner(
                    port, clientCount, eachMsgCount, eachMsgSize, mode, redisUrl);
            runner.runTest();
            System.exit(0);
        } catch (Exception e) {
            log.error("Performance test failed", e);
            System.exit(1);
        }
    }

    private static class MemoryAggregate {
        private long maxHeapUsed;
        private long postGcDelta;
    }

    public static class PerformanceResult {
        public String timestamp;
        public String javaVersion;
        public String jvmArgs;
        public String serverJvmArgs;
        public String version;
        public String mode;
        public int serverCount;
        public String operatingSystem;
        public String architecture;
        public int cpuCount;
        public long totalMemory;
        public long freeMemory;
        public int port;
        public int clientCount;
        public int eachMsgCount;
        public int eachMsgSize;
        public long messagesSent;
        public long messagesReceived;
        public long bytesSent;
        public long bytesReceived;
        public long errors;
        public long minLatency;
        public long maxLatency;
        public double avgLatency;
        public long p50Latency;
        public long p90Latency;
        public long p99Latency;
        public long testDuration;
        public double messagesPerSecond;
        public double bytesPerSecond;
        public double errorRate;
        public double messageLossRate;
        public long heapUsed;
        public long heapMax;
        public long heapCommitted;
        public String memoryCsv;
        public String resultJson;
        public List<ServerMemoryMonitor.MemorySummary> memorySummaries = new ArrayList<>();
    }
}
