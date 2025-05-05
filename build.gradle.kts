plugins {
    id("java")
    // Optional: Shadow plugin for fat JARs if needed, but usually not for simple plugins
    // id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.travelplus"
version = "1.0-SNAPSHOT"

description = "TravellerPlus Plugin"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Optional: Configure shadowJar task if using the shadow plugin
/*
shadowJar {
    archiveClassifier.set("") // Optional: remove the '-all' suffix
    relocate("com.example.dependency", "com.travelplus.lib.dependency") // Relocate dependencies if needed
}

build {
    dependsOn(shadowJar)
}
*/

