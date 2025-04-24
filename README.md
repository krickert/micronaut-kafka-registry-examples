# Micronaut Kafka Registry Testing Framework

A comprehensive testing framework for Kafka with schema registries in Micronaut applications. This library makes it easy to test Kafka applications that use schema registries without needing actual cloud resources.

## Overview

When developing applications that use Kafka with schema registries, testing can be challenging. This library provides a set of abstractions and concrete implementations to simplify testing by:

1. Providing a unified interface for different schema registry implementations
2. Using TestContainers to spin up the necessary infrastructure locally
3. Offering base test classes that handle common testing scenarios
4. Supporting multiple schema registry implementations (Apicurio, AWS Glue via Moto)
5. Working seamlessly with Micronaut's testing framework

## Features

- **Schema Registry Abstraction**: Common interface for different schema registry implementations
- **Kafka Integration Testing**: Base classes for testing Kafka producers and consumers
- **Multiple Registry Implementations**:
  - Apicurio Registry
  - AWS Glue Schema Registry (via Moto emulation)
- **Protobuf Support**: Full support for Protobuf serialization/deserialization
- **TestContainers Integration**: Automatic container management for testing
- **Micronaut Integration**: Designed to work with Micronaut's testing framework

## Modules

- **micronaut-kafka-registry-core**: Core abstractions and base test classes
- **micronaut-kafka-registry-apicurio**: Apicurio Registry implementation
- **micronaut-kafka-registry-moto**: AWS Glue Schema Registry implementation using Moto

## Installation

Add the following to your `build.gradle.kts` file:

```kotlin
repositories {
    mavenCentral()
    // Add if you need the Apicurio artifacts
    maven { url = uri("https://repository.jboss.org/nexus/content/repositories/releases") }
    maven { url = uri("https://repository.jboss.org/nexus/content/groups/public") }
}

dependencies {
    // For Apicurio Registry support
    testImplementation("com.krickert.search.test:micronaut-kafka-registry-apicurio:1.0.0-SNAPSHOT")

    // For AWS Glue Schema Registry support via Moto
    testImplementation("com.krickert.search.test:micronaut-kafka-registry-moto:1.0.0-SNAPSHOT")
}
```

Or with Maven:

```xml
<dependencies>
    <!-- For Apicurio Registry support -->
    <dependency>
        <groupId>com.krickert.search.test</groupId>
        <artifactId>micronaut-kafka-registry-apicurio</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <scope>test</scope>
    </dependency>

    <!-- For AWS Glue Schema Registry support via Moto -->
    <dependency>
        <groupId>com.krickert.search.test</groupId>
        <artifactId>micronaut-kafka-registry-moto</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Usage Examples

### Basic Schema Registry Test

```java
import com.krickert.search.test.registry.AbstractSchemaRegistryLifecycleTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MySchemaRegistryTest extends AbstractSchemaRegistryLifecycleTest {

    @Test
    public void testCustomRegistryFunctionality() {
        // The registry is already started and available via the schemaRegistry field
        assertTrue(schemaRegistry.isRunning());

        // Use the registry endpoint for your custom tests
        String endpoint = schemaRegistry.getEndpoint();

        // Your custom test logic here
    }
}
```

### Kafka Integration Test with Schema Registry

```java
import com.krickert.search.test.kafka.AbstractKafkaIntegrationTest;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MyKafkaTest extends AbstractKafkaIntegrationTest<MyMessage> {

    @Inject
    private MyProducer producer;

    @Inject
    private MyConsumer consumer;

    @Override
    protected MyMessage createTestMessage() {
        return new MyMessage("test-id", "Hello, Kafka!");
    }

    @Override
    protected MessageProducer<MyMessage> getProducer() {
        return producer::sendMessage;
    }

    @Override
    protected MessageConsumer<MyMessage> getConsumer() {
        return consumer;
    }

    // Additional custom tests
    @Test
    void testCustomKafkaFunctionality() {
        // Your custom test logic here
    }

    // Producer implementation
    @Singleton
    @KafkaClient
    public static class MyProducer {
        @Topic("test-message")
        public CompletableFuture<Void> sendMessage(MyMessage message) {
            return CompletableFuture.completedFuture(null);
        }
    }

    // Consumer implementation
    @Singleton
    @KafkaListener(groupId = "test-group")
    public static class MyConsumer implements MessageConsumer<MyMessage> {
        private final List<MyMessage> receivedMessages = new ArrayList<>();

        @Topic("test-message")
        public void receive(MyMessage message) {
            receivedMessages.add(message);
        }

        @Override
        public MyMessage getNextMessage(long timeoutSeconds) throws Exception {
            // Implementation to wait for and return the next message
            // ...
            return receivedMessages.getLast();
        }

        @Override
        public List<MyMessage> getReceivedMessages() {
            return receivedMessages;
        }
    }
}
```

### Using Apicurio Registry

The Apicurio Registry implementation will be automatically injected if you include the `micronaut-kafka-registry-apicurio` dependency and have the `apicurio/apicurio-registry` Docker image available.

```java
import com.krickert.search.test.registry.AbstractSchemaRegistryLifecycleTest;
import org.junit.jupiter.api.Test;

public class ApicurioRegistryTest extends AbstractSchemaRegistryLifecycleTest {

    @Test
    public void testApicurioSpecificFunctionality() {
        // The Apicurio registry is automatically injected and started
        assertEquals("apicurio", schemaRegistry.getRegistryName());

        // Use the registry for Apicurio-specific tests
        // ...
    }
}
```

### Using AWS Glue Schema Registry (via Moto)

The AWS Glue Schema Registry implementation (via Moto) will be automatically injected if you include the `micronaut-kafka-registry-moto` dependency and have the `motoserver/moto` Docker image available.

```java
import com.krickert.search.test.registry.AbstractSchemaRegistryLifecycleTest;
import org.junit.jupiter.api.Test;

public class AwsGlueRegistryTest extends AbstractSchemaRegistryLifecycleTest {

    @Test
    public void testAwsGlueSpecificFunctionality() {
        // The AWS Glue registry (via Moto) is automatically injected and started
        assertEquals("default", schemaRegistry.getRegistryName());

        // Use the registry for AWS Glue-specific tests
        // ...
    }
}
```

## Configuration

### Apicurio Registry Configuration

The Apicurio Registry is configured with in-memory storage by default. You can customize it by extending the `ApicurioSchemaRegistry` class:

```java
import com.krickert.search.test.apicurio.ApicurioSchemaRegistry;
import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class CustomApicurioRegistry extends ApicurioSchemaRegistry {
    @Override
    public Map<String, String> getProperties() {
        Map<String, String> props = super.getProperties();
        // Add or modify properties
        props.put("custom.property", "custom-value");
        return props;
    }
}
```

### AWS Glue Schema Registry Configuration

The AWS Glue Schema Registry (via Moto) is configured with default test credentials. You can customize it by extending the `MotoSchemaRegistry` class:

```java
import com.krickert.search.test.moto.MotoSchemaRegistry;
import jakarta.inject.Singleton;
import java.util.Map;

@Singleton
public class CustomMotoRegistry extends MotoSchemaRegistry {
    @Override
    public Map<String, String> getProperties() {
        Map<String, String> props = super.getProperties();
        // Add or modify properties
        props.put("custom.property", "custom-value");
        return props;
    }
}
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## References

- [Micronaut Documentation](https://docs.micronaut.io)
- [Micronaut Kafka Documentation](https://micronaut-projects.github.io/micronaut-kafka/latest/guide/index.html)
- [Apicurio Registry](https://www.apicur.io/registry/)
- [AWS Glue Schema Registry](https://docs.aws.amazon.com/glue/latest/dg/schema-registry.html)
- [Moto](https://github.com/spulec/moto)
- [TestContainers](https://www.testcontainers.org/)
