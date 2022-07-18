package net.azisaba.gift.spigot.bridge

import net.azisaba.gift.bridge.Player

class SpigotPlayer(private val player: org.bukkit.entity.Player) : Player {
    override fun sendMessage(message: String) {
        player.sendMessage(message)
    }
}
