package net.azisaba.gift.spigot.selectors.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.gift.objects.Selector
import net.azisaba.gift.objects.SelectorResult
import net.azisaba.gift.spigot.SpigotPlugin
import net.azisaba.taxoffice.TaxOffice
import java.util.UUID

@Serializable
@SerialName("has_points_in_tax_office")
data class HasPointsInTaxOffice(val points: Long) : Selector {
    override suspend fun isSelected(player: UUID): SelectorResult {
        return try {
            val pointsManager = TaxOffice.getInstance().pointsManager
            SelectorResult.of(pointsManager.getPoints(player) >= points)
        } catch (e: Exception) {
            SpigotPlugin.instance.logger.warning("TaxOffice is unavailable ($this)")
            e.printStackTrace()
            SelectorResult.FALSE
        }
    }
}
