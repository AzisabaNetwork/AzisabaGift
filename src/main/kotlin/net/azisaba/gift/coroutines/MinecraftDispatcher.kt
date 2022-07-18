package net.azisaba.gift.coroutines

import kotlinx.coroutines.CoroutineDispatcher

object MinecraftDispatcher {
    lateinit var syncDispatcher: CoroutineDispatcher
    lateinit var asyncDispatcher: CoroutineDispatcher
}
