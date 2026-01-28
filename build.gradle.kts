plugins {
    java
}

val javaVersion = libs.versions.java.get().toInt()

subprojects {
    apply(plugin = "java")

    java {
        // withSourcesJar()
        sourceCompatibility = JavaVersion.toVersion(javaVersion)
        targetCompatibility = JavaVersion.toVersion(javaVersion)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(javaVersion)
    }
}
