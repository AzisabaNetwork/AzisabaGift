package net.azisaba.gift.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.azisaba.gift.registry.Registry
import net.azisaba.gift.registry.registerK

sealed interface ExpirationStatus {
    companion object {
        init {
            Registry.EXPIRATION_STATUS.registerK(NeverExpire::class, NeverExpire.serializer())
            Registry.EXPIRATION_STATUS.registerK(ExpireAfterUse::class, ExpireAfterUse.serializer())
            Registry.EXPIRATION_STATUS.registerK(ExpiresAt::class, ExpiresAt.serializer())
            Registry.EXPIRATION_STATUS.registerK(Expired::class, Expired.serializer())
            Registry.EXPIRATION_STATUS.registerK(Revoked::class, Revoked.serializer())
        }
    }

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
