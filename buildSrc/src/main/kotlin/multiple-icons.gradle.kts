plugins {
    java
    id("com.vanniktech.maven.publish")
}

val commonMain = java.sourceSets.create("commonMain")

fun SourceSetContainer.createGen(name: String): SourceSet {
    return create(name) {
        java.setSrcDirs(listOf("build/generated/${name}/java"))
        compileClasspath += commonMain.output
        runtimeClasspath += commonMain.output
    }
}

val antd = java.sourceSets.createGen("antd")
val carbon = java.sourceSets.createGen("carbon")
val fa = java.sourceSets.createGen("fa")
val fluent = java.sourceSets.createGen("fluent")
val ionicons4 = java.sourceSets.createGen("ionicons4")
val ionicons5 = java.sourceSets.createGen("ionicons5")
val material = java.sourceSets.createGen("material")
val tabler = java.sourceSets.createGen("tabler")

java.sourceSets.test {
    compileClasspath += commonMain.output
    runtimeClasspath += commonMain.output
    compileClasspath += antd.output
    runtimeClasspath += antd.output
    compileClasspath += carbon.output
    runtimeClasspath += carbon.output
    compileClasspath += fa.output
    runtimeClasspath += fa.output
    compileClasspath += fluent.output
    runtimeClasspath += fluent.output
    compileClasspath += ionicons4.output
    runtimeClasspath += ionicons4.output
    compileClasspath += ionicons5.output
    runtimeClasspath += ionicons5.output
    compileClasspath += material.output
    runtimeClasspath += material.output
    compileClasspath += tabler.output
    runtimeClasspath += tabler.output
}

val antdJar = tasks.register<Jar>("antdJar") {
    archiveClassifier.set("antd")
    from(commonMain.output)
    from(antd.output)
}

val carbonJar = tasks.register<Jar>("carbonJar") {
    archiveClassifier.set("carbon")
    from(commonMain.output)
    from(carbon.output)
}

val faJar = tasks.register<Jar>("faJar") {
    archiveClassifier.set("fa")
    from(commonMain.output)
    from(fa.output)
}

val fluentJar = tasks.register<Jar>("fluentJar") {
    archiveClassifier.set("fluent")
    from(commonMain.output)
    from(fluent.output)
}

val ionicons4Jar = tasks.register<Jar>("ionicons4Jar") {
    archiveClassifier.set("ionicons4")
    from(commonMain.output)
    from(ionicons4.output)
}

val ionicons5Jar = tasks.register<Jar>("ionicons5Jar") {
    archiveClassifier.set("ionicons5")
    from(commonMain.output)
    from(ionicons5.output)
}

val materialJar = tasks.register<Jar>("materialJar") {
    archiveClassifier.set("material")
    from(commonMain.output)
    from(material.output)
}

val tablerJar = tasks.register<Jar>("tablerJar") {
    archiveClassifier.set("tabler")
    from(commonMain.output)
    from(tabler.output)
}


tasks.jar {
    archiveClassifier.set("all")
    from(commonMain.output)
    from(antd.output)
    from(carbon.output)
    from(fa.output)
    from(fluent.output)
    from(ionicons4.output)
    from(ionicons5.output)
    from(material.output)
    from(tabler.output)
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