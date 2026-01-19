plugins {
    id("java")
    id("eclipse")
    id("idea")
}

group = "dev.kracked.voice"
version = "1.0.0"

base.archivesName.set("KrackedVoice")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    // Hytale Maven repository (when available)
    // maven { url = uri("https://maven.hytale.com/releases") }
}

dependencies {
    // Hytale Server API (placeholder - use actual when available)
    // compileOnly("com.hytale:server-api:1.0.0")
    
    // Opus codec - using placeholder implementation
    // Real Opus will be provided by Hytale's native audio API
    // implementation("org.concentus:Concentus:x.x.x")
    
    // Netty for UDP networking
    implementation("io.netty:netty-all:4.1.100.Final")
    
    // JSON for config
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "dev.kracked.voice.KrackedVoicePlugin",
            "Plugin-Name" to "KrackedVoice",
            "Plugin-Version" to version
        )
    }
    
    // Fat JAR - include all dependencies
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // Exclude Hytale API stubs
    exclude("com/hypixel/**")
}
