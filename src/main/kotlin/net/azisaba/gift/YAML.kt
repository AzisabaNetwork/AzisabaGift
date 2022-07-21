package net.azisaba.gift

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import net.azisaba.gift.config.PlatformConfig
import net.azisaba.gift.config.SpigotPlatformConfig

val YAML = Yaml(
    SerializersModule {
        polymorphic(PlatformConfig::class) {
            subclass(PlatformConfig.Empty::class, PlatformConfig.Empty.serializer())
            subclass(SpigotPlatformConfig::class, SpigotPlatformConfig.serializer())
        }
    },
    YamlConfiguration(polymorphismStyle = PolymorphismStyle.Property),
)
