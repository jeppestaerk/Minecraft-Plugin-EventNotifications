plugins {
    java
    alias(libs.plugins.moddevgradle)
}

base {
    archivesName.set("EventNotifications-NeoForge")
}

neoForge {
    version = libs.versions.neoforge.get()

    runs {
        create("server") {
            server()
        }
    }

    mods {
        create("EventNotifications") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(project(":core"))
    jarJar(project(":core"))

    jarJar(libs.configurate.yaml) {
        version {
            strictly("[${libs.versions.configurate.get()},)")
        }
    }
    jarJar(libs.configurate.core) {
        version {
            strictly("[${libs.versions.configurate.get()},)")
        }
    }
    jarJar(libs.kyori.option) {
        version {
            strictly("[${libs.versions.kyori.option.get()},)")
        }
    }
    jarJar(libs.geantyref) {
        version {
            strictly("[${libs.versions.geantyref.get()},)")
        }
    }
    jarJar(libs.snakeyaml) {
        version {
            strictly("[${libs.versions.snakeyaml.get()},)")
        }
    }

    implementation(libs.configurate.yaml)
    implementation(libs.configurate.core)
    implementation(libs.kyori.option)
    implementation(libs.geantyref)
    implementation(libs.snakeyaml)
}

val modVersion = project.version.toString()

tasks.processResources {
    inputs.property("version", modVersion)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to modVersion)
    }
}

tasks.jar {
    archiveClassifier.set("")
    archiveVersion.set(modVersion)
    dependsOn(tasks.named("jarJar"))
}

tasks.build {
    dependsOn(tasks.jar)
}
