package net.azisaba.gift.providers.types

import net.azisaba.gift.providers.DataProvider
import java.util.UUID

interface FirstJoinedTimeProvider : DataProvider {
    suspend fun getFirstJoinedTime(uuid: UUID): Long
}
