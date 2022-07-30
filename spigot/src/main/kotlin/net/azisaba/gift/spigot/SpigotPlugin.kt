package net.azisaba.gift.spigot

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.JSONWithoutRegistry
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.registry.Registry
import net.azisaba.gift.registry.registerK
import net.azisaba.gift.spigot.bridge.SpigotPlatform
import net.azisaba.gift.spigot.commands.AzisabaGiftCommand
import net.azisaba.gift.spigot.commands.PromoCommand
import net.azisaba.gift.config.SpigotPlatformConfig
import net.azisaba.gift.spigot.handlers.GiveItems
import net.azisaba.gift.spigot.handlers.GivePlayerPoints
import net.azisaba.gift.spigot.handlers.RunCommandOnServer
import net.azisaba.gift.spigot.listeners.ItemOwnerListener
import net.azisaba.gift.spigot.providers.SpigotDataProvider
import net.azisaba.gift.spigot.selectors.HasPermissionOnServer
import net.azisaba.gift.spigot.selectors.NearLocationSelector
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
        PluginConfig.loadConfig(dataFolder.toPath(), PluginConfig(platformConfig = SpigotPlatformConfig()), logger::info)
        setupRegistry()
    }

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(ItemOwnerListener, this)
        runBlocking {
            DatabaseManager.init()
        }
        Bukkit.getScheduler().runTaskLater(this, {
            Bukkit.getPluginCommand("azisabagift")?.executor = AzisabaGiftCommand()
            Bukkit.getPluginCommand("promo")?.executor = PromoCommand(logger)
        }, 1)
        setupDispatcher()
    }

    override fun onDisable() {
        DatabaseManager.close()
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
        Registry.HANDLER_DEFAULT_VALUE.registerK(
            RunCommandOnServer::class,
            JSONWithoutRegistry.encodeToString(RunCommandOnServer("tell %player_name% Hello %player_uuid%!", true)),
        )
        Registry.HANDLER.registerK(GivePlayerPoints::class, GivePlayerPoints.serializer())
        Registry.HANDLER_DEFAULT_VALUE.registerK(
            GivePlayerPoints::class,
            JSONWithoutRegistry.encodeToString(GivePlayerPoints(10)),
        )
        Registry.SELECTOR.registerK(HasPermissionOnServer::class, HasPermissionOnServer.serializer())
        Registry.SELECTOR_DEFAULT_VALUE.registerK(
            HasPermissionOnServer::class,
            JSONWithoutRegistry.encodeToString(HasPermissionOnServer("azisabagift.*")),
        )
        Registry.SELECTOR.registerK(NearLocationSelector::class, NearLocationSelector.serializer())
        Registry.SELECTOR_DEFAULT_VALUE.registerK(
            NearLocationSelector::class,
            JSONWithoutRegistry.encodeToString(NearLocationSelector("world", 0.0, 0.0, 0.0, 5.0)),
        )
    }
}
