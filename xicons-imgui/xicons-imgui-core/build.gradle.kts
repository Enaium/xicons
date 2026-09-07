plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("imgui-kmp")
    id("publish")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.imgui.kmp)
        }
        jvmMain.dependencies {
            implementation(libs.imgui.kmp)
        }
    }
}
