package net.azisaba.gift.providers

import kotlin.jvm.Throws
import kotlin.reflect.KClass

interface DataProvider {
    /**
     * Checks for the availability of the data provider. This method may throw an instance of [Exception]
     * ([Error] will not be handled) or return a reason of unavailability. If the string is null, the provider is
     * available.
     */
    @Throws(Exception::class)
    fun checkAvailability(): String?

    /**
     * Works like [checkAvailability] but does not throw an [Exception].
     */
    fun getUnavailableReason() =
        try {
            checkAvailability()
        } catch (e: Exception) {
            "${e.javaClass.simpleName}: ${e.message}"
        }

    /**
     * Checks if the data provider is available. Returns false if the provider is unavailable for any reason. Use
     * [getUnavailableReason] or [checkAvailability] to get the reason.
     */
    fun isAvailable() = getUnavailableReason() == null
}

object DataProviders {
    private val providers = mutableMapOf<Class<out DataProvider>, MutableSet<DataProvider>>()

    @JvmStatic
    fun <T : DataProvider> register(clazz: Class<T>, provider: T) =
        providers.computeIfAbsent(clazz) { mutableSetOf() }.add(provider)

    @JvmStatic
    fun <T : DataProvider> register(clazz: KClass<T>, provider: T) =
        register(clazz.java, provider)

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : DataProvider> getAll(clazz: Class<T>): Set<T> =
        providers[clazz]?.map { it as T }?.toSet() ?: emptySet()

    @JvmStatic
    fun <T : DataProvider> getByName(clazz: Class<T>, name: String): T? =
        getAll(clazz)
            .filter { it.isAvailable() }
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
                        - 
                    """.trimIndent() + getClassesAsFriendlyString(clazz))
                } else if (it.isEmpty()) {
                    null
                } else {
                    it.first()
                }
            }

    @JvmStatic
    fun <T : DataProvider> getSelected(clazz: Class<T>, providers: List<String>): T =
        providers
            .map { getByName(clazz, it) }
            .firstOrNull { it != null }
            ?: run {
                throw IllegalArgumentException("""
                    No valid providers found in '$providers'.
                    Please specify provider using one of the following and modify the config.yml:
                    - 
                """.trimIndent() + getClassesAsFriendlyString(clazz))
            }

    private fun <T : DataProvider> getClassesAsFriendlyString(clazz: Class<T>) =
        getAll(clazz).joinToString("\n- ") { c ->
            var unavailable = ""
            c.getUnavailableReason()?.let { reason -> unavailable = " (unavailable; reason: $reason)" }
            "${c.javaClass.typeName}$unavailable"
        }
}
