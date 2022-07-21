package net.azisaba.gift.spigot.listeners

import net.azisaba.gift.spigot.SpigotPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent

/**
 * Prevents the unauthorized entities from picking up the items when "gift-owner" metadata is set to item entity.
 */
object ItemOwnerListener : Listener {
    @EventHandler
    fun onEntityPickupItem(e: EntityPickupItemEvent) {
        if (!e.item.hasMetadata("gift-owner")) return
        e.item.getMetadata("gift-owner")?.forEach {
            if (SpigotPlugin.instance == it.owningPlugin && it.value() != "${e.entity.uniqueId}") {
                e.isCancelled = true
                return
            }
        }
    }
}
