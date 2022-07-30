package net.azisaba.gift.velocity.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.CommandSource
import kotlinx.serialization.ExperimentalSerializationApi
import net.azisaba.gift.registry.Registry

object AzisabaGiftCommand : AbstractCommand() {
    @OptIn(ExperimentalSerializationApi::class)
    override fun createBuilder(): LiteralArgumentBuilder<CommandSource> =
        literal("vazisabagift") // note that there is "v" here
            .requires { it.hasPermission("azisabagift.command.vazisabagift") }
            .then(literal("createcode")
                .requires { it.hasPermission("azisabagift.createcode") }
                .then(argument("code", StringArgumentType.string())
                    .executesSuspend {
                        AzisabaGiftCommandImpl.createCode(it.source, StringArgumentType.getString(it, "code"))
                    }
                )
            )
            .then(literal("code")
                .requires { it.hasPermission("azisabagift.code") }
                .then(argument("code", StringArgumentType.string())
                    .then(literal("info")
                        .requires { it.hasPermission("azisabagift.code.info") }
                        .executesSuspend {
                            AzisabaGiftCommandImpl.Code.info(
                                it.source,
                                StringArgumentType.getString(it, "code"),
                            )
                        }
                    )
                    .then(literal("clearusage")
                        .requires { it.hasPermission("azisabagift.code.clearusage") }
                        .executesSuspend {
                            AzisabaGiftCommandImpl.Code.clearUsage(
                                it.source,
                                StringArgumentType.getString(it, "code"),
                            )
                        }
                    )
                    .then(literal("set")
                        .requires { it.hasPermission("azisabagift.code.set") }
                        .then(literal("selector")
                            .requires { it.hasPermission("azisabagift.code.set.selector") }
                            .then(argument("type", StringArgumentType.string())
                                .requiresWithContext { ctx, _ ->
                                    ctx.source.hasPermission("azisabagift.types.selectors.${ctx.arguments["type"]?.result}")
                                }
                                .suggests { _, builder -> builder.suggest(Registry.SELECTOR.getValues().map { it.descriptor.serialName }) }
                                .executesSuspend {
                                    AzisabaGiftCommandImpl.Code.Set.setSelector(
                                        it.source,
                                        StringArgumentType.getString(it, "code"),
                                        StringArgumentType.getString(it, "type"),
                                    )
                                }
                                .then(argument("data", StringArgumentType.greedyString())
                                    .executesSuspend {
                                        AzisabaGiftCommandImpl.Code.Set.setSelector(
                                            it.source,
                                            StringArgumentType.getString(it, "code"),
                                            StringArgumentType.getString(it, "type"),
                                            StringArgumentType.getString(it, "data"),
                                        )
                                    }
                                )
                            )
                        )
                        .then(literal("expirationstatus")
                            .requires { it.hasPermission("azisabagift.code.set.expirationstatus") }
                            .then(argument("type", StringArgumentType.string())
                                .suggests { _, builder -> builder.suggest(Registry.EXPIRATION_STATUS.getValues().map { it.descriptor.serialName }) }
                                .executesSuspend {
                                    AzisabaGiftCommandImpl.Code.Set.setExpirationStatus(
                                        it.source,
                                        StringArgumentType.getString(it, "code"),
                                        StringArgumentType.getString(it, "type"),
                                    )
                                }
                                .then(argument("data", StringArgumentType.greedyString())
                                    .executesSuspend {
                                        AzisabaGiftCommandImpl.Code.Set.setExpirationStatus(
                                            it.source,
                                            StringArgumentType.getString(it, "code"),
                                            StringArgumentType.getString(it, "type"),
                                            StringArgumentType.getString(it, "data"),
                                        )
                                    }
                                )
                            )
                        )
                        .then(literal("allowedserver")
                            .requires { it.hasPermission("azisabagift.code.set.allowedserver") }
                            .then(argument("pattern", StringArgumentType.greedyString())
                                .suggests { _, builder -> builder.suggest(listOf(".*")) }
                                .executesSuspend {
                                    AzisabaGiftCommandImpl.Code.Set.setAllowedServer(
                                        it.source,
                                        StringArgumentType.getString(it, "code"),
                                        StringArgumentType.getString(it, "pattern"),
                                    )
                                }
                            )
                        )
                    )
                    .then(literal("handlers")
                        .requires { it.hasPermission("azisabagift.code.handlers") }
                        .then(literal("info")
                            .requires { it.hasPermission("azisabagift.code.handlers.info") }
                            .executesSuspend {
                                AzisabaGiftCommandImpl.Code.Handlers.info(
                                    it.source,
                                    StringArgumentType.getString(it, "code"),
                                )
                            }
                        )
                        .then(literal("clear")
                            .requires { it.hasPermission("azisabagift.code.handlers.clear") }
                            .executesSuspend {
                                AzisabaGiftCommandImpl.Code.Handlers.clear(
                                    it.source,
                                    StringArgumentType.getString(it, "code"),
                                )
                            }
                        )
                        .then(literal("remove")
                            .requires { it.hasPermission("azisabagift.code.handlers.remove") }
                            .then(argument("position-from-1", IntegerArgumentType.integer(1))
                                .executesSuspend {
                                    AzisabaGiftCommandImpl.Code.Handlers.remove(
                                        it.source,
                                        StringArgumentType.getString(it, "code"),
                                        IntegerArgumentType.getInteger(it, "position-from-1")
                                    )
                                }
                            )
                        )
                        .then(literal("insert")
                            .requires { it.hasPermission("azisabagift.code.handlers.insert") }
                            .then(argument("position-from-1", IntegerArgumentType.integer(1))
                                .then(argument("handler_type", StringArgumentType.string())
                                    .requiresWithContext { ctx, _ ->
                                        ctx.source.hasPermission("azisabagift.types.handlers.${ctx.arguments["handler_type"]?.result}")
                                    }
                                    .suggests(Registry.HANDLER.getValues().map { it.descriptor.serialName })
                                    .executesSuspend {
                                        AzisabaGiftCommandImpl.Code.Handlers.insert(
                                            it.source,
                                            StringArgumentType.getString(it, "code"),
                                            IntegerArgumentType.getInteger(it, "position-from-1"),
                                            StringArgumentType.getString(it, "handler_type"),
                                        )
                                    }
                                    .then(argument("data", StringArgumentType.greedyString())
                                        .executesSuspend {
                                            AzisabaGiftCommandImpl.Code.Handlers.insert(
                                                it.source,
                                                StringArgumentType.getString(it, "code"),
                                                IntegerArgumentType.getInteger(it, "position-from-1"),
                                                StringArgumentType.getString(it, "handler_type"),
                                                StringArgumentType.getString(it, "data"),
                                            )
                                        }
                                    )
                                )
                            )
                        )
                        .then(literal("append")
                            .requires { it.hasPermission("azisabagift.code.handlers.append") }
                            .then(argument("handler_type", StringArgumentType.string())
                                .requiresWithContext { ctx, _ ->
                                    ctx.source.hasPermission("azisabagift.types.handlers.${ctx.arguments["handler_type"]?.result}")
                                }
                                .suggests(Registry.HANDLER.getValues().map { it.descriptor.serialName })
                                .executesSuspend {
                                    AzisabaGiftCommandImpl.Code.Handlers.append(
                                        it.source,
                                        StringArgumentType.getString(it, "code"),
                                        StringArgumentType.getString(it, "handler_type"),
                                    )
                                }
                                .then(argument("data", StringArgumentType.greedyString())
                                    .executesSuspend {
                                        AzisabaGiftCommandImpl.Code.Handlers.append(
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
                        .executesSuspend {
                            AzisabaGiftCommandImpl.debugGenerate(
                                it.source,
                                StringArgumentType.getString(it, "code"),
                            )
                        }
                    )
                )
            )
}
