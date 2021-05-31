plugins {
    java
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

repositories {
    maven {
        setUrl("https://repo.codemc.org/repository/nms/")
    }

}

dependencies {
    compileOnly("org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT")
}

tasks {
    named<JavaCompile>("compileJava") {
        options.encoding = "utf-8"
    }
    shadowJar {
        archiveFileName.set("PolyMartPlugin.jar")
        destinationDirectory.set(file("target"))
    }
}
