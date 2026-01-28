plugins {
    java
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName.set("EventNotifications-Fabric")
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("EventNotifications-Fabric") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.language.kotlin)

    implementation(project(":core"))
    include(project(":core"))

    include(libs.configurate.yaml)
    include(libs.configurate.core)
    include(libs.kyori.option)
    include(libs.geantyref)
    include(libs.snakeyaml)
}

val modVersion = project.version.toString()

tasks.processResources {
    inputs.property("version", modVersion)

    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}

tasks.remapJar {
    archiveClassifier.set("")
    archiveVersion.set(modVersion)
}

tasks.build {
    dependsOn(tasks.remapJar)
}