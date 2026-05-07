pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    create(rootProject) {
        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        versions(
            "1.14.4",
            "1.15.2",
            "1.16.5",
            "1.17.1",
            "1.18.2",
            "1.19.2",
            "1.19.4",
            "1.20.1",
            "1.20.4",
            "1.20.6",
            "1.21.1",
            "1.21.10",
            "1.21.11",
            "26.1"
        )
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "SendModList"