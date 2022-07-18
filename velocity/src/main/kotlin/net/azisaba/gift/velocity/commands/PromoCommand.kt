package net.azisaba.gift.velocity.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import org.slf4j.Logger

class PromoCommand(private val logger: Logger) : AbstractCommand() {
    override fun createBuilder(): LiteralArgumentBuilder<CommandSource> =
        literal("gift")
            .requires { it.hasPermission("azisabagift.command.promo") }
            .then(argument("code", StringArgumentType.greedyString())
                .requires { it is Player }
                .executesSuspend { executeWithCode(it.source as Player, StringArgumentType.getString(it, "code")) }
            )

    private suspend fun executeWithCode(player: Player, code: String): Int {
        TODO()
        //return 0
    }
}
