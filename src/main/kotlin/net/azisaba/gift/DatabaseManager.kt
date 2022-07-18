package net.azisaba.gift

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.coroutines.MinecraftDispatcher
import java.sql.PreparedStatement

object DatabaseManager {
    private val dataSource = PluginConfig.instance.databaseConfig.createDataSource()

    init {
        dataSource.connection.close() // test connection
    }

    suspend fun executeQuery(sql: String, vararg args: Any) = execute(sql, *args, type = QueryType.QUERY)

    suspend fun executeUpdate(sql: String, vararg args: Any) = execute(sql, *args, type = QueryType.UPDATE)

    private suspend fun <T> execute(sql: String, vararg args: Any, type: QueryType<T>): T =
        coroutineScope {
            withContext(MinecraftDispatcher.asyncDispatcher) {
                dataSource.connection.prepareStatement(sql).use { stmt ->
                    args.forEachIndexed { index, value ->
                        stmt.setObject(index + 1, value)
                    }
                    type.getResult(stmt)
                }
            }
        }
}

private class QueryType<T>(val getResult: (stmt: PreparedStatement) -> T) {
    companion object {
        val QUERY = QueryType { it.executeQuery()!! }
        val UPDATE = QueryType { it.executeUpdate() }
    }
}
