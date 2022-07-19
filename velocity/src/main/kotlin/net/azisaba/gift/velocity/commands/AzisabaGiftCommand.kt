package net.azisaba.gift.velocity.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.CommandSource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.JSON
import net.azisaba.gift.objects.CodesData
import net.azisaba.gift.objects.CodesTable
import net.azisaba.gift.objects.DebugMessage
import net.azisaba.gift.objects.Everyone
import net.azisaba.gift.objects.ExpirationStatus
import net.azisaba.gift.objects.HandlerList
import net.azisaba.gift.objects.Selector
import net.azisaba.gift.registry.Registry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger

class AzisabaGiftCommand(private val logger: Logger) : AbstractCommand() {
    @OptIn(ExperimentalSerializationApi::class)
    override fun createBuilder(): LiteralArgumentBuilder<CommandSource> =
        literal("vazisabagift") // note that there is "v" here
            .requires { it.hasPermission("azisabagift.command.vazisabagift") }
            .then(literal("modify")
                .then(argument("code", StringArgumentType.string())
                    .then(literal("handlers")
                        .then(literal("append")
                            .then(argument("handler_type", StringArgumentType.string())
                                .suggests(Registry.HANDLER.getValues().map { it.descriptor.serialName })
                                .then(argument("data", StringArgumentType.greedyString())
                                    .executesSuspend {
                                        Modify.Handlers.append(
                                            it.source,
                                            StringArgumentType.getString(it, "code"),
                                            StringArgumentType.getString(it, "handler_type"),
                                            StringArgumentType.getString(it, "data"),
                                        )
                                    }
                                )
                            )
                        )
                    )
                )
            )
            .then(literal("debug")
                .then(literal("generate")
                    .then(argument("code", StringArgumentType.greedyString())
                        .executesSuspend { debugGenerate(it.source, StringArgumentType.getString(it, "code")) }
                    )
                )
            )

    internal object Modify {
        internal object Handlers {
            @OptIn(ExperimentalSerializationApi::class)
            suspend fun append(source: CommandSource, code: String, handlerType: String, data: String = "{}"): Int {
                val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
                if (codes == null) {
                    source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                val serializer =
                    Registry.HANDLER
                        .getReadonlyMap()
                        .entries
                        .find { (_, serializer) -> serializer.descriptor.serialName == handlerType }
                        ?.value
                if (serializer == null) {
                    source.sendMessage(Component.text("Handlerが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                val handler = JSON.decodeFromString(serializer, data)
                val newHandlerList = codes.handler.copy(handlers = codes.handler.handlers + handler)
                DatabaseManager.executeUpdate("UPDATE `codes` SET `handler` = ? WHERE `code` = ?", JSON.encodeToString(newHandlerList), code).close()
                source.sendMessage(Component.text("${handler}を追加しました。", NamedTextColor.GREEN))
                return 0
            }
        }
    }

    private suspend fun debugGenerate(source: CommandSource, code: String): Int {
        DatabaseManager.executeUpdate("INSERT INTO `codes` (`code`, `selector`, `handler`, `data`) VALUES (?, ?, ?, ?)",
            code,
            JSON.encodeToString<Selector>(Everyone),
            JSON.encodeToString(HandlerList(listOf(DebugMessage("Hello, world!")))),
            JSON.encodeToString(CodesData(ExpirationStatus.NeverExpire))
        ).close()
        source.sendMessage(Component.text("Generated new code: $code", NamedTextColor.GREEN))
        return 0
    }
}
