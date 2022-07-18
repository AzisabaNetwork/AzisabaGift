package net.azisaba.gift.spigot.providers

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.providers.types.FirstJoinedTimeProvider
import org.bukkit.Bukkit
import java.util.UUID

object SpigotDataProvider : FirstJoinedTimeProvider {
    // TODO: Is #getPlayer and #getOfflinePlayer safe to use asynchronously?
    override suspend fun getFirstJoinedTime(uuid: UUID): Long =
        coroutineScope {
            withContext(MinecraftDispatcher.asyncDispatcher) {
                Bukkit.getPlayer(uuid)?.firstPlayed
                    ?: Bukkit.getOfflinePlayer(uuid)?.firstPlayed
                    ?: 0L
            }
        }
}
