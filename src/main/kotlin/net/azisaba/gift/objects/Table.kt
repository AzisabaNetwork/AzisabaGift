package net.azisaba.gift.objects

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializerOrNull
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.JSON
import net.azisaba.gift.coroutines.letSuspend
import org.intellij.lang.annotations.Language
import java.lang.reflect.Parameter
import java.sql.ResultSet
import java.util.UUID
import kotlin.reflect.KClass

abstract class Table<T : Any>(clazz: KClass<T>) {
    private val clazz = clazz.java

    abstract val toValues: suspend (rs: ResultSet) -> Collection<T>

    suspend fun find(@Language("SQL") sql: String, vararg args: Any): Collection<T> =
        DatabaseManager
            .executeQuery(sql, *args)
            .letSuspend { result ->
                result.use { (rs) ->
                    toValues(rs)
                }
            }

    suspend fun findSingle(@Language("SQL") sql: String, vararg args: Any): T? = find(sql, *args).let {
        if (it.isEmpty()) null else it.first()
    }

    /**
     * Select rows from the table. [sql] is a SQL query. [args] is a list of arguments. Order of columns in [sql] must
     * match the order of arguments of constructor of [T].
     */
    suspend fun select(@Language("SQL") sql: String, vararg args: Any): Collection<T> =
        DatabaseManager
            .executeQuery(sql, *args)
            .letSuspend { result ->
                result.use { (rs) ->
                    val values = mutableListOf<T>()
                    val ctr = clazz.constructors.last()
                    while (rs.next()) {
                        val ctrArgs = mutableListOf<Any>()
                        ctr.parameters.forEachIndexed { index, param ->
                            try {
                                ctrArgs.add(param.extractValue(index + 1, rs))
                            } catch (e: Exception) {
                                // TODO: use proper logger
                                System.err.println("Failed to extract value from column ${index + 1}")
                                e.printStackTrace()
                                throw e
                            }
                        }
                        ctr.newInstance(*ctrArgs.toTypedArray()).apply {
                            @Suppress("UNCHECKED_CAST")
                            values.add(this as T)
                        }
                    }
                    values
                }
            }

    suspend fun insert(tableName: String, value: T) {
        val sqlValues = value.javaClass.constructors[0].parameters.joinToString(", ") { "?" }
        val sqlActualValues =
            value
                .javaClass
                .constructors
                .last()
                .parameters
                .map {
                    val fieldValue = value.javaClass.fields.find { f -> f.name == it.name }!!.get(value)
                    if (fieldValue == null) {
                        null
                    } else {
                        it.toValue(fieldValue)
                    }
                }

        DatabaseManager
            .executeUpdate(
                "INSERT INTO `$tableName` VALUES ($sqlValues)",
                *sqlActualValues.toTypedArray(),
            )
    }

    @OptIn(InternalSerializationApi::class)
    private fun Parameter.extractValue(i: Int, rs: ResultSet): Any =
        when (type) {
            Int::class.java -> rs.getInt(i)
            Float::class.java -> rs.getFloat(i)
            Double::class.java -> rs.getDouble(i)
            Long::class.java -> rs.getLong(i)
            Byte::class.java -> rs.getByte(i)
            Short::class.java -> rs.getShort(i)
            Boolean::class.java -> rs.getBoolean(i)
            String::class.java -> rs.getString(i)
            UUID::class.java -> UUID.fromString(rs.getString(i))
            else -> {
                type.kotlin.serializerOrNull().let {
                    if (it == null) {
                        JSON.decodeFromString(rs.getString(i))
                    } else {
                        JSON.decodeFromString(it, rs.getString(i))
                    }
                }
            }
        }

    private inline fun <reified T : Any> Parameter.toValue(value: T): Any =
        when (type) {
            Int::class.java -> value as Int
            Float::class.java -> value as Float
            Double::class.java -> value as Double
            Long::class.java -> value as Long
            Byte::class.java -> value as Byte
            Short::class.java -> value as Short
            Boolean::class.java -> value as Boolean
            String::class.java -> value as String
            else -> JSON.encodeToString(value)
        }
}
