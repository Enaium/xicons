plugins {
    java
    application
}

application {
    mainClass = "Main"
}

dependencies {
    implementation(project(":xicons-swing")) {
        artifact {
            classifier = "all"
        }
    }
    implementation(libs.flatlaf)
}