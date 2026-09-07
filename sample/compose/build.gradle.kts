import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":xicons-compose:xicons-compose-fluent"))
            implementation(project(":xicons-compose:xicons-compose-antd"))
            implementation(project(":xicons-compose:xicons-compose-carbon"))
            implementation(project(":xicons-compose:xicons-compose-fa"))
            implementation(project(":xicons-compose:xicons-compose-ionicons4"))
            implementation(project(":xicons-compose:xicons-compose-ionicons5"))
            implementation(project(":xicons-compose:xicons-compose-material"))
            implementation(project(":xicons-compose:xicons-compose-tabler"))
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(kotlin("reflect"))
        }
    }

    wasmJs {
        outputModuleName.set("sample")
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "sample.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}