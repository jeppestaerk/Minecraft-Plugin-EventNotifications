plugins {
    java
    alias(libs.plugins.moddevgradle)
}

base {
    archivesName.set("eventnotifications-neoforge")
}

neoForge {
    version = libs.versions.neoforge.get()

    runs {
        create("server") {
            server()
        }
    }

    mods {
        create("eventnotifications") {
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

    implementation(libs.configurate.yaml)
    implementation(libs.configurate.core)
    implementation(libs.kyori.option)
    implementation(libs.geantyref)
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    dependsOn(tasks.named("jarJar"))
}
