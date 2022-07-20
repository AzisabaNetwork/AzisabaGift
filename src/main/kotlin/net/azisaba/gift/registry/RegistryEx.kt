package net.azisaba.gift.registry

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

fun <K : Any, V : Any> Registry<Class<out K>, V>.registerK(clazz: KClass<out K>, value: V) {
    register(clazz.java, value)
}

@OptIn(ExperimentalSerializationApi::class)
fun <K : Any, V : Any> Registry<K, KSerializer<out V>>.findSerializerBySerialName(serialName: String): KSerializer<out V>? =
    this.getReadonlyMap()
        .entries
        .find { (_, serializer) -> serializer.descriptor.serialName == serialName }
        ?.value
