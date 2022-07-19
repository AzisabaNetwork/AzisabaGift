package net.azisaba.gift.providers

import kotlin.reflect.KClass

interface DataProvider

object DataProviders {
    private val providers = mutableMapOf<Class<out DataProvider>, MutableSet<DataProvider>>()

    fun <T : DataProvider> register(clazz: Class<T>, provider: T) =
        providers.computeIfAbsent(clazz) { mutableSetOf() }.add(provider)

    fun <T : DataProvider> register(clazz: KClass<T>, provider: T) =
        register(clazz.java, provider)

    @Suppress("UNCHECKED_CAST")
    fun <T : DataProvider> getAll(clazz: Class<T>): Set<T> =
        providers[clazz]?.map { it as T }?.toSet() ?: emptySet()

    fun <T : DataProvider> getByName(clazz: Class<T>, name: String): T? =
        getAll(clazz)
            .filter {
                it.javaClass.typeName.equals(name, true) ||
                it.javaClass.simpleName.equals(name, true) ||
                        it.javaClass.simpleName.replace("DataProvider", "").equals(name, true)
            }
            .let {
                if (it.size > 1) {
                    throw IllegalArgumentException("""
                        There are multiple matching providers with the name $name.
                        Please specify a provider using one of the following:
                        - ${getAll(clazz).joinToString("\n- ") { c -> c.javaClass.typeName }}
                    """.trimIndent())
                } else if (it.isEmpty()) {
                    null
                } else {
                    it.first()
                }
            }

    fun <T : DataProvider> getSelected(clazz: Class<T>, providers: List<String>): T =
        providers
            .map { getByName(clazz, it) }
            .firstOrNull { it != null }
            ?: throw IllegalArgumentException("""
                        No valid providers found in '$providers'.
                        Please specify provider using one of the following and modify the config.yml:
                        - ${getAll(clazz).joinToString("\n- ") { c -> c.javaClass.typeName }}
                    """.trimIndent())
}
