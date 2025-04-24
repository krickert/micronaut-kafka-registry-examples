
rootProject.name="micronaut-kafka-registry"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://repo.micronaut.io/releases") }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Add JBoss Maven repository for Apicurio artifacts
        maven { url = uri("https://repository.jboss.org/nexus/content/repositories/releases") }
        maven { url = uri("https://repository.jboss.org/nexus/content/groups/public") }
    }
}

// Include BOM and core projects
include("bom")
include("micronaut-kafka-registry-core")
include("micronaut-kafka-registry-moto")
include("micronaut-kafka-registry-apicurio")
