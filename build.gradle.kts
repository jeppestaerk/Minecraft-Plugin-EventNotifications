plugins {
    java
}

group = "dev.valhal"
version = libs.versions.mod.get()

val javaVersion = libs.versions.java.get().toInt()

subprojects {
    apply(plugin = "java")

    group = rootProject.group
    version = rootProject.version

    java {
        sourceCompatibility = JavaVersion.toVersion(javaVersion)
        targetCompatibility = JavaVersion.toVersion(javaVersion)
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(javaVersion)
    }
}
