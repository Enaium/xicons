plugins {
    java
    id("com.vanniktech.maven.publish")
}

val commonMain = java.sourceSets.create("commonMain")
val antdMain = java.sourceSets.create("antdMain") {
    compileClasspath += commonMain.output
    runtimeClasspath += commonMain.output
}
val carbonMain = java.sourceSets.create("carbonMain") {
    compileClasspath += commonMain.output
    runtimeClasspath += commonMain.output
}
val faMain = java.sourceSets.create("faMain") {
    compileClasspath += commonMain.output
    runtimeClasspath += commonMain.output
}
val fluentMain = java.sourceSets.create("fluentMain") {
    compileClasspath += commonMain.output
    runtimeClasspath += commonMain.output
}
val ionicons4Main = java.sourceSets.create("ionicons4Main") {
    compileClasspath += commonMain.output
    runtimeClasspath += commonMain.output
}
val ionicons5Main = java.sourceSets.create("ionicons5Main") {
    compileClasspath += commonMain.output
    runtimeClasspath += commonMain.output
}
val materialMain = java.sourceSets.create("materialMain") {
    compileClasspath += commonMain.output
    runtimeClasspath += commonMain.output
}
val tablerMain = java.sourceSets.create("tablerMain") {
    compileClasspath += commonMain.output
    runtimeClasspath += commonMain.output
}

java.sourceSets.test {
    compileClasspath += commonMain.output
    runtimeClasspath += commonMain.output
    compileClasspath += antdMain.output
    runtimeClasspath += antdMain.output
    compileClasspath += carbonMain.output
    runtimeClasspath += carbonMain.output
    compileClasspath += faMain.output
    runtimeClasspath += faMain.output
    compileClasspath += fluentMain.output
    runtimeClasspath += fluentMain.output
    compileClasspath += ionicons4Main.output
    runtimeClasspath += ionicons4Main.output
    compileClasspath += ionicons5Main.output
    runtimeClasspath += ionicons5Main.output
    compileClasspath += materialMain.output
    runtimeClasspath += materialMain.output
    compileClasspath += tablerMain.output
    runtimeClasspath += tablerMain.output
}

val antdJar = tasks.register<Jar>("antdJar") {
    archiveClassifier.set("antd")
    from(commonMain.output)
    from(antdMain.output)
}

val carbonJar = tasks.register<Jar>("carbonJar") {
    archiveClassifier.set("carbon")
    from(commonMain.output)
    from(carbonMain.output)
}

val faJar = tasks.register<Jar>("faJar") {
    archiveClassifier.set("fa")
    from(commonMain.output)
    from(faMain.output)
}

val fluentJar = tasks.register<Jar>("fluentJar") {
    archiveClassifier.set("fluent")
    from(commonMain.output)
    from(fluentMain.output)
}

val ionicons4Jar = tasks.register<Jar>("ionicons4Jar") {
    archiveClassifier.set("ionicons4")
    from(commonMain.output)
    from(ionicons4Main.output)
}

val ionicons5Jar = tasks.register<Jar>("ionicons5Jar") {
    archiveClassifier.set("ionicons5")
    from(commonMain.output)
    from(ionicons5Main.output)
}

val materialJar = tasks.register<Jar>("materialJar") {
    archiveClassifier.set("material")
    from(commonMain.output)
    from(materialMain.output)
}

val tablerJar = tasks.register<Jar>("tablerJar") {
    archiveClassifier.set("tabler")
    from(commonMain.output)
    from(tablerMain.output)
}


tasks.jar {
    archiveClassifier.set("all")
    from(commonMain.output)
    from(antdMain.output)
    from(carbonMain.output)
    from(faMain.output)
    from(fluentMain.output)
    from(ionicons4Main.output)
    from(ionicons5Main.output)
    from(materialMain.output)
    from(tablerMain.output)
    dependsOn(antdJar, carbonJar, faJar, fluentJar, ionicons4Jar, ionicons5Jar, materialJar, tablerJar)
}

afterEvaluate {
    publishing {
        publications {
            getByName<MavenPublication>("maven") {
                artifact(antdJar)
                artifact(carbonJar)
                artifact(faJar)
                artifact(fluentJar)
                artifact(ionicons4Jar)
                artifact(ionicons5Jar)
                artifact(materialJar)
                artifact(tablerJar)
            }
        }
    }
}