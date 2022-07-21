package net.azisaba.gift.spigot.handlers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.gift.objects.Handler
import net.azisaba.gift.spigot.SpigotPlugin
import org.black_ixx.playerpoints.PlayerPoints
import java.util.UUID

@SerialName("give_player_points")
@Serializable
data class GivePlayerPoints(val amount: Int) : Handler {
    override fun isAvailableInVelocity(): Boolean = false
    override fun isAvailableInSpigot(): Boolean = true

    override suspend fun handle(uuid: UUID): Boolean {
        try {
            PlayerPoints.getInstance().api.give(uuid, amount)
        } catch (e: Throwable) {
            SpigotPlugin.instance.logger.warning("PlayerPoints is unavailable (tried to give $amount points to $uuid)")
            e.printStackTrace()
            return false
        }
        return true
    }
}
