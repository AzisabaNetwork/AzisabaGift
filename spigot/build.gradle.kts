repositories {
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
}

dependencies {
    implementation(project(":"))
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        relocate("org.slf4j", "net.azisaba.gift.lib.org.slf4j")
    }
}
