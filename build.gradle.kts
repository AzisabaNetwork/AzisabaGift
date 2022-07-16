import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("kapt") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

subprojects {
    apply {
        plugin("kotlin")
        plugin("com.github.johnrengelman.shadow")
    }
}

allprojects {
    group = "net.azisaba.gift"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.3")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
        testImplementation(kotlin("test"))
    }

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

            archiveFileName.set("${parent!!.name}-${project.name}-${project.version}.jar")
        }
    }
}
