package net.azisaba.gift.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface PlatformConfig {
    @SerialName("empty")
    @Serializable
    object Empty : PlatformConfig
}
