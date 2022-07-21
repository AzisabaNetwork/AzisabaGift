package net.azisaba.gift.objects

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.JSON
import net.azisaba.gift.coroutines.letSuspend
import net.azisaba.gift.serializers.UUIDSerializer
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import java.util.UUID

object CodesTable : Table<Codes>(Codes::class) {
    override val toValues: suspend (rs: ResultSet) -> Collection<Codes> = { rs ->
        val values = mutableListOf<Codes>()
        while (rs.next()) {
            values.add(Codes(
                rs.getLong("id"),
                rs.getString("code"),
                JSON.decodeFromString(rs.getString("selector")),
                JSON.decodeFromString(rs.getString("handler")),
                JSON.decodeFromString(rs.getString("data")),
            ))
        }
        values
    }

    @Language("SQL")
    val createTable = """
        CREATE TABLE IF NOT EXISTS `codes` (
            `id` BIGINT NOT NULL AUTO_INCREMENT,
            `code` VARCHAR(255) NOT NULL UNIQUE,
            `selector` MEDIUMTEXT NOT NULL,
            `handler` LONGTEXT NOT NULL,
            `data` MEDIUMTEXT NOT NULL,
            PRIMARY KEY (id)
        )
    """.trimIndent()
}

@Serializable
data class Codes(
    val id: Long,
    val code: String,
    val selector: Selector,
    val handler: HandlerList,
    val data: CodesData,
) {
    suspend fun isValid() =
        when (data.expirationStatus) {
            is ExpirationStatus.ExpireAfterUse -> data.expirationStatus.expireAfterUse > getCodeUses()
            else -> data.expirationStatus.isValid()
        }

    suspend fun getCodeUses() =
        DatabaseManager.executeQuery("SELECT COUNT(*) FROM `used_codes` WHERE `code` = ?", code).letSuspend { result ->
            result.use { (rs) ->
                if (!rs.next()) {
                    return@letSuspend 0
                }
                println(rs.getLong(1))
                return@letSuspend rs.getLong(1)
            }
        }
}

object UsedCodesTable : Table<UsedCodes>(UsedCodes::class) {
    override val toValues: suspend (rs: ResultSet) -> Collection<UsedCodes> = { rs ->
        val values = mutableListOf<UsedCodes>()
        while (rs.next()) {
            values.add(UsedCodes(
                UUID.fromString(rs.getString("player")),
                rs.getString("code"),
                rs.getBoolean("handled_velocity"),
                rs.getBoolean("handled_spigot"),
            ))
        }
        values
    }

    @Language("SQL")
    val createTable = """
        CREATE TABLE IF NOT EXISTS `used_codes` (
            `player` VARCHAR(36) NOT NULL,
            `code` VARCHAR(255) NOT NULL,
            `handled_velocity` TINYINT(1) NOT NULL DEFAULT 0,
            `handled_spigot` TINYINT(1) NOT NULL DEFAULT 0,
            PRIMARY KEY (player, code)
        )
    """.trimIndent()
}

@Serializable
data class UsedCodes(
    @Serializable(with = UUIDSerializer::class)
    val player: UUID,
    val code: String,
    val handled_velocity: Boolean,
    val handled_spigot: Boolean,
)
