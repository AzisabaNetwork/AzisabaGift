package net.azisaba.gift

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import net.azisaba.gift.objects.UnknownHandler
import net.azisaba.gift.registry.Registry
import net.azisaba.gift.serializers.DynamicLookupSerializer
import net.azisaba.gift.serializers.UUIDSerializer
import java.util.UUID

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class)
val JSON = Json {
    if (!DatabaseManager.initialized) {
        error("Attempted to initialize JSON too early")
    }

    serializersModule = SerializersModule {
        contextual(Any::class, DynamicLookupSerializer)
        contextual(UUID::class, UUIDSerializer)
        registerPolymorphic(Registry.SELECTOR)
        registerPolymorphic(Registry.HANDLER) { UnknownHandler.Serializer }
        registerPolymorphic(Registry.EXPIRATION_STATUS)
    }
}

/**
 * Register polymorphic serializers using [Registry].
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
private inline fun <reified T : Any> SerializersModuleBuilder.registerPolymorphic(
    registry: Registry<Class<out T>, KSerializer<out T>>,
    noinline defaultSerializerProvider: ((className: String?) -> DeserializationStrategy<out T>?)? = null,
) {
    polymorphic(T::class) {
        if (defaultSerializerProvider != null) {
            defaultDeserializer(defaultSerializerProvider)
        }
        registry
            .getReadonlyMap()
            .mapKeys { (k) -> k as Class<T> }
            .mapValues { (_, v) -> v as KSerializer<T> }
            .forEach { (k, v) -> subclass(k.kotlin, v) }
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
