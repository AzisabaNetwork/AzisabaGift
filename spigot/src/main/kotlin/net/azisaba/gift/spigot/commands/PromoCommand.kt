package net.azisaba.gift.spigot.commands

import kotlinx.coroutines.runBlocking
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.config.SpigotPlatformConfig
import net.azisaba.gift.objects.CodesTable
import net.azisaba.gift.objects.UsedCodesTable
import net.azisaba.gift.spigot.SpigotPlugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.util.Collections
import java.util.UUID
import java.util.logging.Logger

class PromoCommand(private val logger: Logger) : TabExecutor {
    private val executing = Collections.synchronizedList(mutableListOf<UUID>())

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>,
    ): Boolean {
        if (label.equals("azisabagift:promo", true)) {
            return false
        }
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}このコマンドはコンソールから実行できません。")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}/promo <コード>")
            return true
        }
        val code = args.joinToString(" ")
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.instance) {
            runBlocking {
                try {
                    if (executing.contains(sender.uniqueId)) {
                        return@runBlocking
                    }
                    executing.add(sender.uniqueId)
                    try {
                        logger.info("${sender.name} (${sender.uniqueId}) is trying to use code '$code'")
                        // check if code exists and is valid
                        val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
                        if (codes == null || !codes.isValid() || !codes.selector.isSelected(sender.uniqueId)) {
                            sender.sendMessage("${ChatColor.RED}このコードは無効か、すでに期限切れです。")
                            return@runBlocking
                        }
                        // server-name works as an extra guard for this. If server-name is absent, the command will always be allowed.
                        val isServerAllowed = (PluginConfig.instance.platformConfig as? SpigotPlatformConfig)
                            ?.serverName
                            ?.let { codes.data.isServerAllowed(it) } ?: true
                        if (!isServerAllowed) {
                            sender.sendMessage("${ChatColor.RED}このコードは無効か、すでに期限切れです。")
                            return@runBlocking
                        }
                        // check if player has already used this code
                        val used = UsedCodesTable.select(
                            "SELECT * FROM `used_codes` WHERE `player` = ? AND `code` = ?",
                            sender.uniqueId.toString(),
                            code,
                        ).firstOrNull()
                        if (used != null && used.handled_spigot) {
                            logger.info("${sender.name} (${sender.uniqueId}) is trying to use code '$code' but it has already been used")
                            sender.sendMessage("${ChatColor.RED}[S] このコードはすでに使用済みです。")
                            return@runBlocking
                        }
                        logger.info("Code ($code) is valid, sending gift to ${sender.name} (${sender.uniqueId})...")
                        // mark as used
                        DatabaseManager.executeUpdate(
                            "INSERT INTO `used_codes` (`player`, `code`, `handled_spigot`) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE `handled_spigot` = 1",
                            sender.uniqueId.toString(),
                            code,
                        ).close()
                        logger.info("Added used_codes record for player: ${sender.name} (${sender.uniqueId}), code: $code")
                        try {
                            codes.handler.handle(sender.uniqueId) {
                                if (it.isAvailableInSpigot() && it.isAvailableInVelocity()) {
                                    used?.handled_velocity != true
                                } else {
                                    it.isAvailableInSpigot()
                                }
                            }
                        } catch (e: Throwable) {
                            logger.severe("Caught exception while handling code '$code' (Code ID: ${codes.id})")
                            e.printStackTrace()
                            logger.warning("Failed to redeem code '$code' to ${sender.name} (${sender.uniqueId}) due to an error")
                            sender.sendMessage("${ChatColor.RED}[S] コードの使用に失敗しました。")
                            return@runBlocking
                        }
                        logger.info("Redeemed code '$code' to ${sender.name} (${sender.uniqueId})")
                        sender.sendMessage("${ChatColor.GREEN}[S] コードを引き換えました。")
                        return@runBlocking
                    } finally {
                        executing.remove(sender.uniqueId)
                    }
                } catch (e: Throwable) {
                    logger.severe("Caught exception while handling code '$code'")
                    e.printStackTrace()
                    logger.warning("Failed to redeem code '$code' to ${sender.name} (${sender.uniqueId}) due to an error")
                    sender.sendMessage("${ChatColor.RED}[S] コードの使用に失敗しました。")
                    return@runBlocking
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>,
    ): List<String> = emptyList()
}
