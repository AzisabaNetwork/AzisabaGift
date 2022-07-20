package net.azisaba.gift.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@SerialName("handler_list")
@Serializable
data class HandlerList(
    val handlers: List<Handler> = emptyList(),
) {
    /**
     * Attempts to invoke all handlers.
     * @throws IllegalStateException if any handler throws an exception or returns false
     */
    suspend fun handle(uuid: UUID, runIf: (Handler) -> Boolean) {
        handlers.forEachIndexed { index, handler ->
            if (!runIf(handler)) {
                return@forEachIndexed
            }
            if (!handler.handle(uuid)) {
                error("Handler $handler (index: $index) returned false")
            }
        }
    }
}
