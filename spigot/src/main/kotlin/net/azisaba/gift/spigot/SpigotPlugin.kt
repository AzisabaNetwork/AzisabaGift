package net.azisaba.gift.spigot

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.registry.Registry
import net.azisaba.gift.registry.registerK
import net.azisaba.gift.spigot.bridge.SpigotPlatform
import net.azisaba.gift.spigot.commands.AzisabaGiftCommand
import net.azisaba.gift.spigot.commands.PromoCommand
import net.azisaba.gift.spigot.handlers.GiveItems
import net.azisaba.gift.spigot.handlers.RunCommandOnServer
import net.azisaba.gift.spigot.providers.SpigotDataProvider
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

@Suppress("unused")
class SpigotPlugin : JavaPlugin() {
    companion object {
        lateinit var instance: SpigotPlugin
    }

    init {
        instance = this
    }

    override fun onLoad() {
        SpigotPlatform
        setupFakeDispatcher()
        setupProvider()
        PluginConfig.loadConfig(dataFolder.toPath(), logger::info)
        setupRegistry()
    }

    override fun onEnable() {
        runBlocking {
            DatabaseManager.init()
        }
        Bukkit.getPluginCommand("azisabagift")?.executor = AzisabaGiftCommand(logger)
        Bukkit.getPluginCommand("promo")?.executor = PromoCommand(logger)
        setupDispatcher()
    }

    private fun setupFakeDispatcher() {
        val count = AtomicInteger()
        MinecraftDispatcher.syncDispatcher =
            Executor { error("Cannot use syncDispatcher") }.asCoroutineDispatcher()
        MinecraftDispatcher.asyncDispatcher =
            Executor { Thread(it, "AzisabaGift-coroutines-#${count.getAndIncrement()}").start() }.asCoroutineDispatcher()
    }

    private fun setupDispatcher() {
        MinecraftDispatcher.syncDispatcher =
            Executor { Bukkit.getScheduler().runTask(this, it) }.asCoroutineDispatcher()
        MinecraftDispatcher.asyncDispatcher =
            Executor { Bukkit.getScheduler().runTaskAsynchronously(this, it) }.asCoroutineDispatcher()
    }

    private fun setupProvider() {
        SpigotDataProvider
    }

    private fun setupRegistry() {
        Registry.HANDLER.registerK(GiveItems::class, GiveItems.serializer())
        Registry.HANDLER.registerK(RunCommandOnServer::class, RunCommandOnServer.serializer())
    }
}
