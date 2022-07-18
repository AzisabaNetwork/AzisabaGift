package net.azisaba.gift.bridge

import java.util.UUID

interface Platform {
    companion object : Platform {
        lateinit var instance: Platform

        override fun getPlayer(uuid: UUID): Player? {
            return instance.getPlayer(uuid)
        }
    }

    fun getPlayer(uuid: UUID): Player?
}
