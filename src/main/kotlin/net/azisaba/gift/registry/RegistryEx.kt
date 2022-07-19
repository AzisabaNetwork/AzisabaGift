package net.azisaba.gift.registry

import kotlin.reflect.KClass

fun <K : Any, V : Any> Registry<Class<out K>, V>.registerK(clazz: KClass<out K>, value: V) {
    register(clazz.java, value)
}
