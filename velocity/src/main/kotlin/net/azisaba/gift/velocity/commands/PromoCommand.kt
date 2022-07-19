package net.azisaba.gift.velocity.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.coroutines.letSuspend
import net.azisaba.gift.objects.CodesTable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger
import java.util.Collections
import java.util.UUID

class PromoCommand(private val logger: Logger) : AbstractCommand() {
    private val executing = Collections.synchronizedList(mutableListOf<UUID>())

    override fun createBuilder(): LiteralArgumentBuilder<CommandSource> =
        literal("promo")
            .requires { it.hasPermission("azisabagift.command.promo") }
            .then(argument("code", StringArgumentType.greedyString())
                .requires { it is Player }
                .executesSuspend { executeWithCode(it.source as Player, StringArgumentType.getString(it, "code")) }
            )

    // TODO: modify used_codes table and add `handled_velocity` and `handled_spigot` column
    private suspend fun executeWithCode(player: Player, code: String): Int {
        if (executing.contains(player.uniqueId)) {
            return 0
        }
        executing.add(player.uniqueId)
        try {
            logger.info("${player.username} (${player.uniqueId}) is trying to use code '$code'")
            // check if code exists and is valid
            val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
            if (codes == null || !codes.isValid()) {
                player.sendMessage(Component.text("このコードは無効か、すでに期限切れです。", NamedTextColor.RED))
                return 0
            }
            // check if player has already used this code
            val used = DatabaseManager.executeQuery(
                "SELECT `player` FROM `used_codes` WHERE `code` = ? AND `player` = ?",
                code,
                player.uniqueId.toString(),
            ).letSuspend { result -> result.use { (rs) -> rs.next() } }
            if (used) {
                logger.info("${player.username} (${player.uniqueId}) is trying to use code '$code' but it has already been used")
                player.sendMessage(Component.text("このコードはすでに使用済みです。", NamedTextColor.RED))
                return 0
            }
            logger.info("Code ($code) is valid, sending gift to ${player.username} (${player.uniqueId})...")
            // mark as used
            DatabaseManager.executeUpdate(
                "INSERT INTO `used_codes` (`player`, `code`) VALUES (?, ?)",
                player.uniqueId.toString(),
                code,
            ).close()
            logger.info("Added used_codes record for player: ${player.username} (${player.uniqueId}), code: $code")
            try {
                codes.handler.handle(player.uniqueId) { it.isAvailableInVelocity }
            } catch (e: Throwable) {
                logger.error("Caught exception while handling code '$code' (Code ID: ${codes.id})", e)
                logger.warn("Failed to redeem code '$code' to ${player.username} (${player.uniqueId}) due to an error")
                player.sendMessage(Component.text("[V] コードの使用に失敗しました。", NamedTextColor.RED))
                return 0
            }
            logger.info("Redeemed code '$code' to ${player.username} (${player.uniqueId})")
            player.sendMessage(Component.text("[V] コードを引き換えました。", NamedTextColor.GREEN))
            return 0
        } finally {
            executing.remove(player.uniqueId)
        }
    }
}
