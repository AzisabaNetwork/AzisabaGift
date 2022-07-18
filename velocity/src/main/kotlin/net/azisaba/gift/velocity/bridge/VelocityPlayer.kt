package net.azisaba.gift.velocity.bridge

import net.azisaba.gift.bridge.Player

class VelocityPlayer(private val player: com.velocitypowered.api.proxy.Player): Player {
    override fun sendMessage(message: String) {
        player.sendMessage(VelocityPlatform.legacyComponentSerializer.deserialize(message))
    }
}
