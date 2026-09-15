plugins {
    alias(libs.plugins.fabric.loom)
    id("maven-publish")
}

fun gitShortSha(): String? = try {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim().takeIf { it.isNotBlank() }
} catch (_: Exception) {
    null
}

base {
    archivesName.set(providers.gradleProperty("archives_base_name"))
    // builds: dupersunited-{mod}+{mc}-{sha}.jar, releases: dupersunited-{mod}+{mc}.jar
    val release = providers.gradleProperty("release").orNull == "true"
    val suffix = if (release) "" else "-" + (gitShortSha() ?: "local")
    version = "${providers.gradleProperty("version").get()}+${libs.versions.minecraft.get()}$suffix"
    group = providers.gradleProperty("maven_group").get()
}

repositories {
    maven {
        name = "TerraformersMC"
        url = uri("https://maven.terraformersmc.com/releases/")
    }
}

configurations {
    val library = create("library")

    implementation.configure {
        extendsFrom(library)
    }
    include.configure {
        extendsFrom(library)
    }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn) { classifier("v2") })
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    modCompileOnly(libs.modmenu)

    val library = configurations.named("library")

    library(libs.netty.handler.proxy) { isTransitive = false }
    library(libs.netty.codec.socks) { isTransitive = false }
}

tasks {
    processResources {
        val properties = mapOf(
            "version" to project.version,
            "minecraft_version" to libs.versions.minecraft.get()
        )

        filteringCharset = "UTF-8"

        inputs.properties(properties)
        filesMatching("fabric.mod.json") {
            expand(properties)
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }

        withSourcesJar()
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }
        from("THIRD_PARTY_ASSETS.md")
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.base.archivesName.get()
            from(components["java"])
        }
    }

    repositories {
        val isRelease = providers.gradleProperty("release").orNull == "true"
        maven(if (isRelease) "https://maven.dupers.wtf/releases" else "https://maven.dupers.wtf/snapshots") {
            name = "DupersWtfMaven"

            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
