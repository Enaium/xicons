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
include("xicons-imgui:xicons-imgui-core")
include("xicons-imgui:xicons-imgui-antd")
include("xicons-imgui:xicons-imgui-carbon")
include("xicons-imgui:xicons-imgui-fa")
include("xicons-imgui:xicons-imgui-fluent")
include("xicons-imgui:xicons-imgui-ionicons4")
include("xicons-imgui:xicons-imgui-ionicons5")
include("xicons-imgui:xicons-imgui-material")
include("xicons-imgui:xicons-imgui-tabler")
include("sample:imgui")