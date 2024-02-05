pluginManagement {
    repositories {
        maven("https://repo.spongepowered.org/repository/maven-public/") // Sponge
    }
}

rootProject.name = "pledge"
include("common")
include("spigot")
include("sponge")
