// Root build.gradle.kts
allprojects {
    group = "com.krickert.search.test"
    version = "1.0.0-SNAPSHOT"
    
    repositories {
        mavenLocal()
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
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
}

tasks.register("publishAll") {
    dependsOn(":micronaut-moto-glue-msk-bom:publish")
    dependsOn(":micronaut-kafka-test-core:publish")
}