package net.azisaba.gift.velocity.bridge

import com.velocitypowered.api.proxy.ProxyServer
import net.azisaba.gift.bridge.Platform
import net.azisaba.gift.bridge.Player
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.UUID

class VelocityPlatform(private val server: ProxyServer) : Platform {
    companion object {
        val legacyComponentSerializer = LegacyComponentSerializer.legacySection()
    }

    init {
        Platform.instance = this
    }

    override fun getPlayer(uuid: UUID): Player? = server.getPlayer(uuid).map(::VelocityPlayer).orElse(null)
}
