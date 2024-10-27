plugins {
    id("myessentials.java-conventions")
    id("myessentials.publishing")
}

dependencies {
    api(libs.spigot.api)
    api(libs.panther.common)
    api(libs.panther.placeholders)
    api(libs.labyrinth.common)
    api(libs.labyrinth.gui)
    api(libs.labyrinth.perms)
}