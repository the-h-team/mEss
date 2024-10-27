plugins {
    id("myessentials.java-conventions")
    `maven-publish`
}

java {
    withSourcesJar()
}
// un-wire sourcesJar from plain assemble
tasks.named("assemble") {
    setDependsOn(dependsOn.filterNot {
        (it as? Named)?.name == "sourcesJar"
    })
}
tasks.withType<AbstractPublishToMaven> {
    dependsOn.add(tasks.named("sourcesJar"))
}

afterEvaluate {
    publishing {
        publications.create<MavenPublication>(name) {
            // if on an Actions runner, set up GitHub Packages
            if (System.getenv("GITHUB_ACTIONS") == "true") {
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/the-h-team/myEssentials")
                        credentials {
                            username = System.getenv("GITHUB_ACTOR")
                            password = System.getenv("GITHUB_TOKEN")
                        }
                    }
                }
            }
            pom {
                description.set(
                    project.description ?:
                    throw IllegalStateException("Set the project description in ${project.projectDir.name}/build.gradle.kts before activating publishing.")
                )
                url.set(
                    project.properties["url"] as String? ?:
                    throw IllegalStateException("Set the project URL as the Gradle project property 'url' before activating publishing.")
                )
                inceptionYear.set(
                    project.properties["inceptionYear"] as String? ?:
                    throw IllegalStateException("Set the project inception year as the Gradle project property 'inceptionYear' before activating publishing.")
                )
                licenses {
                    license {
                        if (project.name.contains("plugin")) {
                            name.set("GNU General Public License v3.0 or later")
                            url.set("https://www.gnu.org/licenses/gpl-3.0-standalone.html")
                        } else {
                            name.set("Apache License 2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                        }
                        distribution.set("repo")
                    }
                }
                organization {
                    name.set("Sanctum")
                    url.set("https://github.com/the-h-team")
                }
                developers {
                    developer {
                        id.set("ms5984")
                        name.set("Matt")
                        url.set("https://github.com/ms5984")
                    }
                    developer {
                        id.set("Hempfest")
                        name.set("Hempfest")
                        url.set("https://github.com/Hempfest")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/the-h-team/myEssentials.git")
                    developerConnection.set("scm:git:ssh://github.com/the-h-team/myEssentials.git")
                    url.set("https://github.com/the-h-team/myEssentials/tree/master")
                }
            }
            from(components["java"])
        }
    }
}
