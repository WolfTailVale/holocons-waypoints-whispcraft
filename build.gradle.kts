import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    `java-library`
}

// Load version properties
val versionProps = Properties()
val versionFile = file("version.properties")
if (versionFile.exists()) {
    versionFile.inputStream().use { versionProps.load(it) }
}

// Build clean version string: MinecraftVersion_PluginVersion
val minecraftVersion = versionProps.getProperty("minecraft", "1.21.8")
val pluginMajor = versionProps.getProperty("plugin_major", "1")
val pluginMinor = versionProps.getProperty("plugin_minor", "3")
val pluginPatch = versionProps.getProperty("plugin_patch", "0")

val buildVersion = "${minecraftVersion}_${pluginMajor}.${pluginMinor}.${pluginPatch}"

group = "xyz.holocons.mc"
version = buildVersion
description = "WhispWaypoints - Banner waypoints for Minecraft servers"

// Print version info
println("Building WhispWaypoints version: $version")

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
        rootProject.file("ProtocolLib.jar"),
        rootProject.file("dependencies/ProtocolLib.jar")
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
            "authors" to listOf("dlee13", "wolftailvale"),
            "website" to "github.com/WolfTailVale/holocons-waypoints-whispcraft",
            "depend" to listOf("ProtocolLib"),
            "prefix" to "WhispWaypoints",
        )

        filesMatching("plugin.yml") {
            expand(pluginProperties)
        }
    }

    // Task to increment plugin version
    register("incrementPatch") {
        group = "versioning"
        description = "Increments the plugin patch version (x.y.z -> x.y.z+1)"
        doLast {
            if (versionFile.exists()) {
                val currentPatch = versionProps.getProperty("plugin_patch", "0").toInt()
                val newPatch = currentPatch + 1
                
                val lines = versionFile.readLines().toMutableList()
                for (i in lines.indices) {
                    if (lines[i].startsWith("plugin_patch=")) {
                        lines[i] = "plugin_patch=$newPatch"
                        break
                    }
                }
                
                versionFile.writeText(lines.joinToString("\n"))
                println("Plugin patch version incremented: $currentPatch -> $newPatch")
            }
        }
    }

    register("incrementMinor") {
        group = "versioning"
        description = "Increments the plugin minor version and resets patch to 0"
        doLast {
            if (versionFile.exists()) {
                val currentMinor = versionProps.getProperty("plugin_minor", "3").toInt()
                val newMinor = currentMinor + 1
                
                val lines = versionFile.readLines().toMutableList()
                for (i in lines.indices) {
                    when {
                        lines[i].startsWith("plugin_minor=") -> lines[i] = "plugin_minor=$newMinor"
                        lines[i].startsWith("plugin_patch=") -> lines[i] = "plugin_patch=0"
                    }
                }
                
                versionFile.writeText(lines.joinToString("\n"))
                println("Plugin minor version incremented: $currentMinor -> $newMinor (patch reset to 0)")
            }
        }
    }

    register("incrementMajor") {
        group = "versioning"
        description = "Increments the plugin major version and resets minor/patch to 0"
        doLast {
            if (versionFile.exists()) {
                val currentMajor = versionProps.getProperty("plugin_major", "1").toInt()
                val newMajor = currentMajor + 1
                
                val lines = versionFile.readLines().toMutableList()
                for (i in lines.indices) {
                    when {
                        lines[i].startsWith("plugin_major=") -> lines[i] = "plugin_major=$newMajor"
                        lines[i].startsWith("plugin_minor=") -> lines[i] = "plugin_minor=0"
                        lines[i].startsWith("plugin_patch=") -> lines[i] = "plugin_patch=0"
                    }
                }
                
                versionFile.writeText(lines.joinToString("\n"))
                println("Plugin major version incremented: $currentMajor -> $newMajor (minor/patch reset to 0)")
            }
        }
    }

    // Task to set Minecraft version
    register<Task>("setMinecraftVersion") {
        group = "versioning"
        description = "Sets the Minecraft version (use -PmcVersion=1.21.8)"
        doLast {
            val newMcVersion = project.findProperty("mcVersion") as String? ?: "1.21.8"
            
            if (versionFile.exists()) {
                val lines = versionFile.readLines().toMutableList()
                for (i in lines.indices) {
                    if (lines[i].startsWith("minecraft=")) {
                        lines[i] = "minecraft=$newMcVersion"
                        break
                    }
                }
                
                versionFile.writeText(lines.joinToString("\n"))
                println("Minecraft version set to: $newMcVersion")
            }
        }
    }
}
