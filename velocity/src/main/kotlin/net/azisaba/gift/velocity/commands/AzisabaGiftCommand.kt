package net.azisaba.gift.velocity.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.CommandSource
import org.slf4j.Logger

class AzisabaGiftCommand(private val logger: Logger) : AbstractCommand() {
    override fun createBuilder(): LiteralArgumentBuilder<CommandSource> =
        literal("vazisabagift") // note that there is "v" here
            .requires { it.hasPermission("azisabagift.command.vazisabagift") }
}
