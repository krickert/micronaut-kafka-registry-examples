plugins {
    id("io.micronaut.library") version "4.5.0"
    id("io.micronaut.test-resources") version "4.5.0"
    id("com.google.protobuf") version "0.9.5"
}
var protobufVersion = "3.25.6"
var grpcVersion = "1.72.0"

version = "1.0.0-SNAPSHOT"
group = "com.krickert.search.test"

repositories {
    mavenLocal()
    maven ("https://maven-central.storage-download.googleapis.com/maven2/")
    mavenCentral()
    maven {
        url = uri("https://gitlab.com/api/v4/groups/91411712/-/packages/maven")
        name = "GitLab Group Repository"
        credentials(HttpHeaderCredentials::class) {
            name = "Private-Token"
            value = project.findProperty("gitLabPrivateToken") as String? ?: System.getenv("GITLAB_TOKEN")
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

dependencies {
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")

    implementation("io.micronaut:micronaut-discovery-core")
    implementation("io.micronaut.grpc:micronaut-grpc-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("javax.annotation:javax.annotation-api")

    implementation("io.micronaut.aws:micronaut-aws-sdk-v2")
    implementation("io.micronaut.kafka:micronaut-kafka")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("jakarta.annotation:jakarta.annotation-api")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("ch.qos.logback:logback-classic")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.awaitility:awaitility:4.2.2")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:testcontainers")
    //AWS dependencies
    // https://mvnrepository.com/artifact/software.amazon.glue/schema-registry-serde
    implementation("software.amazon.glue:schema-registry-serde:1.1.23")
    // https://mvnrepository.com/artifact/software.amazon.msk/aws-msk-iam-auth
    implementation("software.amazon.msk:aws-msk-iam-auth:2.2.0")
    // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java-util
    implementation("com.google.protobuf:protobuf-java-util:${grpcVersion}")
    implementation("io.grpc:grpc-protobuf:${grpcVersion}")
    implementation("io.grpc:grpc-stub:${grpcVersion}")
    implementation("com.google.protobuf:protobuf-java:${protobufVersion}")
    testImplementation("org.testcontainers:kafka")
    // https://mvnrepository.com/artifact/io.micronaut.testresources/micronaut-test-resources-kafka
    implementation("io.micronaut.testresources:micronaut-test-resources-kafka")
// https://mvnrepository.com/artifact/software.amazon.awssdk/url-connection-client
    testImplementation("software.amazon.awssdk:url-connection-client:2.30.31")

}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
    targetCompatibility = JavaVersion.toVersion("21")
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.krickert.search.test.*")
    }
    testResources {
        sharedServer = true
    }
}

sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/grpc")
            srcDirs("build/generated/source/proto/main/java")
        }
    }
}
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protobufVersion}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
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
