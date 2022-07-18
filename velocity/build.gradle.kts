plugins {
    kotlin("kapt")
}

repositories {
    maven { url = uri("https://repo.acrylicstyle.xyz/repository/maven-public/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    implementation(project(":"))
    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    kapt("com.velocitypowered:velocity-api:3.1.1")

    // SpicyAzisaBan
    compileOnly("net.azisaba.spicyazisaban:common:0.2.1-dev-8077128")
    compileOnly("xyz.acrylicstyle.util:promise:0.16.6")
    compileOnly("xyz.acrylicstyle:minecraft-util:1.1.0")
}
