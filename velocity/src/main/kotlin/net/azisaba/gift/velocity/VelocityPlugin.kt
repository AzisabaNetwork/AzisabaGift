package net.azisaba.gift.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.registry.Registry
import net.azisaba.gift.velocity.bridge.VelocityPlatform
import net.azisaba.gift.velocity.commands.AzisabaGiftCommand
import net.azisaba.gift.velocity.commands.PromoCommand
import net.azisaba.gift.velocity.providers.SpicyAzisaBanDataProvider
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.Executor

@Plugin(id = "azisaba-gift", name = "AzisabaGift", version = "dev", authors = ["Azisaba Network"],
    url = "https://github.com/AzisabaNetwork/AzisabaGift", description = "/promo plugin")
class VelocityPlugin @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @DataDirectory
    val dataDirectory: Path,
) {
    init {
        VelocityPlatform(server)
        setupDispatcher(server)
        setupProvider()
        PluginConfig.loadConfig(dataDirectory, logInfo = logger::info)
        Registry // load registry
    }

    private fun setupDispatcher(server: ProxyServer) {
        MinecraftDispatcher.syncDispatcher =
            Executor { server.scheduler.buildTask(this, it).schedule() }.asCoroutineDispatcher()
        MinecraftDispatcher.asyncDispatcher = MinecraftDispatcher.syncDispatcher
    }

    private fun setupProvider() {
        SpicyAzisaBanDataProvider
    }

    @Subscribe
    fun onProxyInitialization(e: ProxyInitializeEvent) {
        runBlocking {
            DatabaseManager.init()
        }
        server.commandManager.register(AzisabaGiftCommand.createCommand())
        server.commandManager.register(PromoCommand(logger).createCommand())
    }
}
