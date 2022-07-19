package net.azisaba.gift.objects

import kotlinx.serialization.Serializable

@Serializable
data class CodesData(val expirationStatus: ExpirationStatus) {
    fun swapExpirationStatus(expirationStatus: ExpirationStatus) =
        CodesData(expirationStatus)
}
