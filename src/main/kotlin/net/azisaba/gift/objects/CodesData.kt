package net.azisaba.gift.objects

import kotlinx.serialization.Serializable
import org.intellij.lang.annotations.Language

@Serializable
data class CodesData(
    val expirationStatus: ExpirationStatus = ExpirationStatus.NeverExpire,
    @Language("RegExp")
    val allowedOnServer: String = ".*",
) {
    private val allowedOnServerRegex
        get() = allowedOnServer.toRegex()

    fun isServerAllowed(serverName: String) =
        allowedOnServerRegex.matches(serverName)
}
