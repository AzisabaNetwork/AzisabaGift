package net.azisaba.gift.spigot.handlers

import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.objects.Handler
import org.bukkit.Bukkit
import java.util.UUID

/**
 * Runs a command on server.
 * @param commandWithoutSlash The command without the slash. %player_uuid% and %player_name% can be used here.
 */
@Serializable
data class RunCommandOnServer(val commandWithoutSlash: String) : Handler {
    override fun isAvailableInVelocity(): Boolean = false
    override fun isAvailableInSpigot(): Boolean = true

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
