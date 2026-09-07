plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("imgui-kmp")
    id("xicons-imgui")
    id("publish")
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/commonMain/kotlin")
            dependencies {
                implementation(project(":xicons-imgui:xicons-imgui-core"))
                implementation(libs.imgui.kmp)
            }
        }
        jvmMain.dependencies {
            implementation(libs.imgui.kmp)
        }
    }
}
