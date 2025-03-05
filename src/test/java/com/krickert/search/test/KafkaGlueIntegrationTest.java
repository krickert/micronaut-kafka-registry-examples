package com.krickert.search.test;

import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryKafkaDeserializer;
import com.amazonaws.services.schemaregistry.serializers.GlueSchemaRegistryKafkaSerializer;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import com.krickert.search.model.pipe.PipeDoc;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.glue.model.Compatibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@MicronautTest(environments = "test", transactional = false)
public class KafkaGlueIntegrationTest extends AbstractMotoTest implements TestPropertyProvider {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaGlueIntegrationTest.class);
    private static final String TOPIC = "test-pipedoc";

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.7.2")
    );

    @Inject
    PipeDocProducer producer;

    @Inject
    TestPipeDocConsumer consumer;

    @BeforeAll
    static void setup() {
        // Set the bootstrap servers property for Micronaut to use
        // Set AWS region for Glue Schema Registry
        System.setProperty("aws.region", "us-east-1");

        // Set Mock AWS endpoint if needed (from AbstractMotoTest)
        log.info("AWS endpoint: {}", getMotoEndpoint());
        System.setProperty("aws.endpoint", getMotoEndpoint());

        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
        System.setProperty("aws.sessionToken", "test-session");

    }


    @Test
    void testProduceAndConsumeMessage() throws Exception {
        // Create test PipeDoc
        PipeDoc testDoc = PipeDocExample.createFullPipeDoc();
        
        // Produce the message
        producer.sendPipeDoc(testDoc).get(10, TimeUnit.SECONDS);
        LOG.info("Produced test document: {}", testDoc);

        // Wait for the consumer to receive the message
        PipeDoc receivedDoc = consumer.getNextMessage(10);
        LOG.info("Received document: {}", receivedDoc);

        // Verify the received message
        assertNotNull(receivedDoc, "Should have received a message");
        assertEquals(testDoc, receivedDoc, "Received message should match sent message");
    }

    /**
     * Allows dynamically providing properties for a test.
     *
     * @return A map of properties
     */
    @Override
    public @NonNull Map<String, String> getProperties() {
        String producerPrefix = "kafka.producers.default.";
        Map<String, String> props = new HashMap<>();
        props.put(producerPrefix + ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(producerPrefix + ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(producerPrefix + ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GlueSchemaRegistryKafkaSerializer.class.getName());
        props.put(producerPrefix + AWSSchemaRegistryConstants.AWS_REGION, "us-east-1");
        props.put(producerPrefix + AWSSchemaRegistryConstants.AWS_ENDPOINT, getMotoEndpoint());
        props.put(producerPrefix + AWSSchemaRegistryConstants.REGISTRY_NAME, "default");
        props.put(producerPrefix + AWSSchemaRegistryConstants.DATA_FORMAT, "PROTOBUF");
        props.put(producerPrefix + AWSSchemaRegistryConstants.PROTOBUF_MESSAGE_TYPE, "POJO");
        props.put(producerPrefix + AWSSchemaRegistryConstants.COMPATIBILITY_SETTING, "FULL");
        props.put(producerPrefix + AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, "true");

        String consumerPrefix = "kafka.consumers.default.";

        props.put(consumerPrefix + ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(consumerPrefix + ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(consumerPrefix + ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(consumerPrefix + ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(consumerPrefix + ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, GlueSchemaRegistryKafkaDeserializer.class.getName());
        props.put(consumerPrefix + AWSSchemaRegistryConstants.AWS_REGION, "us-east-1");
        props.put(consumerPrefix + AWSSchemaRegistryConstants.AWS_ENDPOINT, getMotoEndpoint());
        props.put(consumerPrefix + AWSSchemaRegistryConstants.REGISTRY_NAME, "default");
        props.put(consumerPrefix + AWSSchemaRegistryConstants.DATA_FORMAT, "PROTOBUF");
        props.put(consumerPrefix + AWSSchemaRegistryConstants.PROTOBUF_MESSAGE_TYPE, "POJO");
        props.put(consumerPrefix + AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, "true");
        props.put(consumerPrefix + AWSSchemaRegistryConstants.COMPATIBILITY_SETTING, "FULL");
        return props;
    }

    // Producer client
    @KafkaClient
    public interface PipeDocProducer {
        @Topic(TOPIC)
        CompletableFuture<Void> sendPipeDoc(PipeDoc pipeDoc);
    }

    // Consumer implementation
    @KafkaListener(groupId = "test-group")
    public static class TestPipeDocConsumer {
        private final List<PipeDoc> receivedMessages = new ArrayList<>();
        private final CompletableFuture<PipeDoc> nextMessage = new CompletableFuture<>();

        @Topic(TOPIC)
        void receive(PipeDoc pipeDoc) {
            LOG.info("Received message: {}", pipeDoc);
            synchronized (receivedMessages) {
                receivedMessages.add(pipeDoc);
                nextMessage.complete(pipeDoc);
            }
        }

        public PipeDoc getNextMessage(long timeoutSeconds) throws Exception {
            return nextMessage.get(timeoutSeconds, TimeUnit.SECONDS);
        }

        public List<PipeDoc> getReceivedMessages() {
            synchronized (receivedMessages) {
                return new ArrayList<>(receivedMessages);
            }
        }
    }
}