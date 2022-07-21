package net.azisaba.gift

import kotlinx.coroutines.withContext
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.objects.CodesTable
import net.azisaba.gift.objects.UsedCodesTable
import org.intellij.lang.annotations.Language
import java.sql.PreparedStatement
import java.sql.SQLException

object DatabaseManager {
    private val dataSource = PluginConfig.instance.databaseConfig.createDataSource()
    var initialized = false
        private set

    suspend fun init() {
        if (initialized) error("Already initialized")
        initialized = true
        executeUpdate(CodesTable.createTable).close()
        executeUpdate(UsedCodesTable.createTable).close()
    }

    suspend fun executeQuery(@Language("SQL") sql: String, vararg args: Any?) =
        execute(sql, *args, type = QueryType.QUERY)

    suspend fun executeUpdate(@Language("SQL") sql: String, vararg args: Any?) =
        execute(sql, *args, type = QueryType.UPDATE)

    private suspend fun <T> execute(
        @Language("SQL") sql: String,
        vararg args: Any?,
        type: QueryType<T>,
    ): CloseableResult<T> =
        withContext(MinecraftDispatcher.asyncDispatcher) {
            try {
                val connection = dataSource.connection
                val stmt = connection.prepareStatement(sql)
                args.forEachIndexed { index, value ->
                    stmt.setObject(index + 1, value)
                }
                CloseableResult.of(type.getResult(stmt)) {
                    stmt.close()
                    connection.close()
                }
            } catch (e: SQLException) {
                throw RuntimeException("Error executing query with values: ${args.toList()}: $sql", e)
            }
        }

    fun close() {
        dataSource.close()
    }
}

class CloseableResult<T> private constructor(val result: T, val closeFunction: () -> Unit) : AutoCloseable {
    companion object {
        fun <T> of(result: T, close: () -> Unit): CloseableResult<T> = CloseableResult(result, close)
    }

    override fun close() = closeFunction()

    operator fun component1() = result

    operator fun component2() = closeFunction
}

private class QueryType<T>(val getResult: (stmt: PreparedStatement) -> T) {
    companion object {
        val QUERY = QueryType { it.executeQuery()!! }
        val UPDATE = QueryType { it.executeUpdate() }
    }
}
