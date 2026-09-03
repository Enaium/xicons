plugins {
    java
    application
    alias(libs.plugins.javafx)
}

application {
    mainClass = "Main"
}

javafx {
    modules("javafx.controls")
}

dependencies {
    implementation(project(":xicons-jfx")) {
        artifact {
            classifier = "all"
        }
    }
}