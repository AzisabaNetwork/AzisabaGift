package net.azisaba.gift.spigot.commands

import kotlinx.coroutines.asExecutor
import kotlinx.serialization.ExperimentalSerializationApi
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.JSON
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.objects.CodesTable
import net.azisaba.gift.objects.ExpirationStatus
import net.azisaba.gift.registry.Registry
import net.azisaba.gift.util.executeAsync
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import java.util.logging.Logger

val commands = listOf("modify", "debug")

val modifyActions = listOf("setExpirationStatus")

class AzisabaGiftCommand(private val logger: Logger) : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>,
    ): Boolean {
        if (args.isEmpty()) {
            return false
        }
        MinecraftDispatcher.asyncDispatcher.asExecutor().executeAsync {
            when (args[0]) {
                "modify" -> {
                    if (args.size < 3) {
                        sender.sendMessage("${ChatColor.RED}/azisabagift modify <code> <action>")
                        return@executeAsync
                    }
                    executeModify(sender, args[1], args[2], args.drop(3))
                }
            }
        }
        return true
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun executeModify(sender: CommandSender, code: String, action: String, args: List<String>) {
        val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
        if (codes == null) {
            sender.sendMessage("${ChatColor.RED}コードが見つかりません。")
            return
        }
        if (action == "setExpirationStatus") {
            val expirationStatus = if (ExpirationStatus.ExpireAfterUse.serializer().descriptor.serialName == args[0]) {
                ExpirationStatus.ExpireAfterUse(args[1].toInt())
            } else if (ExpirationStatus.ExpiresAt.serializer().descriptor.serialName == args[0]) {
                ExpirationStatus.ExpiresAt(args[1].toLong())
            } else {
                val serializer = Registry.EXPIRATION_STATUS
                    .getReadonlyMap()
                    .entries
                    .find { (_, serializer) -> serializer.descriptor.serialName == args[0] }
                    ?.value
                if (serializer == null) {
                    sender.sendMessage("${ChatColor.RED}指定されたExpirationStatusは無効です。")
                    return
                }
                JSON.decodeFromString(serializer, args.drop(1).joinToString(" "))
            }
            val newData = codes.data.copy(expirationStatus = expirationStatus)
            DatabaseManager.executeUpdate("UPDATE `codes` SET `data` = ? WHERE `code` = ?", newData, code).close()
            sender.sendMessage("${ChatColor.GREEN}コードの有効期限を${expirationStatus}に変更しました。")
        } else {
            return
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>,
    ): List<String> {
        if (args.size == 1) {
            return commands.filter(args.last())
        }
        if (args.size == 2) {
            when (args[0]) {
                "modify" -> listOf("<code>")
            }
        }
        if (args.size == 3) {
            when (args[0]) {
                "modify" -> modifyActions.filter(args.last())
            }
        }
        if (args.size == 4) {
            when (args[0]) {
                "modify" -> when (args[2]) {
                    "setExpirationStatus" -> Registry.EXPIRATION_STATUS.getValues().map { it.descriptor.serialName }
                }
            }
        }
        if (args.size == 5) {
            when (args[0]) {
                "modify" -> when (args[2]) {
                    "setExpirationStatus" -> when (args[3]) {
                        ExpirationStatus.ExpireAfterUse.serializer().descriptor.serialName -> listOf("<count>")
                        ExpirationStatus.ExpiresAt.serializer().descriptor.serialName -> listOf("<expiresAtMillis>")
                        else -> listOf("[\"data\", \"in\", \"JSON\", \"format\"]")
                    }
                }
            }
        }
        return emptyList()
    }

    private fun List<String>.filter(s: String) = filter { it.lowercase().startsWith(s.lowercase()) }
}
