# Netty SocketIO Smoke Test (Standalone)

This is a standalone smoke test project for Netty-SocketIO that tests versions from Maven Central.

## Overview

This smoke test project is independent from the main Netty-SocketIO project and can be used to test any released version from Maven Central. It automatically detects the version being tested and generates performance reports.

## Prerequisites

- Java 8 or higher
- Maven 3.0.5 or higher

## Usage

### Quick Start

Run the test with default parameters (current GA on Maven Central, **4.0.0**):

```bash
./run-test.sh
```

### Specify Version

Test a specific version (must resolve from Maven Central unless you add snapshot repositories / local `mvn install`):

```bash
./run-test.sh 4.0.0
```

### Custom Test Parameters

```bash
./run-test.sh [version] [port] [clientCount] [eachMsgCount] [eachMsgSize] [standalone|distributed] [redisUrl]
```

Example:
```bash
./run-test.sh 4.0.0 8899 10 50000 32 standalone
./run-test.sh 4.0.0 8899 10 50000 32 distributed redis://127.0.0.1:6379
TEST_DURATION_SECONDS=60 CLIENT_ROUNDS_PER_SECOND=1 SERVER_JVM_OPTS="-Xms1g -Xmx4g -XX:+UseG1GC -XX:+AlwaysPreTouch" ./run-test.sh 4.0.0 8899 10000 0 32 standalone
```

Parameters:
- `version`: Netty-SocketIO version to test (default: **4.0.0**)
- `port`: Server port (default: 8899)
- `clientCount`: Number of concurrent clients (default: 10)
- `eachMsgCount`: Messages per client (default: 10000). Set it to `0` when using duration mode.
- `eachMsgSize`: Message size in bytes (default: 32)
- `mode`: `standalone` starts one Socket.IO server process and tests direct echo; `distributed` starts two server processes and relays messages across Redis-backed rooms
- `redisUrl`: Redis address for distributed mode (default: `redis://127.0.0.1:6379`)

The test runner is only an orchestrator. Socket.IO servers run in separate JVM processes, and the runner samples each server's heap, non-heap, direct/mapped buffer memory, GC data, and process RSS every second by default. Raw samples are saved as `performance-results/*-memory.csv`, which makes it easier to verify the expected sawtooth pattern and compare first/last/post-GC heap, off-heap, and process memory baselines.

Duration mode is enabled with `TEST_DURATION_SECONDS`. In that mode the client sends full-client rounds for the configured time instead of stopping at `eachMsgCount`; `CLIENT_ROUNDS_PER_SECOND` controls how many rounds are sent per second.

### Manual Execution

If you prefer to run manually:

```bash
# Build with specific version
mvn clean package -Dnetty.socketio.version=4.0.0

# Run the test
java -Xms128m -Xmx256m -XX:+UseG1GC \
     -Dnetty.socketio.version=4.0.0 \
     -Dsmoke.server.jvm.args="-Xms256m -Xmx256m -XX:+UseG1GC -XX:+AlwaysPreTouch" \
     -Dsmoke.memory.sample.ms=1000 \
     -Dsmoke.test.duration.seconds=0 \
     -Dsmoke.client.rounds.per.second=1 \
     -cp target/netty-socketio-smoke-test.jar:target/dependency/* \
     com.socketio4j.socketio.smoketest.PerformanceTestRunner \
     8899 10 50000 32 standalone
```

## Test Results

- **JSON Results**: Saved in `performance-results/` directory
- **Markdown Report**: Automatically updated in `../gitbook/performance/performance-report.md`

## GitHub Actions

You can also trigger the smoke test via GitHub Actions workflow:

1. Go to the **Actions** tab in the repository
2. Select **Smoke Test** workflow
3. Click **Run workflow**
4. Fill in the parameters:
   - **Netty-SocketIO version**: The version to test (e.g., `4.0.0`, `3.0.1`, or a pre-release / snapshot coordinate you can resolve)
   - **JDK version**: Java version to use (8, 11, 17, 21, or 25)
   - **Port**: Server port (default: 8899)
   - **Client count**: Number of concurrent clients (default: 10)
   - **Messages per client**: Number of messages each client sends (default: 10000)
   - **Message size**: Size of each message in bytes (default: 32)
   - **Mode**: `standalone`, `distributed`, or `both`
   - **Duration seconds**: Set this to a positive value for continuous-send duration mode
   - **Client rounds per second**: Full-client send rounds per second in duration mode
   - **Post-GC wait**: Delay after explicit full GC before taking the post-GC memory sample
   - **Runner/Server JVM options**: Separate JVM options for the test runner and server process(es)
5. Click **Run workflow**

The workflow will:
- Build the smoke test with the specified Netty-SocketIO version
- Run the selected standalone and/or distributed mode on the specified JDK version
- Save JSON, CSV, and generated PNG memory charts to `performance-results/`
- Upload the same files as a workflow artifact
- Automatically commit the updated performance report and result files to the repository

## Configuration

### Changing Default Version

Edit `pom.xml` and change the `netty.socketio.version` property:

```xml
<properties>
    <netty.socketio.version>4.0.0</netty.socketio.version>
</properties>
```

### JVM Options

Set separate JVM options for the runner and server processes:

```bash
RUNNER_JVM_OPTS="-Xms1g -Xmx4g"
SERVER_JVM_OPTS="-Xms1g -Xmx4g -XX:+UseG1GC -XX:+AlwaysPreTouch"
```

Other useful runtime knobs:

```bash
TEST_DURATION_SECONDS=1800
CLIENT_ROUNDS_PER_SECOND=1
CLIENT_DRAIN_TIMEOUT_SECONDS=600
MEMORY_SAMPLE_MS=1000
POST_GC_WAIT_MS=60000
```

## Version Detection

The test automatically detects the Netty-SocketIO version being tested by:
1. System property `netty.socketio.version` (set by Maven)
2. Maven manifest files
3. Package implementation version
4. pom.properties from dependencies

## Notes

- This is a **manual test** - it does not run automatically
- Test results are saved to local `performance-results/` directory
- The test reads all historical JSON files from the local directory to generate the complete report
- The test uses Maven Central releases, not local builds
- Make sure the specified version exists in Maven Central before running
- To exercise an unreleased **`-SNAPSHOT`**, you must be able to resolve it (for example Sonatype snapshot repository and/or `mvn install` from the main library project); pass that version as the first argument to `./run-test.sh`

