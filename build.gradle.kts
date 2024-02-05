plugins {
    java
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

allprojects {
    group = "dev.thomazz.pledge"
    version = "3.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
}

subprojects {
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.6")
        annotationProcessor("org.projectlombok:lombok:1.18.30")
        compileOnly("io.netty:netty-all:4.1.42.Final")
        compileOnly("com.google.guava:guava:33.0.0-jre")
    }

    tasks {
        withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(17)
        }

        build {
            dependsOn(shadowJar)
        }
    }

    publishing {
        publications {
            create<MavenPublication>("pledge") {
                groupId = "org.empirewar.pledge"
                artifactId = project.name
                version = project.version.toString()
                from(components["java"])
            }
        }

        repositories {
            // See Gradle docs for how to provide credentials to PasswordCredentials
            // https://docs.gradle.org/current/samples/sample_publishing_credentials.html
            maven {
                val snapshotUrl = "https://repo.convallyria.com/snapshots/"
                val releaseUrl = "https://repo.convallyria.com/releases/"

                // Check which URL should be used
                url = if (project.version.toString().endsWith("SNAPSHOT")) {
                    uri(snapshotUrl)
                } else {
                    uri(releaseUrl)
                }

                name = "snapshots"
                url = uri("https://repo.convallyria.com/snapshots/")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }
}


