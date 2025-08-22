plugins {
    id("compose")
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/commonMain/kotlin")
        }
    }
}