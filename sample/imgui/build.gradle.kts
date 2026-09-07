plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("imgui-kmp")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
        mainRun {
            mainClass = "cn.enaium.xicons.imgui.sample.MainKt"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":xicons-imgui:xicons-imgui-core"))
            implementation(project(":xicons-imgui:xicons-imgui-antd"))
            implementation(project(":xicons-imgui:xicons-imgui-carbon"))
            implementation(project(":xicons-imgui:xicons-imgui-fa"))
            implementation(project(":xicons-imgui:xicons-imgui-fluent"))
            implementation(project(":xicons-imgui:xicons-imgui-ionicons4"))
            implementation(project(":xicons-imgui:xicons-imgui-ionicons5"))
            implementation(project(":xicons-imgui:xicons-imgui-material"))
            implementation(project(":xicons-imgui:xicons-imgui-tabler"))
            implementation(libs.imgui.kmp)
        }
        jvmMain.dependencies {
            implementation(libs.imgui.kmp)
            implementation(libs.sdl.kmp)
        }
    }
}
