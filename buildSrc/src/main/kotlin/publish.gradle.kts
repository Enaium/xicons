plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {

    publishToMavenCentral(automaticRelease = true)

    signAllPublications()

    coordinates(
        groupId = project.group.toString(),
        artifactId = project.name,
        version = project.version.toString(),
    )

    pom {
        name = "XIcons"
        description = "A library for icon"
        url = "https://github.com/Enaium/xicons"
        licenses {
            license {
                name = "MIT"
                url = "https://mit-license.org/"
            }
        }
        developers {
            developer {
                name = "Enaium"
                url = "https://github.com/Enaium"
            }
        }
        scm {
            connection.set("scm:git:git://github.com/Enaium/xicons.git")
            developerConnection.set("scm:git:ssh://github.com/Enaium/xicons.git")
            url.set("https://github.com/Enaium/xicons")
        }
    }
}