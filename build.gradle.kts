plugins {
    `java-library`
    `maven-publish`
    idea
    id("net.neoforged.moddev") version "2.0.141"
}

val modId = project.property("mod_id") as String
val modName = project.property("mod_name") as String
val modLicense = project.property("mod_license") as String
val modVersion = project.property("mod_version") as String
val modGroupId = project.property("mod_group_id") as String
val modAuthors = project.property("mod_authors") as String
val modDescription = project.property("mod_description") as String
val minecraftVersion = project.property("minecraft_version") as String
val minecraftVersionRange = project.property("minecraft_version_range") as String
val neoVersion = project.property("neo_version") as String
val neoVersionRange = project.property("neo_version_range") as String
val loaderVersionRange = project.property("loader_version_range") as String

version = modVersion
group = modGroupId

repositories {
    mavenLocal()
    mavenCentral()
}

base {
    archivesName.set(modId)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

val libraries: Configuration by configurations.creating
configurations.named("implementation") { extendsFrom(libraries) }

neoForge {
    version = neoVersion

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("data") {
            data()
            programArguments.addAll(
                listOf(
                    "--mod", modId,
                    "--all",
                    "--output", file("src/generated/resources/").absolutePath,
                    "--existing", file("src/main/resources/").absolutePath,
                ),
            )
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
            additionalRuntimeClasspathConfiguration.extendsFrom(libraries)
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets.main.get().resources.srcDir("src/generated/resources")

dependencies {
    libraries(project(":core"))
}

tasks.jar {
    dependsOn(project(":core").tasks.named("jar"))
    from(project(":core").sourceSets["main"].output)
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "neo_version_range" to neoVersionRange,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription,
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(layout.projectDirectory.dir("repo"))
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
