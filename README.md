---
icon: play
---

# Getting Started

## Version

* 5.0 - Planned
* 4.0 - New API & Adapter
  * 4.0.0-SNAPSHOT - Upcoming LTS
* 3.0 - Maintenance of parent fork
  * 3.0.1 - Stable - Compatiable with parent fork

{% hint style="info" %}
## Please check version policy for more [version-policy.md](version-policy.md "mention")
{% endhint %}

## Java Support Matrix

| Version | Minimum Java for Compilation | Minimum Java for Runtime | Recommended Java Versions |
| ------- | ---------------------------- | ------------------------ | ------------------------- |
| 3.0.x   | 11                           | 8                        | 17 / 21 / 25              |
| 4.0.x   | 11                           | 8                        | 17 / 21 / 25              |

## Installation

### Java

{% tabs %}
{% tab title="Maven" %}
{% code title="pom.xml" %}
```xml
<dependency>
  <groupId>com.socketio4j</groupId>
  <artifactId>netty-socketio-core</artifactId>
  <version>{$socketio.core.version}</version>
</dependency>
```
{% endcode %}
{% endtab %}

{% tab title="Gradle - Groovy DSL" %}
{% code title="build.gradle" %}
```groovy
dependencies {
    implementation "com.socketio4j:netty-socketio-core:${socketioCoreVersion}"
}
```
{% endcode %}
{% endtab %}

{% tab title="Gradle - Kotlin DSL" %}
{% code title="build.gradle.kts" %}
```kts
dependencies {
    implementation("com.socketio4j:netty-socketio-quarkus:$socketioCoreVersion")
}
```
{% endcode %}
{% endtab %}
{% endtabs %}

### Spring

{% tabs %}
{% tab title="Maven" %}
{% code title="pom.xml" %}
```xml
<dependency>
  <groupId>com.socketio4j</groupId>
  <artifactId>netty-socketio-spring</artifactId>
  <version>{$socketio.spring.version}</version>
</dependency>
```
{% endcode %}
{% endtab %}

{% tab title="Gradle - Groovy DSL" %}
{% code title="build.gradle" %}
```groovy
dependencies {
    implementation "com.socketio4j:netty-socketio-spring:${socketioSpringVersion}"
}
```
{% endcode %}
{% endtab %}

{% tab title="Gradle - Kotlin DSL" %}
{% code title="build.gradle.kts" %}
```kts
dependencies {
    implementation("com.socketio4j:netty-socketio-spring:$socketioSpringVersion")
}
```
{% endcode %}
{% endtab %}
{% endtabs %}

### Spring Boot

{% tabs %}
{% tab title="Maven" %}
{% code title="pom.xml" %}
```xml
<dependency>
  <groupId>com.socketio4j</groupId>
  <artifactId>netty-socketio-spring-boot-starter</artifactId>
  <version>{$socketio.spring-boot-starter.version}</version>
</dependency>
```
{% endcode %}
{% endtab %}

{% tab title="Gradle - Groovy DSL" %}
{% code title="build.gradle" %}
```groovy
dependencies {
    implementation "com.socketio4j:netty-socketio-spring-boot-starter:${socketioSpringBootStarterVersion}"
}
```
{% endcode %}
{% endtab %}

{% tab title="Gradle - Kotlin DSL" %}
{% code title="build.gradle.kts" %}
```kts
dependencies {
    implementation("com.socketio4j:netty-socketio-spring-boot-starter:$socketioSpringBootStarterVersion")
}
```
{% endcode %}
{% endtab %}
{% endtabs %}

### Quarkus

{% tabs %}
{% tab title="Maven" %}
{% code title="pom.xml" %}
```xml
<dependency>
  <groupId>com.socketio4j</groupId>
  <artifactId>netty-socketio-quarkus</artifactId>
  <version>{$socketio.quarkus.version}</version>
</dependency>
```
{% endcode %}
{% endtab %}

{% tab title="Gradle - Groovy DSL" %}
{% code title="build.gradle" %}
```groovy
dependencies {
    implementation "com.socketio4j:netty-socketio-quarkus:${socketioQuarkusVersion}"
}
```
{% endcode %}
{% endtab %}

{% tab title="Gradle - Kotlin DSL" %}
{% code title="build.gradle.kts" %}
```kts
dependencies {
    implementation("com.socketio4j:netty-socketio-quarkus:$socketioQuarkusVersion")
}
```
{% endcode %}
{% endtab %}
{% endtabs %}

### Micronaut

{% tabs %}
{% tab title="Maven" %}
{% code title="pom.xml" %}
```xml
<dependency>
  <groupId>com.socketio4j</groupId>
  <artifactId>netty-socketio-micronaut</artifactId>
  <version>{$socketio.micronaut.version}</version>
</dependency>
```
{% endcode %}
{% endtab %}

{% tab title="Gradle - Groovy DSL" %}
{% code title="build.gradle" %}
```groovy
dependencies {
    implementation "com.socketio4j:netty-socketio-micronaut:${socketioMicronautVersion}"
}
```
{% endcode %}
{% endtab %}

{% tab title="Gradle - Kotlin DSL" %}
{% code title="build.gradle.kts" %}
```kts
dependencies {
    implementation("com.socketio4j:netty-socketio-micronaut:$socketioMicronautVersion")
}
```
{% endcode %}
{% endtab %}
{% endtabs %}

