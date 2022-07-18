package net.azisaba.gift.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CodesData(val expirationStatus: ExpirationStatus)

sealed interface ExpirationStatus {
    suspend fun isValid(): Boolean

    @SerialName("never_expire")
    @Serializable
    object NeverExpire : ExpirationStatus {
        override suspend fun isValid() = true
    }

    @SerialName("expire_after_use")
    @Serializable
    class ExpireAfterUse(val expireAfterUse: Int) : ExpirationStatus {
        init {
            if (expireAfterUse < 1) {
                throw IllegalArgumentException("expireAfterUse must be greater than 0")
            }
        }

        override suspend fun isValid() = error("Must be checked from caller")
    }

    @SerialName("expires_at")
    @Serializable
    data class ExpiresAt(val expiresAt: Long) : ExpirationStatus {
        override suspend fun isValid(): Boolean = expiresAt > System.currentTimeMillis()
    }

    @SerialName("expired")
    @Serializable
    object Expired : ExpirationStatus {
        override suspend fun isValid() = false
    }

    @SerialName("revoked")
    @Serializable
    object Revoked : ExpirationStatus {
        override suspend fun isValid() = false
    }
}
