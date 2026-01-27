plugins {
    java
    alias(libs.plugins.shadow)
}

base {
    archivesName.set("eventnotifications-paper")
}

dependencies {
    compileOnly(libs.paper.api)

    implementation(project(":core"))
    implementation(libs.configurate.yaml)
    implementation(libs.configurate.core)
    implementation(libs.kyori.option)
    implementation(libs.geantyref)
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

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
