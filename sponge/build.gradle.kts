repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(project(":common"))

    compileOnly("org.spongepowered:spongeapi:11.0.0-SNAPSHOT")
}

tasks.shadowJar {
    archiveClassifier.set("")
}