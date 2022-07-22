package net.azisaba.gift.objects

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
import net.azisaba.gift.bridge.Platform
import net.azisaba.gift.registry.Registry
import net.azisaba.gift.registry.registerK
import java.util.UUID

interface Handler {
    companion object {
        init {
            Registry.HANDLER.registerK(SendMessage::class, SendMessage.serializer())
            Registry.HANDLER_DEFAULT_VALUE.registerK(
                SendMessage::class,
                JSONWithoutRegistry.encodeToString(SendMessage("Hello, world!")),
            )
            Registry.HANDLER.registerK(UnknownHandler::class, UnknownHandler.Serializer)
        }
    }

    fun isAvailableInVelocity(): Boolean

    fun isAvailableInSpigot(): Boolean

    /**
     * @param uuid UUID of the player
     * @return `true` if handled and used_codes will be inserted. `false` if not handled and used_codes will not be inserted
     */
    suspend fun handle(uuid: UUID): Boolean /* throws Throwable */
}

@SerialName("send_message")
@Serializable
data class SendMessage(val message: String) : Handler {
    override fun isAvailableInVelocity(): Boolean = true
    override fun isAvailableInSpigot(): Boolean = true

    override suspend fun handle(uuid: UUID): Boolean {
        Platform.getPlayer(uuid)?.sendMessage(message) ?: return false
        return true
    }
}

data class UnknownHandler(val json: JsonObject) : Handler {
    override fun isAvailableInVelocity(): Boolean = false
    override fun isAvailableInSpigot(): Boolean = false

    override suspend fun handle(uuid: UUID): Boolean {
        return false
    }

    object Serializer : KSerializer<UnknownHandler> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UnknownHandler") {
            element<JsonElement>("json")
        }

        override fun deserialize(decoder: Decoder): UnknownHandler {
            val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
            val json = jsonInput.decodeJsonElement().jsonObject
            return UnknownHandler(JsonObject(json))
        }

        override fun serialize(encoder: Encoder, value: UnknownHandler) {
            val jsonOutput = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
            jsonOutput.encodeJsonElement(value.json)
        }
    }
}
