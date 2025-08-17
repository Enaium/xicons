plugins {
    `kotlin-dsl`
}

repositories {
    google {
        content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google.*")
            includeGroupByRegex("androidx.*")
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("com.vanniktech:gradle-maven-publish-plugin:${property("maven")}")
    api("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:${property("kotlin")}")
    api("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:${property("kotlin")}")
    api("org.jetbrains.compose:org.jetbrains.compose.gradle.plugin:${property("compose")}")
    api("com.android.tools.build:gradle:${property("android")}")
}