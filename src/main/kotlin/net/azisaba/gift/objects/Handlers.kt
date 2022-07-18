package net.azisaba.gift.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.gift.bridge.Platform
import java.util.UUID

interface Handler {
    val isAvailableInVelocity: Boolean
    val isAvailableInSpigot: Boolean

    /**
     * @param uuid UUID of the player
     * @return `true` if handled and used_codes will be inserted. `false` if not handled and used_codes will not be inserted
     */
    suspend fun handle(uuid: UUID): Boolean /* throws Throwable */
}

@SerialName("debug_message")
@Serializable
data class DebugMessage(val message: String) : Handler {
    override val isAvailableInSpigot = true
    override val isAvailableInVelocity = true

    override suspend fun handle(uuid: UUID): Boolean {
        Platform.getPlayer(uuid)?.sendMessage(message) ?: return false
        return true
    }
}
