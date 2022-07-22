package net.azisaba.gift

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.azisaba.gift.serializers.DynamicLookupSerializer
import net.azisaba.gift.serializers.UUIDSerializer
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
val JSONWithoutRegistry = Json {
    encodeDefaults = true

    serializersModule = SerializersModule {
        contextual(UUID::class, UUIDSerializer)
        contextual(Any::class, DynamicLookupSerializer)
    }
}
