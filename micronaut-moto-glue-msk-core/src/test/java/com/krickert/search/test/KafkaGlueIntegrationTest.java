package com.krickert.search.test;

import com.krickert.search.model.pipe.PipeDoc;
import kafka.AbstractKafkaTest;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@MicronautTest(environments = "test", transactional = false)
public class KafkaGlueIntegrationTest extends AbstractKafkaTest {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaGlueIntegrationTest.class);
    private static final String TOPIC = "test-pipedoc";

    @Inject
    PipeDocProducer producer;

    @Inject
    TestPipeDocConsumer consumer;

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
