plugins {
    `java-platform`
    `maven-publish`
}

group = "com.krickert.search.test"
version = "1.0.0-SNAPSHOT"

javaPlatform {
    allowDependencies()
}

val micronautVersion: String by project
val protobufVersion: String by project
val grpcVersion: String by project
val glueSchemaRegistryVersion: String by project
val mskIamAuthVersion: String by project
val awsSdkVersion: String by project
val awaitilityVersion: String by project

dependencies {
    // Define the constraints for all dependencies that should be part of the BOM
    constraints {
        // Core project
        api(project(":"))

        // Schema Registry Interface
        api(project(":micronaut-moto-glue-msk-schema-registry"))

        // AWS dependencies
        api("software.amazon.glue:schema-registry-serde:$glueSchemaRegistryVersion")
        api("software.amazon.msk:aws-msk-iam-auth:$mskIamAuthVersion")
        api("software.amazon.awssdk:url-connection-client:$awsSdkVersion")

        // Protobuf and gRPC
        api("com.google.protobuf:protobuf-java:$protobufVersion")
        api("com.google.protobuf:protobuf-java-util:$grpcVersion")
        api("io.grpc:grpc-protobuf:$grpcVersion")
        api("io.grpc:grpc-stub:$grpcVersion")

        // Micronaut
        api("io.micronaut:micronaut-discovery-core")
        api("io.micronaut.test:micronaut-test-core:$micronautVersion")
        api("io.micronaut.test:micronaut-test-junit5")
        api("io.micronaut.grpc:micronaut-grpc-runtime")
        api("io.micronaut.serde:micronaut-serde-jackson")
        api("io.micronaut.aws:micronaut-aws-sdk-v2")
        api("io.micronaut.kafka:micronaut-kafka")
        api("io.micronaut.testresources:micronaut-test-resources-kafka")

        // Testing
        api("org.testcontainers:junit-jupiter")
        api("org.testcontainers:testcontainers")
        api("org.testcontainers:kafka")
        api("org.awaitility:awaitility:$awaitilityVersion")
        api("org.assertj:assertj-core")
    }
}

publishing {
    publications {
        create<MavenPublication>("bomPublication") {
            from(components["javaPlatform"])

            pom {
                name.set("Micronaut Moto Glue MSK BOM")
                description.set("Bill of Materials for Micronaut Moto Glue MSK")
                url.set("https://github.com/yourusername/micronaut-moto-glue-msk")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("krickert")
                        name.set("Kevin Rickert")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/yourusername/micronaut-moto-glue-msk.git")
                    developerConnection.set("scm:git:ssh://github.com/yourusername/micronaut-moto-glue-msk.git")
                    url.set("https://github.com/yourusername/micronaut-moto-glue-msk")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/yourusername/micronaut-moto-glue-msk")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
