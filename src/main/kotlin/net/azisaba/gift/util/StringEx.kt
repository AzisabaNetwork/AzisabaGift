package net.azisaba.gift.util

fun String.truncate(length: Int): String {
    return if (this.length > length) {
        this.substring(0, length) + "..."
    } else {
        this
    }
}
