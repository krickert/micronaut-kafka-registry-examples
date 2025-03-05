package com.krickert.search.test;

import com.amazonaws.services.schemaregistry.deserializers.GlueSchemaRegistryKafkaDeserializer;
import com.amazonaws.services.schemaregistry.serializers.GlueSchemaRegistryKafkaSerializer;
import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import com.krickert.search.model.pipe.PipeDoc;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.glue.model.Compatibility;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Testcontainers
public class KafkaGlueSerdesIntegrationTest extends AbstractMotoTest {

    public static final Logger log = LoggerFactory.getLogger(KafkaGlueSerdesIntegrationTest.class);

    // Use the vanilla Apache Kafka image through Testcontainers' KafkaContainer.
    @Container
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.2"));

    @Test
    public void testKafkaProducerConsumerWithGlueSerdes() {
        // Log current configuration.
        log.info("Kafka bootstrap servers: {}", kafka.getBootstrapServers());
        log.info("AWS Glue endpoint from system property: {}", System.getProperty(AWSSchemaRegistryConstants.AWS_ENDPOINT));

        // Configure Kafka Producer properties.
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GlueSchemaRegistryKafkaSerializer.class.getName());
        producerProps.put(AWSSchemaRegistryConstants.AWS_REGION, "us-east-1");
        producerProps.put(AWSSchemaRegistryConstants.AWS_ENDPOINT, System.getProperty(AWSSchemaRegistryConstants.AWS_ENDPOINT));
        producerProps.put(AWSSchemaRegistryConstants.REGISTRY_NAME, "default");
        producerProps.put(AWSSchemaRegistryConstants.DATA_FORMAT, "PROTOBUF");
        producerProps.put(AWSSchemaRegistryConstants.PROTOBUF_MESSAGE_TYPE, "POJO");
        producerProps.put(AWSSchemaRegistryConstants.COMPATIBILITY_SETTING, Compatibility.FULL);
        producerProps.put(AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, "true");

        KafkaProducer<String, PipeDoc> producer = new KafkaProducer<>(producerProps);

        // Configure Kafka Consumer properties.
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, GlueSchemaRegistryKafkaDeserializer.class.getName());
        consumerProps.put(AWSSchemaRegistryConstants.AWS_REGION, "us-east-1");
        consumerProps.put(AWSSchemaRegistryConstants.AWS_ENDPOINT, System.getProperty(AWSSchemaRegistryConstants.AWS_ENDPOINT));
        consumerProps.put(AWSSchemaRegistryConstants.REGISTRY_NAME, "default");
        consumerProps.put(AWSSchemaRegistryConstants.DATA_FORMAT, "PROTOBUF");
        consumerProps.put(AWSSchemaRegistryConstants.PROTOBUF_MESSAGE_TYPE, "POJO");
        consumerProps.put(AWSSchemaRegistryConstants.SCHEMA_AUTO_REGISTRATION_SETTING, "true");
        consumerProps.put(AWSSchemaRegistryConstants.COMPATIBILITY_SETTING, Compatibility.FULL);

        KafkaConsumer<String, PipeDoc> consumer = new KafkaConsumer<>(consumerProps);
        String topic = "test-pipedoc";
        consumer.subscribe(Collections.singletonList(topic));

        // Create a Timestamp Protobuf message as payload.
        PipeDoc pipeDoc = PipeDocExample.createFullPipeDoc();
        log.info("Original Timestamp payload: {}", pipeDoc);

        // Produce a record to Kafka.
        ProducerRecord<String, PipeDoc> record = new ProducerRecord<>(topic, "key1", pipeDoc);
        try {
            producer.send(record).get();  // Wait for acknowledgment
            log.info("Record sent successfully.");
        } catch (Exception e) {
            log.error("Error sending record", e);
        }
        producer.flush();

        // Poll for records from Kafka.
        ConsumerRecords<String, PipeDoc> records = consumer.poll(Duration.ofSeconds(10));
        if (records.isEmpty()) {
            log.warn("No records received from Kafka!");
        }
        Assertions.assertFalse(records.isEmpty(), "Consumer should receive at least one record");

        records.forEach(r -> {
            log.info("Received record with key: {} and value: {}", r.key(), r.value());
            Object value = r.value();
            PipeDoc deserializedPipeDoc;
            if (value != null) {
                deserializedPipeDoc = (PipeDoc) value;
            } else {
               throw new RuntimeException("Failed to unpack Any to PipeDoc: " + value);
            }
            // Now compare fields individually.
            Assertions.assertEquals(pipeDoc, deserializedPipeDoc, "Docs should be the same");
        });

        producer.close();
        consumer.close();
    }

}
