package net.azisaba.gift.velocity.commands

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

abstract class AbstractCommand {
    companion object {
        fun literal(name: String): LiteralArgumentBuilder<CommandSource> = LiteralArgumentBuilder.literal(name)

        fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<CommandSource, T> = RequiredArgumentBuilder.argument(name, type)!!
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

fun <S, T> RequiredArgumentBuilder<S, T>.suggests(suggestions: List<String>) =
    suggests { _, builder -> builder.suggest(suggestions) }!!

fun SuggestionsBuilder.suggest(suggestions: List<String>): CompletableFuture<Suggestions> {
    val input = remaining.lowercase()
    suggestions.filter { matchesSubStr(input, it.lowercase()) }.forEach(::suggest)
    return buildFuture()
}

private fun matchesSubStr(input: String, suggestion: String): Boolean {
    var i = 0
    while (!suggestion.startsWith(input, i)) {
        i = suggestion.indexOf('_', i)
        if (i < 0) {
            return false
        }
        i++
    }
    return true
}
