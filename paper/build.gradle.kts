plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

base {
    archivesName.set("EventNotifications-Paper")
}

dependencies {
    compileOnly(libs.paper.api)

    implementation(project(":core"))
    implementation(libs.configurate.yaml)
    implementation(libs.configurate.core)
    implementation(libs.kyori.option)
    implementation(libs.geantyref)
}

tasks.compileJava {
    options.compilerArgs.add("-Xlint:deprecation")
}

val modVersion = project.version.toString()

tasks.processResources {
    inputs.property("version", modVersion)

    filesMatching("plugin.yml") {
        expand("version" to modVersion)
    }
}

tasks.jar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveVersion.set(modVersion)

    relocate("org.spongepowered.configurate", "dev.valhal.minecraft.plugin.EventNotifications.libs.configurate")
    relocate("org.yaml.snakeyaml", "dev.valhal.minecraft.plugin.EventNotifications.libs.snakeyaml")
    relocate("io.leangen.geantyref", "dev.valhal.minecraft.plugin.EventNotifications.libs.geantyref")
    relocate("net.kyori.option", "dev.valhal.minecraft.plugin.EventNotifications.libs.option")

    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion(libs.versions.minecraft.get())
    }
}
