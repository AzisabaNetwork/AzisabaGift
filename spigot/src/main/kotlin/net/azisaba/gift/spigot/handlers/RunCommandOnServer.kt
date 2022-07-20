package net.azisaba.gift.spigot.handlers

import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.objects.Handler
import org.bukkit.Bukkit
import java.util.UUID

/**
 * Runs a command on server.
 * @param commandWithoutSlash The command without the slash. %player_uuid% and %player_name% can be used here.
 */
@SerialName("run_command_on_server")
@Serializable
data class RunCommandOnServer(
    @SerialName("command")
    val commandWithoutSlash: String,
    @SerialName("run_as_console")
    val runAsConsole: Boolean = false,
) : Handler {
    override fun isAvailableInVelocity(): Boolean = false
    override fun isAvailableInSpigot(): Boolean = true

    override suspend fun handle(uuid: UUID): Boolean {
        // we do getPlayer regardless of runAsConsole property,
        // because we want to prevent the player from breaking the command by quitting.
        val player = Bukkit.getPlayer(uuid) ?: return false
        withContext(MinecraftDispatcher.syncDispatcher) {
            Bukkit.dispatchCommand(
                if (runAsConsole) Bukkit.getConsoleSender() else player,
                commandWithoutSlash
                    .replace("%player_uuid%", uuid.toString())
                    .replace("%player_name%", player.name),
            )
        }
        return true
    }
}
