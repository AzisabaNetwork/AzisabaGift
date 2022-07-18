package net.azisaba.gift.velocity

import com.google.inject.Inject
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.asCoroutineDispatcher
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.config.PluginConfig
import net.azisaba.gift.coroutines.MinecraftDispatcher
import net.azisaba.gift.velocity.commands.AzisabaGiftCommand
import net.azisaba.gift.velocity.commands.PromoCommand
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.Executor

@Plugin(id = "azisaba-gift", name = "AzisabaGift", version = "dev", authors = ["Azisaba Network"],
    url = "https://github.com/AzisabaNetwork/AzisabaGift", description = "/promo plugin")
class VelocityPlugin @Inject constructor(server: ProxyServer, logger: Logger, @DataDirectory val dataDirectory: Path) {
    init {
        setupDispatcher(server)
        PluginConfig.loadConfig(dataDirectory, logger::info)
        DatabaseManager
        server.commandManager.register(AzisabaGiftCommand(logger).createCommand())
        server.commandManager.register(PromoCommand(logger).createCommand())
    }

    private fun setupDispatcher(server: ProxyServer) {
        MinecraftDispatcher.syncDispatcher =
            Executor { server.scheduler.buildTask(this, it).schedule() }.asCoroutineDispatcher()
        MinecraftDispatcher.asyncDispatcher = MinecraftDispatcher.syncDispatcher
    }
}
