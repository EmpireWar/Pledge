/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    id("buildlogic.java-conventions")
    id("io.github.goooler.shadow") version "8.1.7"
}

dependencies {
    api(project(":common"))
    compileOnly(libs.org.spongepowered.spongeapi)
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

java.targetCompatibility = JavaVersion.VERSION_17

group = "dev.thomazz.pledge"
description = "sponge"
