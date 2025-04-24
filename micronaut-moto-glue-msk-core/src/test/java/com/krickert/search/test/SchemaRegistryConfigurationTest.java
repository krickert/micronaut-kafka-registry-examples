package com.krickert.search.test;

import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import kafka.registry.SchemaRegistry;
import kafka.registry.moto.MotoSchemaRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that the SchemaRegistry is properly loaded and configured.
 */
@MicronautTest
public class SchemaRegistryConfigurationTest {
    private static final Logger log = LoggerFactory.getLogger(SchemaRegistryConfigurationTest.class);

    @Inject
    private SchemaRegistry schemaRegistry;

    @Test
    void testDefaultSchemaRegistryIsMoto() {
        log.info("[DEBUG_LOG] Testing default SchemaRegistry configuration");
        assertNotNull(schemaRegistry, "SchemaRegistry should not be null");
        assertTrue(schemaRegistry instanceof MotoSchemaRegistry, 
                "Default SchemaRegistry should be MotoSchemaRegistry, but was: " + schemaRegistry.getClass().getName());
        log.info("[DEBUG_LOG] Default SchemaRegistry is: {}", schemaRegistry.getClass().getName());
    }

    @Test
    void testSchemaRegistryConfiguration() {
        log.info("[DEBUG_LOG] Testing SchemaRegistry configuration with explicit type");
        
        // Create a new ApplicationContext with explicit configuration
        Map<String, Object> config = new HashMap<>();
        config.put("schema.registry.type", "moto");
        
        ApplicationContext context = ApplicationContext.builder()
                .properties(config)
                .build();
        context.start();
        
        try {
            SchemaRegistry registry = context.getBean(SchemaRegistry.class);
            assertNotNull(registry, "SchemaRegistry should not be null");
            assertTrue(registry instanceof MotoSchemaRegistry, 
                    "Configured SchemaRegistry should be MotoSchemaRegistry, but was: " + registry.getClass().getName());
            log.info("[DEBUG_LOG] Configured SchemaRegistry is: {}", registry.getClass().getName());
        } finally {
            context.close();
        }
    }
}