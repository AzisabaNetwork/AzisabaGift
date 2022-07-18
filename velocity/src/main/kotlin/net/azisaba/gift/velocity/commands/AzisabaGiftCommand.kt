package net.azisaba.gift.velocity.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.CommandSource
import kotlinx.serialization.encodeToString
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.JSON
import net.azisaba.gift.objects.CodesData
import net.azisaba.gift.objects.DebugMessage
import net.azisaba.gift.objects.Everyone
import net.azisaba.gift.objects.ExpirationStatus
import net.azisaba.gift.objects.Handler
import net.azisaba.gift.objects.Selector
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger

class AzisabaGiftCommand(private val logger: Logger) : AbstractCommand() {
    override fun createBuilder(): LiteralArgumentBuilder<CommandSource> =
        literal("vazisabagift") // note that there is "v" here
            .requires { it.hasPermission("azisabagift.command.vazisabagift") }
            .then(literal("debug")
                .then(literal("generate")
                    .then(argument("code", StringArgumentType.greedyString())
                        .executesSuspend { debugGenerate(it.source, StringArgumentType.getString(it, "code")) }
                    )
                )
            )

    private suspend fun debugGenerate(source: CommandSource, code: String): Int {
        DatabaseManager.executeUpdate("INSERT INTO `codes` (`code`, `selector`, `handler`, `data`) VALUES (?, ?, ?, ?)",
            code,
            JSON.encodeToString<Selector>(Everyone),
            JSON.encodeToString<Handler>(DebugMessage("Hello, world!")),
            JSON.encodeToString(CodesData(ExpirationStatus.NeverExpire))
        ).close()
        source.sendMessage(Component.text("Generated new code: $code", NamedTextColor.GREEN))
        return 0
    }
}
