plugins {
    java
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName.set("eventnotifications-fabric")
}

tasks.remapJar {
    archiveVersion.set(project.version.toString())
}

dependencies {
    minecraft(libs.minecraft)
    mappings("${libs.yarn.mappings.get()}:v2")
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    implementation(project(":core"))
    include(project(":core"))

    include(libs.configurate.yaml)
    include(libs.configurate.core)
    include(libs.kyori.option)
    include(libs.geantyref)
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
