package net.azisaba.gift.spigot.providers

import kotlinx.coroutines.withContext
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.providers.DataProviders
import net.azisaba.gift.providers.types.FirstJoinedTimeProvider
import org.bukkit.Bukkit
import java.util.UUID

object SpigotDataProvider : FirstJoinedTimeProvider {
    init {
        DataProviders.register(FirstJoinedTimeProvider::class, this)
    }

    override suspend fun getFirstJoinedTime(uuid: UUID): Long =
        withContext(MinecraftDispatcher.syncDispatcher) {
            Bukkit.getPlayer(uuid)?.firstPlayed ?: 0L
        }
}
