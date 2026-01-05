---
icon: hand-wave
layout:
  width: default
  title:
    visible: true
  description:
    visible: false
  tableOfContents:
    visible: true
  outline:
    visible: true
  pagination:
    visible: true
  metadata:
    visible: true
metaLinks:
  alternates:
    - https://app.gitbook.com/s/yE16Xb3IemPxJWydtPOj/
---

# Performance

Customer feedback in **2012**:\
CentOS, 1 CPU, 4GB RAM runned on VM: CPU 10%, Memory 15%\
6000 xhr-long polling sessions or 15000 websockets sessions\
4000 messages per second

Customer feedback in **2014**:\
"To stress test the solution we run 30 000 simultaneous websocket clients and managed to peak at total of about 140 000 messages per second with less than 1 second average delay." (c) Viktor Endersz - Kambi Sports Solutions

Reference : [https://github.com/mrniko/netty-socketio](https://github.com/mrniko/netty-socketio)

### Performance Characteristics  <a href="#performance-characteristics" id="performance-characteristics"></a>

#### Scalability  <a href="#scalability" id="scalability"></a>

* Supports thousands of concurrent connections
* Efficient memory usage with connection pooling
* Lock-free and thread-safe implementation

#### Throughput  <a href="#throughput" id="throughput"></a>

* High message throughput (140,000+ messages/second)
* Low latency message delivery
* Efficient binary and text message handling

#### Resource Efficiency  <a href="#resource-efficiency" id="resource-efficiency"></a>

* Low CPU usage under load
* Efficient memory management
* Minimal garbage collection pressure

#### Running Performance Tests  <a href="#running-performance-tests" id="running-performance-tests"></a>

```
cd netty-socketio-smoke-test
mvn test
```

### Optimisation Tips  <a href="#optimization-tips" id="optimization-tips"></a>

1. **Use WebSocket Transport**: WebSocket is more efficient than polling
2. **Configure Worker Threads**: Adjust worker thread count based on your CPU cores
3. **Use Distributed Stores**: For multi-node deployments, use Redisson or Hazelcast
4. **Tune Buffer Sizes**: Configure write buffer watermarks appropriately
5. **Monitor Resource Usage**: Keep an eye on CPU and memory usage
