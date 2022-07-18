package net.azisaba.gift.spigot.bridge

import net.azisaba.gift.bridge.Platform
import net.azisaba.gift.bridge.Player
import org.bukkit.Bukkit
import java.util.UUID

object SpigotPlatform : Platform {
    init {
        Platform.instance = this
    }

    override fun getPlayer(uuid: UUID): Player? = Bukkit.getPlayer(uuid)?.let { SpigotPlayer(it) }
}
