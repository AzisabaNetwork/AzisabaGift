repositories {
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.rosewooddev.io/repository/public/") }
}

dependencies {
    implementation(project(":"))
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("org.black_ixx:playerpoints:3.2.3")
}

tasks {
    shadowJar {
        relocate("org.slf4j", "net.azisaba.gift.lib.org.slf4j")
    }
}
