package net.azisaba.gift.registry

import java.util.concurrent.ConcurrentHashMap

class MutableRegistry<K : Any, V : Any> : Registry<K, V>() {
    private val map: MutableMap<K, V> = ConcurrentHashMap()

    override fun isRegisteredKey(key: K): Boolean = map.containsKey(key)

    override fun isRegisteredValue(value: V): Boolean = map.containsValue(value)

    override fun getValues(): Collection<V> = map.values

    override fun getKeys(): Set<K> = map.keys

    override fun register(key: K, value: V) {
        map[key] = value
    }

    override fun get(key: K): V? = map[key]

    // use raw access to map
    override fun getByValue(value: V): Collection<K> =
        map.entries
            .filter { (_, v) -> v == value }
            .map { (k) -> k }

    override fun getReadonlyMap() = map.toMap()
}
