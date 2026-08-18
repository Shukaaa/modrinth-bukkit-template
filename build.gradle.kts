import java.net.URI
import java.util.jar.JarFile
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import xyz.jpenilla.runpaper.RunPaperExtension
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").orElse("0.0.0-SNAPSHOT").get()

data class MinecraftTarget(val version: String, val javaVersion: Int)

// This checked-in list is the only source for builds, runs, CI, and documentation.
val minecraftTargets = listOf(
    MinecraftTarget("1.21", 21),
    MinecraftTarget("1.21.1", 21),
    MinecraftTarget("1.21.3", 21),
    MinecraftTarget("1.21.4", 21),
    MinecraftTarget("1.21.5", 21),
    MinecraftTarget("1.21.6", 21),
    MinecraftTarget("1.21.7", 21),
    MinecraftTarget("1.21.8", 21),
    MinecraftTarget("1.21.9", 21),
    MinecraftTarget("1.21.10", 21),
    MinecraftTarget("1.21.11", 21),
    MinecraftTarget("26.1.1", 25),
    MinecraftTarget("26.1.2", 25),
    MinecraftTarget("26.2", 25),
)

val latestTarget = minecraftTargets.last()
val projectProps = providers.provider {
    mapOf(
        "name" to providers.gradleProperty("pluginName").get(),
        "id" to providers.gradleProperty("pluginId").get(),
        "package" to providers.gradleProperty("pluginPackage").get(),
        "mainClass" to providers.gradleProperty("pluginMainClass").get(),
        "description" to providers.gradleProperty("pluginDescription").get(),
        "author" to providers.gradleProperty("pluginAuthor").get(),
        "version" to project.version.toString(),
    )
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

extensions.configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging.events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
}

tasks.named<ProcessResources>("processResources") {
    inputs.properties(projectProps.get())
    filesMatching("plugin.yml") {
        expand(projectProps.get())
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    dependsOn("check")
    archiveBaseName.set(providers.gradleProperty("pluginId"))
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    mergeServiceFiles()
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
}

fun prepareRunDirectory(directory: java.io.File) {
    directory.mkdirs()
    directory.resolve("eula.txt").writeText("eula=true\n")
}

val toolchains = extensions.getByType<JavaToolchainService>()
val runJar = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")

tasks.named<RunServer>("runServer") {
    group = "minecraft development"
    description = "Run Paper ${latestTarget.version}"
    minecraftVersion(latestTarget.version)
    val runDir = layout.projectDirectory.dir("run/paper_26-2").asFile
    runDirectory.set(runDir)
    pluginJars(runJar.flatMap { it.archiveFile })
    javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(latestTarget.javaVersion)) })
    doFirst { prepareRunDirectory(runDir) }
    dependsOn("shadowJar")
}

tasks.register<RunServer>("runPaper26_2") {
    group = "minecraft development"
    description = "Run Paper 26.2"
    minecraftVersion("26.2")
    val runDir = layout.projectDirectory.dir("run/paper_26-2").asFile
    runDirectory.set(runDir)
    pluginJars(runJar.flatMap { it.archiveFile })
    javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
    doFirst { prepareRunDirectory(runDir) }
    dependsOn("shadowJar")
}

tasks.register<Exec>("runPurpur26_2") {
    group = "minecraft development"
    description = "Run Purpur 26.2"
    dependsOn("shadowJar")
    doFirst {
        val runDir = layout.projectDirectory.dir("run/purpur_26-2").asFile
        val pluginDir = runDir.resolve("plugins")
        pluginDir.mkdirs()
        prepareRunDirectory(runDir)
        val serverJar = runDir.resolve("purpur-26.2.jar")
        if (!serverJar.exists()) {
            URI("https://api.purpurmc.org/v2/purpur/26.2/latest/download").toURL()
                .openStream().use { input -> serverJar.outputStream().use { input.copyTo(it) } }
        }
        layout.buildDirectory.file("libs/${providers.gradleProperty("pluginId").get()}-${project.version}.jar")
            .get().asFile.copyTo(pluginDir.resolve("plugin.jar"), overwrite = true)
        commandLine(
            toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) }
                .get().executablePath.asFile,
            "-jar", serverJar.absolutePath
        )
        workingDir(runDir)
    }
}

tasks.register("verifyReleaseJar") {
    group = "release"
    description = "Verify the canonical standalone release JAR"
    dependsOn("shadowJar")
    doLast {
        val expected = "${providers.gradleProperty("pluginId").get()}-${project.version}.jar"
        val jar = layout.buildDirectory.file("libs/$expected").get().asFile
        check(jar.isFile && jar.length() > 0) { "Missing release JAR: $jar" }
        JarFile(jar).use { archive ->
            val entries = archive.entries().asSequence().map { it.name }.toSet()
            check("plugin.yml" in entries)
            check(providers.gradleProperty("pluginMainClass").get().replace('.', '/') + ".class" in entries)
            check(entries.any { it.startsWith("kotlin/") }) { "Kotlin runtime is missing" }
            val pluginYml = archive.getInputStream(archive.getJarEntry("plugin.yml")).bufferedReader().readText()
            check(Regex("""(?m)^version:\s*${Regex.escape(project.version.toString())}\s*$""").containsMatchIn(pluginYml))
        }
        check(!layout.buildDirectory.dir("libs").get().asFile.listFiles()!!.any { it.name.endsWith("-plain.jar") })
    }
}
