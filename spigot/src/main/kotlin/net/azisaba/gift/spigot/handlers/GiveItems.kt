package net.azisaba.gift.spigot.handlers

import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.spigot.SpigotPlugin
import net.azisaba.gift.spigot.decodeFromBase64String
import net.azisaba.gift.spigot.encodeToBase64String
import net.azisaba.gift.spigot.toByteArray
import net.azisaba.gift.spigot.toItemStack
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import java.util.UUID

@SerialName("give_items")
@Serializable
data class GiveItems(var items: List<String> = emptyList()) : ItemHandler {
    override fun isAvailableInVelocity(): Boolean = false
    override fun isAvailableInSpigot(): Boolean = true

    override suspend fun handle(uuid: UUID): Boolean {
        val player = Bukkit.getPlayer(uuid) ?: return false
        return withContext(MinecraftDispatcher.syncDispatcher) {
            /*
            val emptySlots = player.inventory.filter { it == null || it.type == Material.AIR || it.amount == 0 }.size
            if (emptySlots < items.size) {
                player.sendMessage("${ChatColor.RED}受け取るには${ChatColor.YELLOW}${items.size}${ChatColor.RED}個以上のインベントリの空きが必要です。")
                return@withContext false
            }
            */
            player.inventory
                .addItem(*getBukkitItems().toTypedArray())
                .forEach { (_, item) ->
                    player.world.dropItem(player.location, item).apply {
                        pickupDelay = 1
                        this.setMetadata("gift-owner", FixedMetadataValue(SpigotPlugin.instance, "${player.uniqueId}"))
                    }
                }
            return@withContext true
        }
    }

    fun getBukkitItems() = items.map { it.decodeFromBase64String().toItemStack() }

    override fun getItemList(): List<ItemStack> = getBukkitItems()

    override fun setItemList(itemList: List<ItemStack>) {
        this.items = itemList.map { it.toByteArray().encodeToBase64String() }
    }
}

fun GiveItems(items: List<ItemStack>) = GiveItems(items.map { it.toByteArray().encodeToBase64String() })
