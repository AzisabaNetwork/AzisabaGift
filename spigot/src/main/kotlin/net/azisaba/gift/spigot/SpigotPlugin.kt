package net.azisaba.gift.spigot

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.spigot.bridge.SpigotPlatform
import net.azisaba.gift.spigot.providers.SpigotDataProvider
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.Executor

@Suppress("unused")
class SpigotPlugin : JavaPlugin() {
    override fun onLoad() {
        SpigotPlatform
        setupDispatcher()
        setupProvider()
        PluginConfig.loadConfig(dataFolder.toPath(), logger::info)
    }

    override fun onEnable() {
        runBlocking {
            DatabaseManager.init()
        }
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
}
