# Netty SocketIO Smoke Test (Standalone)

This is a standalone smoke test project for Netty-SocketIO that tests versions from Maven Central.

## Overview

This smoke test project is independent from the main Netty-SocketIO project and can be used to test any released version from Maven Central. It automatically detects the version being tested and generates performance reports.

## Prerequisites

- Java 8 or higher
- Maven 3.0.5 or higher

## Usage

### Quick Start

Run the test with default parameters (version 3.0.1):

```bash
./run-test.sh
```

### Specify Version

Test a specific version:

```bash
./run-test.sh 3.0.1
```

### Custom Test Parameters

```bash
./run-test.sh [version] [port] [clientCount] [eachMsgCount] [eachMsgSize]
```

Example:
```bash
./run-test.sh 3.0.1 8899 10 50000 32
```

Parameters:
- `version`: Netty-SocketIO version to test (default: 3.0.1)
- `port`: Server port (default: 8899)
- `clientCount`: Number of concurrent clients (default: 10)
- `eachMsgCount`: Messages per client (default: 50000)
- `eachMsgSize`: Message size in bytes (default: 32)

### Manual Execution

If you prefer to run manually:

```bash
# Build with specific version
mvn clean package -Dnetty.socketio.version=3.0.1

# Run the test
java -Xms256m -Xmx256m -XX:+UseG1GC -XX:+AlwaysPreTouch \
     -Dnetty.socketio.version=3.0.1 \
     -cp target/netty-socketio-smoke-test.jar:target/dependency/* \
     com.socketio4j.socketio.smoketest.PerformanceTestRunner \
     8899 10 50000 32
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
   - **Netty-SocketIO version**: The version to test (e.g., 3.0.1)
   - **JDK version**: Java version to use (8, 11, 17, 21, or 25)
   - **Port**: Server port (default: 8899)
   - **Client count**: Number of concurrent clients (default: 10)
   - **Messages per client**: Number of messages each client sends (default: 10000)
   - **Message size**: Size of each message in bytes (default: 32)
5. Click **Run workflow**

The workflow will:
- Build the smoke test with the specified Netty-SocketIO version
- Run the test on the specified JDK version
- Save test results to `performance-results/` directory
- Automatically commit the updated performance report and JSON results to the repository

## Configuration

### Changing Default Version

Edit `pom.xml` and change the `netty.socketio.version` property:

```xml
<properties>
    <netty.socketio.version>3.0.1</netty.socketio.version>
</properties>
```

### JVM Options

Modify the JVM options in `run-test.sh`:

```bash
java -Xms256m -Xmx256m -XX:+UseG1GC -XX:+AlwaysPreTouch ...
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

