package com.krickert.search.test;

import com.amazonaws.services.schemaregistry.utils.AWSSchemaRegistryConstants;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.order.Ordered;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.CreateRegistryRequest;
import software.amazon.awssdk.services.glue.model.CreateRegistryResponse;
import software.amazon.awssdk.services.glue.model.EntityNotFoundException;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Requires(env = "test")
@Order(Ordered.HIGHEST_PRECEDENCE)
public abstract class AbstractMotoTest {
    protected static final Logger log = LoggerFactory.getLogger(AbstractMotoTest.class);
    private static final String endpoint;

    // Static initialization of the Moto container
    private static final GenericContainer<?> motoGlue;

    static {
        motoGlue = new GenericContainer<>(DockerImageName.parse("motoserver/moto:latest"))
                .withExposedPorts(5000)
                .withAccessToHost(true)
                .withCommand("-H0.0.0.0")
                .withEnv(Map.of(
                        "MOTO_SERVICE", "glue",
                        "TEST_SERVER_MODE", "true"
                ))
                .withStartupTimeout(Duration.ofSeconds(30))
                .withReuse(true); // Changed to true for singleton behavior

        motoGlue.start();

        // Set the endpoint once during initialization
        endpoint = "http://" + motoGlue.getHost() + ":" + motoGlue.getMappedPort(5000);

        // Initialize the registry
        initializeRegistry();
    }

    private static void initializeRegistry() {
        GlueClient glueClient = GlueClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();

        try {
            CreateRegistryRequest createRegistryRequest = CreateRegistryRequest.builder()
                    .registryName("default")
                    .description("Default registry for integration tests")
                    .build();
            CreateRegistryResponse response = glueClient.createRegistry(createRegistryRequest);
            log.info("Registry created: {}", response.registryArn());
        } catch (EntityNotFoundException e) {
            log.info("Registry 'default' already exists.");
        } catch (Exception e) {
            log.error("Failed to create registry: {}", e.getMessage());
            throw new RuntimeException("Registry creation failed", e);
        }
    }

    public static String getMotoEndpoint() {
        return endpoint;
    }

    @BeforeEach
    public void setupProperties() {
        updateProperties(endpoint);
    }

    private void updateProperties(String endpoint) {
        Map<String,String> properties = new HashMap<>();
        properties.putAll(Map.of(
                "aws.accessKeyId", "test",
                "aws.secretAccessKey", "test",
                "aws.sessionToken", "test-session",
                AWSSchemaRegistryConstants.AWS_ENDPOINT, endpoint,
                "aws.endpoint", endpoint,
                "kafka.consumers.default.aws.endpoint", endpoint,
                "kafka.producers.default.aws.endpoint", endpoint,
                "software.amazon.awssdk.regions.region", "us-east-1",
                "aws.region", "us-east-1",
                "software.amazon.awssdk.endpoints.endpoint-url", endpoint));
        properties.putAll(
                Map.of("software.amazon.awssdk.glue.endpoint", endpoint,
                "software.amazon.awssdk.glue.endpoint-url", endpoint,
                "aws.glue.endpoint", endpoint,
                "aws.serviceEndpoint", endpoint,
                "aws.endpointUrl", endpoint,
                "aws.endpointDiscoveryEnabled", "false"
                ));

        properties.forEach(System::setProperty);
    }
}