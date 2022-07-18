package net.azisaba.gift.spigot

import kotlinx.coroutines.asCoroutineDispatcher
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.coroutines.MinecraftDispatcher
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.Executor

class SpigotPlugin : JavaPlugin() {
    init {
        MinecraftDispatcher.syncDispatcher =
            Executor { Bukkit.getScheduler().runTask(this, it) }.asCoroutineDispatcher()
        MinecraftDispatcher.asyncDispatcher =
            Executor { Bukkit.getScheduler().runTaskAsynchronously(this, it) }.asCoroutineDispatcher()
    }

    override fun onLoad() {
        PluginConfig.loadConfig(dataFolder.toPath(), logger::info)
        DatabaseManager
    }
}
