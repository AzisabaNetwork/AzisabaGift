package net.azisaba.gift.config

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("spigot")
@Serializable
data class SpigotPlatformConfig(
    @YamlComment(
        "The name of the server",
        "This config is used to identify the server when checking with allowedServer regex in code settings.",
    )
    val serverName: String = "please change me",
) : PlatformConfig
