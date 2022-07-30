package net.azisaba.gift.objects

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.azisaba.gift.JSONWithoutRegistry
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.providers.DataProviders
import net.azisaba.gift.providers.types.FirstJoinedTimeProvider
import net.azisaba.gift.registry.Registry
import net.azisaba.gift.registry.registerK
import java.util.UUID

enum class SelectorResult {
    TRUE,
    FALSE,
    SKIP;

    companion object {
        fun of(expression: Boolean): SelectorResult =
            if (expression) {
                TRUE
            } else {
                FALSE
            }
    }
}

interface Selector {
    companion object {
        init {
            // Make sure to register serializer when you create a new selector, or you will not be able to (de)serialize it.
            Registry.SELECTOR.registerK(CompoundSelector::class, CompoundSelector.serializer())
            Registry.SELECTOR_DEFAULT_VALUE.registerK(
                CompoundSelector::class,
                JSONWithoutRegistry.encodeToString(CompoundSelector(emptyList(), false)),
            )
            Registry.SELECTOR.registerK(Everyone::class, Everyone.serializer())
            Registry.SELECTOR.registerK(Nobody::class, Nobody.serializer())
            Registry.SELECTOR.registerK(SinglePlayer::class, SinglePlayer.serializer())
            Registry.SELECTOR_DEFAULT_VALUE.registerK(
                SinglePlayer::class,
                JSONWithoutRegistry.encodeToString(SinglePlayer(UUID.fromString("9c2ac958-5de9-45a8-8ca1-4122eb4c0b9e"))),
            )
            Registry.SELECTOR.registerK(MultiplePlayers::class, MultiplePlayers.serializer())
            Registry.SELECTOR_DEFAULT_VALUE.registerK(
                MultiplePlayers::class,
                JSONWithoutRegistry.encodeToString(MultiplePlayers(listOf(
                    UUID.fromString("9c2ac958-5de9-45a8-8ca1-4122eb4c0b9e"),
                    UUID.fromString("61699b2e-d327-4a01-9f1e-0ea8c3f06bc6"),
                ))),
            )
            Registry.SELECTOR.registerK(FirstJoinedAfter::class, FirstJoinedAfter.serializer())
            Registry.SELECTOR_DEFAULT_VALUE.registerK(
                FirstJoinedAfter::class,
                JSONWithoutRegistry.encodeToString(FirstJoinedAfter(System.currentTimeMillis())),
            )
            Registry.SELECTOR.registerK(FirstJoinedBefore::class, FirstJoinedBefore.serializer())
            Registry.SELECTOR_DEFAULT_VALUE.registerK(
                FirstJoinedBefore::class,
                JSONWithoutRegistry.encodeToString(FirstJoinedBefore(System.currentTimeMillis())),
            )
        }
    }

    suspend fun isSelected(player: UUID): SelectorResult
}

@Serializable
@SerialName("compound")
data class CompoundSelector(val selectors: List<Selector> = emptyList(), val or: Boolean = false) : Selector {
    override suspend fun isSelected(player: UUID): SelectorResult {
        val result = if (or) {
            selectors.any {
                val localResult = it.isSelected(player)
                if (localResult == SelectorResult.SKIP) return SelectorResult.SKIP
                localResult == SelectorResult.TRUE
            }
        } else {
            selectors.all {
                val localResult = it.isSelected(player)
                if (localResult == SelectorResult.SKIP) return SelectorResult.SKIP
                localResult == SelectorResult.TRUE
            }
        }
        return SelectorResult.of(result)
    }
}

@Serializable
@SerialName("everyone")
object Everyone : Selector {
    override suspend fun isSelected(player: UUID) = SelectorResult.TRUE
}

@Serializable
@SerialName("nobody")
object Nobody : Selector {
    override suspend fun isSelected(player: UUID) = SelectorResult.FALSE
}

@Serializable
@SerialName("single_player")
data class SinglePlayer(@Contextual val player: UUID) : Selector {
    override suspend fun isSelected(player: UUID) = SelectorResult.of(player == this.player)
}

@Serializable
@SerialName("multiple_players")
data class MultiplePlayers(val players: List<@Contextual UUID>) : Selector {
    override suspend fun isSelected(player: UUID) = SelectorResult.of(players.contains(player))
}

@Serializable
@SerialName("first_joined_after")
data class FirstJoinedAfter(val time: Long) : Selector {
    companion object {
        // provider for first join time
        val provider = DataProviders.getSelected(
            FirstJoinedTimeProvider::class.java,
            PluginConfig.instance.providers.firstJoinedTime.split(","),
        )::getFirstJoinedTime
    }

    override suspend fun isSelected(player: UUID) = SelectorResult.of(provider(player) > time)
}

@Serializable
@SerialName("first_joined_before")
data class FirstJoinedBefore(val time: Long) : Selector {
    override suspend fun isSelected(player: UUID) = SelectorResult.of(FirstJoinedAfter.provider(player) < time)
}

data class UnknownSelector(val json: JsonObject) : Selector {
    override suspend fun isSelected(player: UUID): SelectorResult = SelectorResult.SKIP

    object Serializer : KSerializer<UnknownSelector> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UnknownHandler") {
            element<JsonElement>("json")
        }

        override fun deserialize(decoder: Decoder): UnknownSelector {
            val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
            val json = jsonInput.decodeJsonElement().jsonObject
            return UnknownSelector(JsonObject(json))
        }

        override fun serialize(encoder: Encoder, value: UnknownSelector) {
            val jsonOutput = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
            jsonOutput.encodeJsonElement(value.json)
        }
    }
}
