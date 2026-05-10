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
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ServerMemoryMonitor implements AutoCloseable {

    private final String nodeName;
    private final int jmxPort;
    private final long pid;
    private final long intervalMs;
    private final List<MemorySample> samples = new ArrayList<>();
    private JMXConnector connector;
    private MBeanServerConnection connection;
    private MemoryMXBean memoryBean;
    private ScheduledExecutorService executor;

    public ServerMemoryMonitor(String nodeName, int jmxPort, long pid, long intervalMs) {
        this.nodeName = nodeName;
        this.jmxPort = jmxPort;
        this.pid = pid;
        this.intervalMs = intervalMs;
    }

    public void start() throws Exception {
        connect();
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "memory-monitor-" + nodeName);
            thread.setDaemon(true);
            return thread;
        });
        sample("start");
        executor.scheduleAtFixedRate(() -> {
            try {
                sample("periodic");
            } catch (Exception ignored) {
                // The runner will notice if the server process exits; keep sampling best-effort.
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void sample(String phase) throws Exception {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        GcStats gcStats = gcStats();
        long directMemoryUsed = bufferPoolMemoryUsed("direct");
        long mappedMemoryUsed = bufferPoolMemoryUsed("mapped");
        long processRss = processRss();
        samples.add(new MemorySample(nodeName, phase, System.currentTimeMillis(),
                heap.getUsed(), heap.getCommitted(), heap.getMax(),
                nonHeap.getUsed(), nonHeap.getCommitted(), nonHeap.getMax(),
                directMemoryUsed, mappedMemoryUsed, processRss,
                gcStats.count, gcStats.timeMs));
    }

    public void requestGcAndSample() throws Exception {
        memoryBean.gc();
        Thread.sleep(Long.getLong("smoke.post.gc.wait.ms", 2000L));
        sample("post-gc");
    }

    public synchronized MemorySummary summary() {
        MemorySummary summary = new MemorySummary();
        summary.nodeName = nodeName;
        if (samples.isEmpty()) {
            return summary;
        }

        MemorySample first = samples.get(0);
        MemorySample last = samples.get(samples.size() - 1);
        MemorySample max = first;
        MemorySample maxDirect = first;
        MemorySample maxRss = first;
        for (MemorySample sample : samples) {
            if (sample.heapUsed > max.heapUsed) {
                max = sample;
            }
            if (sample.directMemoryUsed > maxDirect.directMemoryUsed) {
                maxDirect = sample;
            }
            if (sample.processRss > maxRss.processRss) {
                maxRss = sample;
            }
            if ("post-gc".equals(sample.phase)) {
                summary.postGcHeapUsed = sample.heapUsed;
                summary.postGcDirectMemoryUsed = sample.directMemoryUsed;
                summary.postGcProcessRss = sample.processRss;
            }
        }

        summary.samples = samples.size();
        summary.firstHeapUsed = first.heapUsed;
        summary.lastHeapUsed = last.heapUsed;
        summary.maxHeapUsed = max.heapUsed;
        summary.heapMax = last.heapMax;
        summary.firstDirectMemoryUsed = first.directMemoryUsed;
        summary.lastDirectMemoryUsed = last.directMemoryUsed;
        summary.maxDirectMemoryUsed = maxDirect.directMemoryUsed;
        summary.firstMappedMemoryUsed = first.mappedMemoryUsed;
        summary.lastMappedMemoryUsed = last.mappedMemoryUsed;
        summary.maxMappedMemoryUsed = maxMappedMemoryUsed();
        summary.firstProcessRss = first.processRss;
        summary.lastProcessRss = last.processRss;
        summary.maxProcessRss = maxRss.processRss;
        summary.lastMinusFirst = last.heapUsed - first.heapUsed;
        summary.postGcMinusFirst = summary.postGcHeapUsed == 0 ? 0 : summary.postGcHeapUsed - first.heapUsed;
        summary.directLastMinusFirst = last.directMemoryUsed - first.directMemoryUsed;
        summary.directPostGcMinusFirst = summary.postGcDirectMemoryUsed == 0 ? 0 : summary.postGcDirectMemoryUsed - first.directMemoryUsed;
        summary.rssLastMinusFirst = last.processRss - first.processRss;
        summary.rssPostGcMinusFirst = summary.postGcProcessRss == 0 ? 0 : summary.postGcProcessRss - first.processRss;
        summary.gcCount = last.gcCount - first.gcCount;
        summary.gcTimeMs = last.gcTimeMs - first.gcTimeMs;
        return summary;
    }

    public synchronized void writeCsv(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("node,phase,timestamp,heapUsed,heapCommitted,heapMax,nonHeapUsed,nonHeapCommitted,nonHeapMax,directMemoryUsed,mappedMemoryUsed,processRss,gcCount,gcTimeMs\n");
            for (MemorySample sample : samples) {
                writer.write(sample.toCsv());
                writer.write('\n');
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (connector != null) {
            connector.close();
        }
    }

    private void connect() throws Exception {
        JMXServiceURL url = new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + jmxPort + "/jmxrmi");
        connector = JMXConnectorFactory.connect(url);
        connection = connector.getMBeanServerConnection();
        memoryBean = ManagementFactory.newPlatformMXBeanProxy(
                connection, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
    }

    private GcStats gcStats() throws Exception {
        Set<ObjectName> names = connection.queryNames(
                new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*"), null);
        long count = 0;
        long timeMs = 0;
        for (ObjectName name : names) {
            Object collectionCount = connection.getAttribute(name, "CollectionCount");
            Object collectionTime = connection.getAttribute(name, "CollectionTime");
            if (collectionCount instanceof Long && (Long) collectionCount > 0) {
                count += (Long) collectionCount;
            }
            if (collectionTime instanceof Long && (Long) collectionTime > 0) {
                timeMs += (Long) collectionTime;
            }
        }
        return new GcStats(count, timeMs);
    }

    private long bufferPoolMemoryUsed(String name) throws Exception {
        ObjectName objectName = new ObjectName("java.nio:type=BufferPool,name=" + name);
        if (!connection.isRegistered(objectName)) {
            return 0;
        }
        Object memoryUsed = connection.getAttribute(objectName, "MemoryUsed");
        return memoryUsed instanceof Long ? (Long) memoryUsed : 0;
    }

    private long maxMappedMemoryUsed() {
        long max = 0;
        for (MemorySample sample : samples) {
            max = Math.max(max, sample.mappedMemoryUsed);
        }
        return max;
    }

    private long processRss() {
        if (pid <= 0) {
            return 0;
        }
        Process process = null;
        try {
            process = new ProcessBuilder("ps", "-o", "rss=", "-p", String.valueOf(pid)).start();
            byte[] data = readAll(process.getInputStream());
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return 0;
            }
            String value = new String(data, "UTF-8").trim();
            if (value.isEmpty()) {
                return 0;
            }
            return Long.parseLong(value) * 1024L;
        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }
            return 0;
        }
    }

    private byte[] readAll(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[128];
        int offset = 0;
        int read;
        while ((read = inputStream.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += read;
            if (offset == buffer.length) {
                byte[] next = new byte[buffer.length * 2];
                System.arraycopy(buffer, 0, next, 0, buffer.length);
                buffer = next;
            }
        }
        byte[] result = new byte[offset];
        System.arraycopy(buffer, 0, result, 0, offset);
        return result;
    }

    private static class GcStats {
        private final long count;
        private final long timeMs;

        private GcStats(long count, long timeMs) {
            this.count = count;
            this.timeMs = timeMs;
        }
    }

    private static class MemorySample {
        private final String nodeName;
        private final String phase;
        private final long timestamp;
        private final long heapUsed;
        private final long heapCommitted;
        private final long heapMax;
        private final long nonHeapUsed;
        private final long nonHeapCommitted;
        private final long nonHeapMax;
        private final long directMemoryUsed;
        private final long mappedMemoryUsed;
        private final long processRss;
        private final long gcCount;
        private final long gcTimeMs;

        private MemorySample(String nodeName, String phase, long timestamp,
                             long heapUsed, long heapCommitted, long heapMax,
                             long nonHeapUsed, long nonHeapCommitted, long nonHeapMax,
                             long directMemoryUsed, long mappedMemoryUsed, long processRss,
                             long gcCount, long gcTimeMs) {
            this.nodeName = nodeName;
            this.phase = phase;
            this.timestamp = timestamp;
            this.heapUsed = heapUsed;
            this.heapCommitted = heapCommitted;
            this.heapMax = heapMax;
            this.nonHeapUsed = nonHeapUsed;
            this.nonHeapCommitted = nonHeapCommitted;
            this.nonHeapMax = nonHeapMax;
            this.directMemoryUsed = directMemoryUsed;
            this.mappedMemoryUsed = mappedMemoryUsed;
            this.processRss = processRss;
            this.gcCount = gcCount;
            this.gcTimeMs = gcTimeMs;
        }

        private String toCsv() {
            return nodeName + "," + phase + "," + timestamp + "," + heapUsed + "," + heapCommitted + ","
                    + heapMax + "," + nonHeapUsed + "," + nonHeapCommitted + "," + nonHeapMax + ","
                    + directMemoryUsed + "," + mappedMemoryUsed + "," + processRss + ","
                    + gcCount + "," + gcTimeMs;
        }
    }

    public static class MemorySummary {
        public String nodeName;
        public int samples;
        public long firstHeapUsed;
        public long lastHeapUsed;
        public long maxHeapUsed;
        public long postGcHeapUsed;
        public long heapMax;
        public long firstDirectMemoryUsed;
        public long lastDirectMemoryUsed;
        public long maxDirectMemoryUsed;
        public long postGcDirectMemoryUsed;
        public long firstMappedMemoryUsed;
        public long lastMappedMemoryUsed;
        public long maxMappedMemoryUsed;
        public long firstProcessRss;
        public long lastProcessRss;
        public long maxProcessRss;
        public long postGcProcessRss;
        public long lastMinusFirst;
        public long postGcMinusFirst;
        public long directLastMinusFirst;
        public long directPostGcMinusFirst;
        public long rssLastMinusFirst;
        public long rssPostGcMinusFirst;
        public long gcCount;
        public long gcTimeMs;
    }
}
