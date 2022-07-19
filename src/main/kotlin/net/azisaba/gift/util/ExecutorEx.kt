package net.azisaba.gift.util

import java.util.concurrent.Executor
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun Executor.executeAsync(block: suspend () -> Unit) {
    this.execute {
        block.startCoroutine(Continuation(EmptyCoroutineContext) {})
    }
}
