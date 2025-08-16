plugins {
    `java-library`
}

group = "xyz.holocons.mc"
version = "1.21.8_update"
description = "Banner waypoints for HoloCons"

java {
    // Paper 1.21+ requires Java 21
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    // dmulloy2 Nexus repos for ProtocolLib
    maven("https://repo.dmulloy2.net/nexus/repository/public/")
    maven("https://repo.dmulloy2.net/nexus/repository/releases/")
    maven("https://repo.dmulloy2.net/nexus/repository/snapshots/")
    // CodeMC often mirrors ProtocolLib releases
    maven("https://repo.codemc.io/repository/maven-public/")
    // Maven Central as general fallback
    mavenCentral()
}

dependencies {
    // Update to Paper 1.21.x API (adjust patch version if needed, e.g. 1.21.2). 1.21.8 placeholder target.
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    // Local fallback for ProtocolLib: place the desired jar at libs/ProtocolLib.jar
    val protocolLibPaths = listOf(
        rootProject.file("libs/ProtocolLib.jar"),
        rootProject.file("ProtocolLib.jar")
    )
    val protocolLibLocal = protocolLibPaths.firstOrNull { it.exists() }
    if (protocolLibLocal != null) {
        println("[build.gradle.kts] Using local ProtocolLib.jar at ${'$'}{protocolLibLocal.relativeTo(rootProject.projectDir)}")
        compileOnly(files(protocolLibLocal))
    } else {
        // Remote dependency (version may need adjustment to one present in the repo you target)
        compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
    options.release.set(21)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything

        val pluginProperties = mapOf(
            "main" to "xyz.holocons.mc.waypoints.WaypointsPlugin",
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            // Target newer Minecraft API version
            "apiVersion" to "1.21",
            "authors" to listOf("dlee13"),
            "website" to "holocons.xyz",
            "depend" to listOf("ProtocolLib"),
            "prefix" to "Waypoints",
        )

        filesMatching("plugin.yml") {
            expand(pluginProperties)
        }
    }
}
