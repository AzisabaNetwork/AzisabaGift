package net.azisaba.gift.spigot.selectors

import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.objects.Selector
import net.azisaba.gift.objects.SelectorResult
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.UUID

@SerialName("near_location")
@Serializable
data class NearLocationSelector(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val maxDistance: Double,
) : Selector {
    override suspend fun isSelected(player: UUID): SelectorResult =
        withContext(MinecraftDispatcher.syncDispatcher) {
            val bukkitWorld = Bukkit.getWorld(world) ?: error("world '$world' not found")
            val playerLocation = Bukkit.getPlayer(player)?.location ?: error("player is offline")
            val distance = playerLocation.distance(Location(bukkitWorld, x, y, z))
            SelectorResult.of(distance <= maxDistance)
        }
}
