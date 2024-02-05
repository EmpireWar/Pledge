dependencies {
    implementation(project(":common"))

    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    archiveClassifier.set("")
}