package net.azisaba.gift.spigot.handlers

import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.objects.Handler
import net.azisaba.gift.spigot.decodeFromBase64String
import net.azisaba.gift.spigot.encodeToBase64String
import net.azisaba.gift.spigot.toByteArray
import net.azisaba.gift.spigot.toItemStack
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.UUID

@SerialName("give_items")
@Serializable
data class GiveItems(val items: List<String>) : Handler {
    override val isAvailableInVelocity = false
    override val isAvailableInSpigot = true

    override suspend fun handle(uuid: UUID): Boolean {
        val player = Bukkit.getPlayer(uuid) ?: return false
        return withContext(MinecraftDispatcher.syncDispatcher) {
            val emptySlots = player.inventory.filter { it == null || it.type == Material.AIR || it.amount == 0 }.size
            if (emptySlots < items.size) {
                player.sendMessage("${ChatColor.RED}受け取るには${ChatColor.YELLOW}${items.size}${ChatColor.RED}個以上のインベントリの空きが必要です。")
                return@withContext false
            }
            player.inventory
                .addItem(*getBukkitItems().toTypedArray())
                .forEach { (_, item) -> player.world.dropItem(player.location, item) }
            return@withContext true
        }
    }

    fun getBukkitItems() = items.map { it.decodeFromBase64String().toItemStack() }
}

fun GiveItems(items: List<ItemStack>) = GiveItems(items.map { it.toByteArray().encodeToBase64String() })

/**
 * Runs a command on server.
 * @param commandWithoutSlash The command without the slash. %player_uuid% and %player_name% can be used here.
 */
@Serializable
data class RunCommandOnServer(val commandWithoutSlash: String) : Handler {
    override val isAvailableInVelocity = false
    override val isAvailableInSpigot = true

    override suspend fun handle(uuid: UUID): Boolean {
        val player = Bukkit.getPlayer(uuid) ?: return false
        withContext(MinecraftDispatcher.syncDispatcher) {
            Bukkit.dispatchCommand(
                player,
                commandWithoutSlash
                    .replace("%player_uuid%", uuid.toString())
                    .replace("%player_name%", player.name),
            )
        }
        return true
    }
}
