package net.azisaba.gift.spigot.selectors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.gift.objects.Selector
import net.azisaba.gift.objects.SelectorResult
import org.bukkit.Bukkit
import java.util.UUID

@Serializable
@SerialName("has_permission_on_server")
data class HasPermissionOnServer(val node: String, val invert: Boolean = false) : Selector {
    override suspend fun isSelected(player: UUID): SelectorResult {
        return SelectorResult.of(Bukkit.getPlayer(player)!!.hasPermission(node) xor invert)
    }
}
