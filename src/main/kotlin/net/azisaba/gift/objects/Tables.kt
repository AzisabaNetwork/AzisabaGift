package net.azisaba.gift.objects

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.azisaba.gift.JSON
import java.sql.ResultSet

object CodesTable : Table<Codes>(Codes::class) {
    override val toValues: suspend (rs: ResultSet) -> Collection<Codes> = { rs ->
        val values = mutableListOf<Codes>()
        while (rs.next()) {
            values.add(Codes(
                rs.getLong("id"),
                rs.getString("code"),
                JSON.decodeFromString(rs.getString("selector")),
            ))
        }
        values
    }
}

@Serializable
data class Codes(val id: Long, val code: String, val selector: Selector)
