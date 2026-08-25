plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "zepsizola.me"
version = "1.8"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly("net.kyori:adventure-text-minimessage:4.15.0")
    compileOnly(kotlin("stdlib"))
    implementation("org.bstats:bstats-bukkit:3.0.2")
}

// Paper 26.2 requires Java 25. This configures both Kotlin and Java compilation.
kotlin {
    jvmToolchain(25)
}

tasks {
    shadowJar {
        relocate("org.bstats", "me.zepsizola.zInvisItemFrames.bstats")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
        minimize()
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
