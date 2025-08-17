import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":xicons-compose:xicons-compose-fluent"))
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
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