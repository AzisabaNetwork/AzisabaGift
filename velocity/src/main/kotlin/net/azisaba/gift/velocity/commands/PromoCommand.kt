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

class PromoCommand(private val logger: Logger) : AbstractCommand() {
    override fun createBuilder(): LiteralArgumentBuilder<CommandSource> =
        literal("gift")
            .requires { it.hasPermission("azisabagift.command.promo") }
            .then(argument("code", StringArgumentType.greedyString())
                .requires { it is Player }
                .executesSuspend { executeWithCode(it.source as Player, StringArgumentType.getString(it, "code")) }
            )

    private suspend fun executeWithCode(player: Player, code: String): Int {
        logger.info("${player.username} (${player.uniqueId}) is trying to use code '$code'")
        val codes = CodesTable.find("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
        if (codes == null || !codes.isValid()) {
            player.sendMessage(Component.text("このコードは無効か、すでに期限切れです。", NamedTextColor.RED))
            return 0
        }
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
        if (!codes.handler.isAvailableInVelocity) {
            // TODO: implement command event listener to forward command to backend if it is not available in velocity
            // This shouldn't happen
            error("Code ($code) is valid, but cannot be invoked in this environment")
        }
        logger.info("Code ($code) is valid, sending gift to ${player.username} (${player.uniqueId})...")
        val handled = try {
            codes.handler.handle(player.uniqueId)
        } catch (e: Throwable) {
            logger.error("Caught exception while handling code '$code' (Code ID: ${codes.id})", e)
            false
        }
        if (handled) {
            logger.info("Redeemed code '$code' to ${player.username} (${player.uniqueId})")
            // mark as used
            DatabaseManager.executeUpdate(
                "INSERT INTO `used_codes` (`player`, `code`) VALUES (?, ?)",
                player.uniqueId.toString(),
                code,
            ).close()
            logger.info("Added used_codes record for player: ${player.username} (${player.uniqueId}), code: $code")
            player.sendMessage(Component.text("コードを引き換えました。", NamedTextColor.GREEN))
        } else {
            logger.warn("Failed to redeem code '$code' to ${player.username} (${player.uniqueId}) due to an error")
            player.sendMessage(Component.text("コードの使用に失敗しました。", NamedTextColor.RED))
        }
        return 0
    }
}
