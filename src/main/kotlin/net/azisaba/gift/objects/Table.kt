package net.azisaba.gift.objects

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializerOrNull
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.JSON
import net.azisaba.gift.coroutines.letSuspend
import org.intellij.lang.annotations.Language
import java.lang.reflect.Parameter
import java.sql.ResultSet
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
                    val ctr = clazz.constructors.first()
                    while (rs.next()) {
                        val ctrArgs = mutableListOf<Any>()
                        ctr.parameters.forEachIndexed { index, param ->
                            ctrArgs.add(param.extractValue(index + 1, rs))
                        }
                        ctr.newInstance(*ctrArgs.toTypedArray()).apply {
                            @Suppress("UNCHECKED_CAST")
                            values.add(this as T)
                        }
                    }
                    values
                }
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
            else -> {
                val annotation = type.getAnnotation(Serializable::class.java)
                val serializer = annotation?.with?.serializerOrNull()// ?: type.kotlin.serializerOrNull()
                if (serializer == null) {
                    JSON.decodeFromString(rs.getString(1))
                } else {
                    //JSON.decodeFromString(serializer, rs.getString(i))
                    JSON.decodeFromString(rs.getString(i))
                }
                //throw IllegalArgumentException("Unsupported type: $type")
            }
        }
}
