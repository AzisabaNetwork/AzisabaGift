package net.azisaba.gift.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlComment
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mariadb.jdbc.Driver
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

@Serializable
data class PluginConfig(
    val databaseConfig: DatabaseConfig = DatabaseConfig(),
    val providers: Providers = Providers(),
) {
    companion object {
        lateinit var instance: PluginConfig

        fun loadConfig(dataDirectory: Path, logInfo: (message: String) -> Unit = {}) {
            val configPath = dataDirectory.resolve("config.yml")
            logInfo("Loading config from $configPath")
            val comment = """
                # This is the config file for the plugin.
                # Note: Configuration file is regenerated every time the plugin is loaded.
                #       Values will be kept but all non-default comments will be removed.
            """.trimIndent() + "\n"
            if (!Files.exists(configPath)) {
                configPath.writeText(comment + Yaml.default.encodeToString(serializer(), PluginConfig()) + "\n")
            }
            instance = Yaml.default.decodeFromStream(serializer(), configPath.inputStream())
            configPath.writeText(comment + Yaml.default.encodeToString(serializer(), instance) + "\n")

            // initialize driver
            Driver()
        }
    }
}

@SerialName("database")
@Serializable
data class DatabaseConfig(
    @YamlComment(
        "Driver class to use. Default is the bundled mariadb driver.",
        "Set to null if you want to auto-detect the driver.",
    )
    val driver: String? = "net.azisaba.gift.lib.org.mariadb.jdbc.Driver",
    @YamlComment("Change to jdbc:mysql if you want to use MySQL instead of MariaDB")
    val scheme: String = "jdbc:mariadb",
    val hostname: String = "localhost",
    @YamlComment("Database name to ues (must match with database.databaseNames.azisabaApi in api-ktor-server")
    val port: Int = 3306,
    val name: String = "azisaba_gift",
    val username: String = "azisaba_gift",
    val password: String = "",
    val properties: Map<String, String> = mapOf(
        "useSSL" to "false",
        "verifyServerCertificate" to "true",
        "prepStmtCacheSize" to "250",
        "prepStmtCacheSqlLimit" to "2048",
        "cachePrepStmts" to "true",
        "useServerPrepStmts" to "true",
        "socketTimeout" to "60000",
        "useLocalSessionState" to "true",
        "rewriteBatchedStatements" to "true",
        "maintainTimeStats" to "false",
    ),
) {
    fun createDataSource(): HikariDataSource {
        val config = HikariConfig()
        if (driver != null) {
            config.driverClassName = driver
        }
        config.jdbcUrl = "$scheme://$hostname:$port/$name"
        config.username = username
        config.password = password
        config.dataSourceProperties = properties.toProperties()
        return HikariDataSource(config)
    }
}

@Serializable
data class Providers(
    @YamlComment(
        "You can specify the providers here.",
        "Values (providers) are separated by comma,",
        "and the first provider will be used first when it's available.",
        "If first provider is not available, second provider will be used, and so on.",
        "If all providers are not available, the plugin throws error which indicates that you need to install or specify the another provider.",
        "",
        "Notes on providers:",
        "  spicyazisaban - Available only if SpicyAzisaBan is installed on Velocity or BungeeCord instance.",
        "  spigot - As the name implies, this provider is only available on Spigot (or its fork, like Paper).",
        "",
        "Valid providers for properties:",
        "  firstJoinedTime: spicyazisaban, spigot",
    )
    val firstJoinedTime: String = "spicyazisaban,spigot",
)
