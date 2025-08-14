rootProject.name = "xicons"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}


include("xicons-swing")
include("xicons-jfx")