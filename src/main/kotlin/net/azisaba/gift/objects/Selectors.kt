package net.azisaba.gift.objects

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.providers.DataProviders
import net.azisaba.gift.providers.types.FirstJoinedTimeProvider
import java.util.UUID

interface Selector {
    suspend fun isSelected(player: UUID): Boolean
}

@Serializable
@SerialName("single_player")
data class SinglePlayer(@Contextual val player: UUID) : Selector {
    override suspend fun isSelected(player: UUID): Boolean = player == this.player
}

@Serializable
@SerialName("multiple_players")
data class MultiplePlayers(val players: List<@Contextual UUID>) : Selector {
    override suspend fun isSelected(player: UUID): Boolean = players.contains(player)
}

@Serializable
@SerialName("first_joined_after")
data class FirstJoinedAfter(val time: Long) : Selector {
    companion object {
        // provider for first join time
        val provider = DataProviders.getSelected(
            FirstJoinedTimeProvider::class.java,
            PluginConfig.instance.providers.firstJoinedTime.split(","),
        )::getFirstJoinedTime
    }

    override suspend fun isSelected(player: UUID): Boolean = provider(player) > time
}

@Serializable
@SerialName("first_joined_before")
data class FirstJoinedBefore(val time: Long) : Selector {
    override suspend fun isSelected(player: UUID): Boolean = FirstJoinedAfter.provider(player) < time
}
