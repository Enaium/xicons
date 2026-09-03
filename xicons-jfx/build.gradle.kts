plugins {
    id("xicons-jfx")
    id("multiple-icons")
    id("publish")
    alias(libs.plugins.javafx)
}

javafx {
    modules("javafx.controls")
}

dependencies {
    "commonMainCompileOnly"("org.openjfx:javafx-controls:17")
    for (name in listOf("antd", "carbon", "fa", "fluent", "ionicons4", "ionicons5", "material", "tabler")) {
        "${name}CompileOnly"("org.openjfx:javafx-controls:17")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_1_8
}