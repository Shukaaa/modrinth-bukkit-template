# Minecraft Kotlin Plugin Template

Reusable production template for `rip.shuka` plugins. It targets stable Bukkit-compatible server releases from Minecraft 1.21 through 26.2 and produces one standalone JAR.

## Create New Plugin

Change these values in `gradle.properties`:

* `pluginName`
* `pluginId`
* `pluginPackage`
* `pluginMainClass`
* `pluginDescription`
* `pluginAuthor`

Keep the namespace under `rip.shuka`. Rename the main class and package directories when changing `pluginPackage`.

## Architecture

This is a single Gradle module with sources directly below `src/`. It uses only the public Bukkit API and contains no platform-specific, NMS, CraftBukkit, or bootstrap code. The same JAR runs on Bukkit-compatible Spigot, Paper, and Purpur servers.

## Requirements

Builds require Java 21. Plugin bytecode is always Java 21. Minecraft 1.21–1.21.11 runs on Java 21; Minecraft 26.1+ runs on Java 25.

## Build

```bash
./gradlew build
./gradlew shadowJar
./gradlew verifyReleaseJar
```

The canonical artifact is `build/libs/template-plugin-<version>.jar`.

## Run Local Servers

```bash
./gradlew runServer
./gradlew runPaper26_2
./gradlew runPurpur26_2
```

The servers use isolated directories:

```text
run/paper_26-2/
run/purpur_26-2/
```

Bukkit and Spigot are APIs/server implementations, not officially redistributable standalone server downloads. Their Bukkit-compatible behavior is exercised by running Paper or Purpur.

## Local Runtime Development

```bash
./gradlew build
./gradlew runServer
```

There are no automated runtime smoke-test matrices. Start the loader you are actively developing against and inspect the server log normally.

## Release

Use Conventional Commits: `fix:` creates a patch, `feat:` a minor release, and `feat!:` or `BREAKING CHANGE:` a major release. Semantic Release runs only after the verification workflow succeeds, creates the GitHub release/tag, and uploads the same JAR to Modrinth.

## GitHub Configuration

* Secret: `MODRINTH_TOKEN`
* Variable: `MODRINTH_PROJECT_ID`

The release workflow publishes loaders `bukkit`, `spigot`, `paper`, and `purpur` with environment `dedicated_server_only`, using the range `>=1.21 <=26.2`.
