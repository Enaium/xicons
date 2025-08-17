plugins {
    java
    alias(libs.plugins.javafx)
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