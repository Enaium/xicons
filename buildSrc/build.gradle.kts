plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api("com.vanniktech:gradle-maven-publish-plugin:${property("maven")}")
}