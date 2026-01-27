pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "EventNotifications"

include("core")
include("fabric")
include("paper")
include("neoforge")
