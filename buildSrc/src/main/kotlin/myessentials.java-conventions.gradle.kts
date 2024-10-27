plugins {
    `java-library`
}

repositories {
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigot-mc"
    }

    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.1")
/*  Test suite--uncomment when we start writing tests
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    val mockitoVersion = "5.13.0"
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    */
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}