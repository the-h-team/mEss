plugins {
    id("myessentials.java-conventions")
    id("com.gradleup.shadow") version "8.3.3"
}

dependencies {
    api(project(":myessentials-addons"))
    api(project(":myessentials-api"))
    api(libs.location.api)
}

tasks.withType<ProcessResources> {
    inputs.apply {
        // Always check these properties for updates so that the plugin.yml is regenerated properly if they change without performing a full clean
        property("version", project.version)
        property("description", project.description)
        property("url", findProperty("url")!!)
    }
    // Only expand properties in the plugin.yml
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    archiveClassifier.set("plugin")
    dependencies {
        libs.location.api.get().run {
            include(dependency("$group:$name:$version"))
        }
        include(project(":myessentials-addons"))
        include(project(":myessentials-api"))
    }
}