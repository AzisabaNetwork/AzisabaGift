package net.azisaba.gift.spigot

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

fun ItemStack.toByteArray(): ByteArray = ByteArrayOutputStream().use { byteOutput ->
    BukkitObjectOutputStream(byteOutput).use { dataOutput ->
        dataOutput.writeObject(this)
    }
    byteOutput.toByteArray()
}

fun ByteArray.toItemStack(): ItemStack = ByteArrayInputStream(this).use { byteInput ->
    BukkitObjectInputStream(byteInput).use { dataInput ->
        dataInput.readObject() as ItemStack
    }
}

fun ByteArray.encodeToBase64String() = Base64.getEncoder().encodeToString(this)!!

fun String.decodeFromBase64String() = Base64.getDecoder().decode(this)!!

fun ItemStack.toFriendlyOutput(): String {
    val displayName = if (hasItemMeta() && itemMeta.hasDisplayName()) itemMeta.displayName else "null"
    val type = type.name
    val name = displayName ?: type
    return "$name (type: $type, amount: $amount)"
}
