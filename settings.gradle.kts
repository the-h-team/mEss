rootProject.name = "myEssentials"

sequenceOf(
    "api",
    "addons",
    "plugin"
).forEach {
    include(":myessentials-$it")
    project(":myessentials-$it").projectDir = file(it)
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("spigot-api", "org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
            version("labyrinth", "1.9.0")
            sequenceOf("common", "gui", "perms", "regions", "skulls").map { "labyrinth-$it" }.forEach {
                library(it, "com.github.the-h-team.Labyrinth", it).versionRef("labyrinth")
            }
            version("panther", "1.0.2")
            sequenceOf("common", "placeholders").map { "panther-$it"}.forEach {
                library(it, "com.github.the-h-team.Panther", it).versionRef("panther")
            }
            library("location-api", "com.github.the-h-team:LocationAPI:1.0_R1")
        }
    }
}
