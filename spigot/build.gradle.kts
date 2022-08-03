repositories {
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://jitpack.io/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.rosewooddev.io/repository/public/") }
}

dependencies {
    implementation(project(":"))
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")

    // external APIs
    compileOnly("org.black_ixx:playerpoints:3.2.3") // handlers: GivePlayerPoints
    compileOnly("com.github.AzisabaNetwork:TaxOffice:a7cf5a4f6f") // selectors: HasPointsInTaxOffice
}

tasks {
    shadowJar {
        relocate("org.slf4j", "net.azisaba.gift.lib.org.slf4j")
    }
}
