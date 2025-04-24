plugins {
    id("io.micronaut.library") version "4.5.0"
    `maven-publish`
}

dependencies {
    // Micronaut
    implementation(libs.micronaut.test.core)
    implementation(libs.micronaut.test.junit5)
    
    // Annotation processors
    annotationProcessor(libs.micronaut.serde.processor)
    
    // Other
    implementation(libs.javax.annotation.api)
    implementation(libs.jakarta.annotation.api)
}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
    targetCompatibility = JavaVersion.toVersion("21")
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("kafka.registry.*")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Micronaut Moto Glue MSK Schema Registry")
                description.set("Schema Registry interface for Micronaut Moto Glue MSK")
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
}