package net.azisaba.gift

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.azisaba.gift.objects.Selector
import net.azisaba.gift.serializers.DynamicLookupSerializer
import net.azisaba.gift.serializers.UUIDSerializer
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
val JSON = Json {
    serializersModule = SerializersModule {
        contextual(Any::class, DynamicLookupSerializer)
        contextual(UUID::class, UUIDSerializer)
        fun PolymorphicModuleBuilder<Selector>.registerProjectSubclasses() {
            subclass(Selector::class)
        }
        polymorphic(Any::class) { registerProjectSubclasses() }
        polymorphic(Selector::class) { registerProjectSubclasses() }
    }
}

// hacks (workaround) for kotlinx.serialization messing with map of <String, Any?>
// https://github.com/Kotlin/kotlinx.serialization/issues/296#issuecomment-1132714147
// (related: https://youtrack.jetbrains.com/issue/KTOR-3063)

fun Collection<*>.toJsonElement(): JsonElement = JsonArray(mapNotNull { it.toJsonElement() })

fun Map<*, *>.toJsonElement(): JsonElement = JsonObject(
    mapNotNull {
        (it.key as? String ?: return@mapNotNull null) to it.value.toJsonElement()
    }.toMap(),
)

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is Map<*, *> -> toJsonElement()
    is Collection<*> -> toJsonElement()
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Enum<*> -> JsonPrimitive(this.toString())
    else -> throw IllegalStateException("Can't serialize unknown type: $this (${this::class.java.typeName})")
}
