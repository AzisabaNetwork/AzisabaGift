package net.azisaba.gift.velocity.providers

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.providers.DataProviders
import net.azisaba.gift.providers.types.FirstJoinedTimeProvider
import net.azisaba.spicyAzisaBan.struct.PlayerData
import java.util.UUID

object SpicyAzisaBanDataProvider : FirstJoinedTimeProvider {
    init {
        DataProviders.register(FirstJoinedTimeProvider::class, this)
    }

    override suspend fun getFirstJoinedTime(uuid: UUID) =
        coroutineScope {
            withContext(MinecraftDispatcher.asyncDispatcher) {
                PlayerData.getByUUID(uuid).complete().firstLogin ?: 0
            }
        }
}
