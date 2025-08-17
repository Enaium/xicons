plugins {
    java
}

dependencies {
    implementation(project(":xicons-swing")) {
        artifact {
            classifier = "all"
        }
    }
    implementation(libs.flatlaf)
}