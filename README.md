# xinstrument

java agent loader wrapper

## maven dependency

```xml
<dependency>
	<groupId>xyz.xkrivzooh</groupId>
	<artifactId>instruments</artifactId>
	<version>${instruments.version></version>
</dependency>
```

## usage

### static attach

```java
java -javaagent:path-to-instrument-agent.jar/instrument-agent.jar app.jar
```

### dynamic attach

when application start, you can use following code to load agent:

```java
try {
    AgentLoader.loadAgent();
}
catch (Exception e) {
    logger.error("instrument agent load error", e);
    throw new RuntimeException(e);
}
```

if you want to get `Instrumentation` instance, you can use following code:


```java
AgentLoader.getInstrumentation()
```






