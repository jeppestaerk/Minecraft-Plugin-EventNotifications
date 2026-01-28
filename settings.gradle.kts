rootProject.name = "EventNotifications"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "fabric-repo"
            url = uri("https://maven.fabricmc.net/")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "papermc-repo"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "sponge-repo"
            url = uri("https://repo.spongepowered.org/repository/maven-public/")
        }
    }
}

include(":core", ":fabric", ":paper", ":neoforge")
