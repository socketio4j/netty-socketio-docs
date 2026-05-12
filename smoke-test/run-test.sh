#!/bin/bash

# Netty SocketIO Smoke Test Runner (Standalone)
# This script runs performance tests using a specific version from Maven Central
# Usage: ./run-test.sh [version] [port] [clientCount] [eachMsgCount] [eachMsgSize] [standalone|distributed] [redisUrl]
#
# Example:
#   ./run-test.sh 4.0.0                           # Use default test parameters (GA on Central)
#   ./run-test.sh 4.0.0 8899 10 10000 32 standalone
#   ./run-test.sh 4.0.0 8899 10 10000 32 distributed redis://127.0.0.1:6379
#   TEST_DURATION_SECONDS=60 CLIENT_ROUNDS_PER_SECOND=1 SERVER_JVM_OPTS="-Xms1g -Xmx4g -XX:+UseG1GC" ./run-test.sh 4.0.0 8899 10000 0 32 standalone

set -e

# Default values
DEFAULT_VERSION="4.0.0"
DEFAULT_PORT=8899
DEFAULT_CLIENT_COUNT=10
DEFAULT_EACH_MSG_COUNT=10000
DEFAULT_EACH_MSG_SIZE=32
DEFAULT_MODE="standalone"
DEFAULT_REDIS_URL="redis://127.0.0.1:6379"

# Parse arguments
VERSION=${1:-$DEFAULT_VERSION}
PORT=${2:-$DEFAULT_PORT}
CLIENT_COUNT=${3:-$DEFAULT_CLIENT_COUNT}
EACH_MSG_COUNT=${4:-$DEFAULT_EACH_MSG_COUNT}
EACH_MSG_SIZE=${5:-$DEFAULT_EACH_MSG_SIZE}
MODE=${6:-$DEFAULT_MODE}
REDIS_URL=${7:-$DEFAULT_REDIS_URL}
SERVER_JVM_OPTS=${SERVER_JVM_OPTS:-"-Xms256m -Xmx256m -XX:+UseG1GC -XX:+AlwaysPreTouch"}
RUNNER_JVM_OPTS=${RUNNER_JVM_OPTS:-"-Xms128m -Xmx256m"}
MEMORY_SAMPLE_MS=${MEMORY_SAMPLE_MS:-1000}
TEST_DURATION_SECONDS=${TEST_DURATION_SECONDS:-0}
CLIENT_ROUNDS_PER_SECOND=${CLIENT_ROUNDS_PER_SECOND:-1}
CLIENT_DRAIN_TIMEOUT_SECONDS=${CLIENT_DRAIN_TIMEOUT_SECONDS:-600}
POST_GC_WAIT_MS=${POST_GC_WAIT_MS:-2000}

echo "=========================================="
echo "Netty SocketIO Smoke Test (Standalone)"
echo "=========================================="
echo "Version: $VERSION"
echo "Port: $PORT"
echo "Client Count: $CLIENT_COUNT"
echo "Messages per Client: $EACH_MSG_COUNT"
echo "Message Size: $EACH_MSG_SIZE bytes"
echo "Mode: $MODE"
echo "Runner JVM Options: $RUNNER_JVM_OPTS"
echo "Server JVM Options: $SERVER_JVM_OPTS"
echo "Memory Sample Interval: ${MEMORY_SAMPLE_MS}ms"
echo "Post-GC Wait: ${POST_GC_WAIT_MS}ms"
echo "Client Drain Timeout: ${CLIENT_DRAIN_TIMEOUT_SECONDS}s"
if [ "$TEST_DURATION_SECONDS" != "0" ]; then
    echo "Duration Mode: ${TEST_DURATION_SECONDS}s at ${CLIENT_ROUNDS_PER_SECOND} round(s)/second"
else
    echo "Duration Mode: disabled"
fi
if [ "$MODE" = "distributed" ]; then
    echo "Redis URL: $REDIS_URL"
fi
echo "=========================================="
echo ""

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    exit 1
fi

# Get Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
echo "Using Java version: $JAVA_VERSION"
echo ""

# Build the project with specified version
echo "Building smoke test with Netty-SocketIO version $VERSION..."
mvn clean package -Dnetty.socketio.version=$VERSION -DskipTests

echo ""
echo "Running smoke test..."
echo ""

# Determine GC flags based on Java version
GC_OPTS="-XX:+UseG1GC"

# Run performance test. The runner is separate from the Socket.IO server JVMs.
java $RUNNER_JVM_OPTS $GC_OPTS \
     -Dnetty.socketio.version=$VERSION \
     -Dsmoke.server.jvm.args="$SERVER_JVM_OPTS" \
     -Dsmoke.memory.sample.ms="$MEMORY_SAMPLE_MS" \
     -Dsmoke.post.gc.wait.ms="$POST_GC_WAIT_MS" \
     -Dsmoke.test.duration.seconds="$TEST_DURATION_SECONDS" \
     -Dsmoke.client.rounds.per.second="$CLIENT_ROUNDS_PER_SECOND" \
     -Dsmoke.client.drain.timeout.seconds="$CLIENT_DRAIN_TIMEOUT_SECONDS" \
     -cp target/netty-socketio-smoke-test.jar:target/dependency/* \
     com.socketio4j.socketio.smoketest.PerformanceTestRunner \
     $PORT $CLIENT_COUNT $EACH_MSG_COUNT $EACH_MSG_SIZE $MODE $REDIS_URL

echo ""
echo "=========================================="
echo "Smoke test completed!"
echo "=========================================="
echo "Results saved in: performance-results/"
echo "Report updated: ../gitbook/performance/performance-report.md"
echo ""

