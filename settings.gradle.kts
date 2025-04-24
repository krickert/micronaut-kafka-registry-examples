
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
    }
}

// Include BOM and core projects
include("bom")
include("micronaut-kafka-registry-core")
include("micronaut-kafka-registry-moto")
