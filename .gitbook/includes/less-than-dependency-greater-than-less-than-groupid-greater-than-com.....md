---
title: <dependency>  <groupId>com....
---

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
