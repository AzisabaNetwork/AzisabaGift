package net.azisaba.gift.spigot.bridge

import net.azisaba.gift.bridge.Player
import org.bukkit.ChatColor

class SpigotPlayer(private val player: org.bukkit.entity.Player) : Player {
    override fun sendMessage(message: String) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message))
    }
}
