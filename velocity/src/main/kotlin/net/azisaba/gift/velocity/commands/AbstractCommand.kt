package net.azisaba.gift.velocity.commands

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.runBlocking
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

abstract class AbstractCommand {
    companion object {
        fun literal(name: String) = LiteralArgumentBuilder.literal<CommandSource>(name)!!

        fun <T> argument(name: String, type: ArgumentType<T>) = RequiredArgumentBuilder.argument<CommandSource, T>(name, type)!!
    }

    fun createCommand() = BrigadierCommand(createBuilder())

    protected abstract fun createBuilder(): LiteralArgumentBuilder<CommandSource>
}

fun <S, T : ArgumentBuilder<S, T>> T.executesSuspend(block: suspend (CommandContext<S>) -> Int): T =
    executes {
        runBlocking {
            block(it)
        }
    }
