package net.azisaba.gift.velocity

import com.google.inject.Inject
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import java.nio.file.Path

@Plugin(id = "azisaba-gift", name = "AzisabaGift", version = "dev", authors = ["Azisaba Network"],
    url = "https://github.com/AzisabaNetwork/AzisabaGift", description = "/promo plugin")
class VelocityPlugin @Inject constructor(server: ProxyServer, logger: Logger, @DataDirectory val dataDirectory: Path) {

}
