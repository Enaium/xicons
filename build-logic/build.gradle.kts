plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.javapoet)
    implementation(libs.kotlinpoet)
}

gradlePlugin {
    plugins {
        create("xicons-swing") {
            id = "xicons-swing"
            implementationClass = "XIconsSwingPlugin"
        }
        create("xicons-jfx") {
            id = "xicons-jfx"
            implementationClass = "XIconsJfxPlugin"
        }
        create("xicons-compose") {
            id = "xicons-compose"
            implementationClass = "XIconsComposePlugin"
        }
    }
}