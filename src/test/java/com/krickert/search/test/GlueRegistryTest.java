package com.krickert.search.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.GetDatabasesRequest;
import software.amazon.awssdk.services.glue.model.GetDatabasesResponse;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@Testcontainers
public class GlueRegistryTest {
    private static final Logger log = LoggerFactory.getLogger(GlueRegistryTest.class);
    // Use a Moto container with a command that starts the Glue service.
    // We specify the command arguments explicitly and use a log-based wait strategy.
    @Container
    public static GenericContainer<?> motoGlue = new GenericContainer<>(DockerImageName.parse("motoserver/moto:latest"))
            .withExposedPorts(5000)
            .withAccessToHost(true)
            .withCommand("-H0.0.0.0")
            .withEnv(Map.of(
                    "MOTO_SERVICE", "glue",
                    "TEST_SERVER_MODE", "true"
            ))
            .withStartupTimeout(Duration.ofSeconds(30))
            .withReuse(true);


    @BeforeAll
    public static void setup() {
        // Set AWS credentials via system properties so that the AWS SDK v2 default chain picks them up.
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
        System.setProperty("aws.sessionToken", "test-session");

        // Compute and set the Glue endpoint based on the container’s mapped port.
        String endpoint = "http://" + motoGlue.getHost() + ":" + motoGlue.getMappedPort(5000);
        System.setProperty("aws.glue.endpoint", endpoint);
        log.info("Using Moto Glue endpoint: " + endpoint);
    }

    @Test
    public void testGlueRegistryWithDefaultCredentials() {
        // Retrieve the endpoint from system properties.
        String endpoint = System.getProperty("aws.glue.endpoint");
        Assertions.assertNotNull(endpoint, "The AWS Glue endpoint must be set");

        // Build the Glue client using the default credentials provider chain.
        GlueClient glueClient = GlueClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .build();
        Assertions.assertNotNull(glueClient, "GlueClient should have been created");

        // Attempt to call the getDatabases operation.
        // Note: Moto’s Glue service may not fully implement this API,
        // so an exception here might be expected.
        try {
            GetDatabasesResponse response = glueClient.getDatabases(GetDatabasesRequest.builder().build());
            log.info("Glue getDatabases succeeded: " + response);
        } catch (Exception e) {
            Assertions.fail("Glue getDatabases operation resulted in an exception", e);
        }
    }
}
