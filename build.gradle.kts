import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("kapt") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

subprojects {
    apply {
        plugin("kotlin")
        plugin("org.jetbrains.kotlin.plugin.serialization")
        plugin("com.github.johnrengelman.shadow")
    }
}

allprojects {
    group = "net.azisaba.gift"
    version = "1.2.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.3")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
        implementation("com.charleskorn.kaml:kaml:0.46.0") // YAML support for kotlinx.serialization
        implementation("org.mariadb.jdbc:mariadb-java-client:3.0.6")
        @Suppress("GradlePackageUpdate")
        implementation("com.zaxxer:HikariCP:4.0.3") // For Java 8
        testImplementation(kotlin("test"))
    }

    java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

    tasks {
        test {
            useJUnitPlatform()
        }

        processResources {
            filteringCharset = "UTF-8"
            from(sourceSets.main.get().resources.srcDirs) {
                include("**")

                val tokenReplacementMap = mapOf(
                    "name" to project.rootProject.name,
                    "version" to project.rootProject.version,
                )

                filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
            }

            duplicatesStrategy = DuplicatesStrategy.INCLUDE

            from(projectDir) { include("LICENSE") }
        }

        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}

subprojects {
    tasks {
        shadowJar {
            relocate("kotlinx", "net.azisaba.gift.lib.kotlinx")
            relocate("kotlin", "net.azisaba.gift.lib.kotlin")
            relocate("org.jetbrains.annotations", "net.azisaba.gift.lib.org.jetbrains.annotations")
            relocate("org.intellij.lang.annotations", "net.azisaba.gift.lib.org.intellij.lang.annotations")
            relocate("com.charleskorn.kaml", "net.azisaba.gift.lib.com.charleskorn.kaml")
            relocate("com.zaxxer", "net.azisaba.gift.lib.com.zaxxer")
            relocate("org.mariadb.jdbc", "net.azisaba.gift.lib.org.mariadb.jdbc")
            relocate("org.snakeyaml", "net.azisaba.gift.lib.org.snakeyaml")

            archiveFileName.set("${parent!!.name}-${project.name}-${project.version}.jar")
        }
    }
}
