plugins {
    id("io.micronaut.library") version "4.5.0"
    id("io.micronaut.test-resources") version "4.5.0"
    id("com.google.protobuf") version "0.9.5"
    `maven-publish`
}

dependencies {
    // Annotation processors
    annotationProcessor(libs.micronaut.serde.processor)
    annotationProcessor(libs.lombok)

    // Schema Registry Interface
    api(project(":micronaut-kafka-registry-core"))

    // Micronaut
    implementation(libs.micronaut.discovery.core)
    implementation(libs.micronaut.test.core)
    implementation(libs.micronaut.test.junit5)
    implementation(libs.micronaut.grpc.runtime)
    implementation(libs.micronaut.serde.jackson)
    implementation(libs.micronaut.kafka)
    implementation(libs.micronaut.test.resources.kafka)

    // Apicurio Registry
    // https://mvnrepository.com/artifact/io.apicurio/apicurio-registry-protobuf-serde-kafka
    implementation("io.apicurio:apicurio-registry-protobuf-serde-kafka:3.0.6")

    // Protobuf and gRPC
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.java.util)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)

    // Testing
    implementation(libs.junit.jupiter)
    implementation(libs.testcontainers)
    implementation(libs.testcontainers.kafka)
    testImplementation(libs.assertj.core)
    testImplementation(libs.awaitility)

    // Other
    implementation(libs.javax.annotation.api)
    implementation(libs.jakarta.annotation.api)
    compileOnly(libs.lombok)
    runtimeOnly(libs.logback.classic)
}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
    targetCompatibility = JavaVersion.toVersion("21")
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("kafka.registry.apicurio.*")
    }
    testResources {
        sharedServer = true
    }
}

sourceSets {
    test {
        java {
            srcDirs("build/generated/source/proto/main/grpc")
            srcDirs("build/generated/source/proto/main/java")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Micronaut Kafka Registry Apicurio")
                description.set("Apicurio implementation for Micronaut Kafka Registry")
                url.set("https://github.com/krickert/micronaut-kafka-registry")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("krickert")
                        name.set("Kristian Rickert")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/krickert/micronaut-kafka-registry.git")
                    developerConnection.set("scm:git:ssh://github.com/krickert/micronaut-kafka-registry.git")
                    url.set("https://github.com/krickert/micronaut-kafka-registry")
                }
            }
        }
    }
}
