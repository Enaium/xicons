plugins {
    java
    application
    alias(libs.plugins.javafx)
}

application {
    mainClass = "Main"
    applicationDefaultJvmArgs = listOf("-Dprism.order=sw")
}

javafx {
    version = "21.0.4"
    modules("javafx.controls")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":xicons-jfx")) {
        artifact {
            classifier = "all"
        }
    }
}