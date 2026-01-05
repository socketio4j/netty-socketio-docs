#!/bin/bash

# Netty SocketIO Smoke Test Runner (Standalone)
# This script runs performance tests using a specific version from Maven Central
# Usage: ./run-test.sh [version] [port] [clientCount] [eachMsgCount] [eachMsgSize]
#
# Example:
#   ./run-test.sh 4.0.0-SNAPSHOT                    # Use default test parameters
#   ./run-test.sh 4.0.0-SNAPSHOT 8899 10 10000 32   # Custom test parameters

set -e

# Default values
DEFAULT_VERSION="4.0.0-SNAPSHOT"
DEFAULT_PORT=8899
DEFAULT_CLIENT_COUNT=10
DEFAULT_EACH_MSG_COUNT=10000
DEFAULT_EACH_MSG_SIZE=32

# Parse arguments
VERSION=${1:-$DEFAULT_VERSION}
PORT=${2:-$DEFAULT_PORT}
CLIENT_COUNT=${3:-$DEFAULT_CLIENT_COUNT}
EACH_MSG_COUNT=${4:-$DEFAULT_EACH_MSG_COUNT}
EACH_MSG_SIZE=${5:-$DEFAULT_EACH_MSG_SIZE}

echo "=========================================="
echo "Netty SocketIO Smoke Test (Standalone)"
echo "=========================================="
echo "Version: $VERSION"
echo "Port: $PORT"
echo "Client Count: $CLIENT_COUNT"
echo "Messages per Client: $EACH_MSG_COUNT"
echo "Message Size: $EACH_MSG_SIZE bytes"
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

# Run performance test
java -Xms256m -Xmx256m $GC_OPTS -XX:+AlwaysPreTouch \
     -Dnetty.socketio.version=$VERSION \
     -cp target/netty-socketio-smoke-test.jar:target/dependency/* \
     com.socketio4j.socketio.smoketest.PerformanceTestRunner \
     $PORT $CLIENT_COUNT $EACH_MSG_COUNT $EACH_MSG_SIZE

echo ""
echo "=========================================="
echo "Smoke test completed!"
echo "=========================================="
echo "Results saved in: performance-results/"
echo "Report updated: ../gitbook/performance/performance-report.md"
echo ""

