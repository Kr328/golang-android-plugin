subprojects {
    group = "com.github.kr328.gradle.golang"
    version = "1.0.5"

    repositories {
        mavenCentral()
        google()
    }

    afterEvaluate {
        extensions.configure<PublishingExtension> {
            val sourcesJar = tasks.register("sourcesJar", type = Jar::class) {
                archiveClassifier.set("sources")

                from(project.extensions.getByType(SourceSetContainer::class)["main"].allSource)
            }

            publications {
                create("release", type = MavenPublication::class) {
                    pom {
                        name.set("kaidl")
                        description.set("Golang building plugin for Android project.")
                        url.set("https://github.com/Kr328/golang-android-plugin")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("http://www.opensource.org/licenses/mit-license.php")
                            }
                        }
                        developers {
                            developer {
                                id.set("kr328")
                                name.set("Kr328")
                                email.set("kr328app@outlook.com")
                            }
                        }
                    }

                    from(components["java"])

                    artifact(sourcesJar)

                    groupId = project.group.toString()
                    artifactId = project.name
                    version = project.version.toString()
                }
            }

            repositories {
                mavenLocal()
                maven {
                    name = "kr328app"
                    url = uri("https://maven.kr328.app/releases")
                    credentials(PasswordCredentials::class.java)
                }
            }
        }
    }
}