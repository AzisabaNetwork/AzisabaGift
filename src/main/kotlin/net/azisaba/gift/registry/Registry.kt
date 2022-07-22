package net.azisaba.gift.registry

import kotlinx.serialization.KSerializer
import net.azisaba.gift.objects.ExpirationStatus
import net.azisaba.gift.objects.Handler
import net.azisaba.gift.objects.Selector

abstract class Registry<K : Any, V : Any> {
    companion object {
        // These should be registered during constructor; there is no guarantee that these settings will apply after
        // plugin initialization (like `onEnable` or similar).
        @JvmStatic
        val HANDLER: Registry<Class<out Handler>, KSerializer<out Handler>> = MutableRegistry()
        @JvmStatic
        val HANDLER_DEFAULT_VALUE: Registry<Class<out Handler>, String> = MutableRegistry()
        @JvmStatic
        val SELECTOR: Registry<Class<out Selector>, KSerializer<out Selector>> = MutableRegistry()
        @JvmStatic
        val SELECTOR_DEFAULT_VALUE: Registry<Class<out Selector>, String> = MutableRegistry()
        @JvmStatic
        val EXPIRATION_STATUS: Registry<Class<out ExpirationStatus>, KSerializer<out ExpirationStatus>> = MutableRegistry()
        @JvmStatic
        val EXPIRATION_STATUS_DEFAULT_VALUE: Registry<Class<out ExpirationStatus>, String> = MutableRegistry()

        init {
            Selector
            Handler
            ExpirationStatus
        }
    }

    open fun isRegisteredKey(key: K): Boolean = getReadonlyMap().containsKey(key)

    open fun isRegisteredValue(value: V): Boolean = getReadonlyMap().containsValue(value)

    open fun getValues(): Collection<V> = getReadonlyMap().values

    open fun getKeys(): Set<K> = getReadonlyMap().keys

    open fun get(key: K): V? = getReadonlyMap()[key]

    open fun getByValue(value: V): Collection<K> =
        getReadonlyMap()
            .entries
            .filter { (_, v) -> v == value }
            .map { (k) -> k }

    abstract fun register(key: K, value: V)

    abstract fun getReadonlyMap(): Map<K, V>
}
