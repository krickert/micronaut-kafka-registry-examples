
rootProject.name="micronaut-moto-glue-msk"

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
include("micronaut-moto-glue-msk-core")
include("micronaut-moto-glue-msk-schema-registry")
