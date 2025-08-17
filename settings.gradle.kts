rootProject.name = "xicons"

pluginManagement {
    includeBuild("build-logic")
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
        maven("https://jetbrains.bintray.com/trove4j")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include("xicons-swing")
include("xicons-jfx")
include("sample:swing")
include("sample:jfx")
include("sample:compose")
include("xicons-compose:xicons-compose-antd")
include("xicons-compose:xicons-compose-carbon")
include("xicons-compose:xicons-compose-fa")
include("xicons-compose:xicons-compose-fluent")
include("xicons-compose:xicons-compose-ionicons4")
include("xicons-compose:xicons-compose-ionicons5")
include("xicons-compose:xicons-compose-material")
include("xicons-compose:xicons-compose-tabler")