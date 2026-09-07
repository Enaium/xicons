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
    api(libs.maven.publish.plugin)
    api(libs.kotlin.multiplatform.plugin)
    api(libs.kotlin.compose.plugin)
    api(libs.compose.plugin)
    api(libs.android.kmp.plugin)
}
