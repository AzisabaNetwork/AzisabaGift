package net.azisaba.gift.velocity.listeners

import com.velocitypowered.api.event.EventTask
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import net.azisaba.gift.objects.CodesTable

object PromoCommandForwarder {
    @Subscribe
    fun onCommandExecute(e: CommandExecuteEvent) = EventTask.async {
        if (!e.command.startsWith("promo ")) {
            return@async
        }
        runBlocking {
            val code = e.command.removePrefix("promo ")
            try {
                val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull() ?: return@runBlocking
                if (false) {
                    e.result = CommandExecuteEvent.CommandResult.forwardToServer()
                }
            } catch (ignored: SerializationException) {
                // handler class is not available in velocity and could not be deserialized
                e.result = CommandExecuteEvent.CommandResult.forwardToServer()
            }
        }
    }!!
}
