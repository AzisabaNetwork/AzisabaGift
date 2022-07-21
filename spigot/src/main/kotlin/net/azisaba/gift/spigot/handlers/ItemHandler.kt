package net.azisaba.gift.spigot.handlers

import net.azisaba.gift.objects.Handler
import org.bukkit.inventory.ItemStack

interface ItemHandler : Handler {
    fun getItemList(): List<ItemStack>

    fun setItemList(itemList: List<ItemStack>)

    fun getMaxItemListSize(): Int? = null
}
