pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
}

rootProject.name = "EventNotifications"

include("core")
include("fabric")
