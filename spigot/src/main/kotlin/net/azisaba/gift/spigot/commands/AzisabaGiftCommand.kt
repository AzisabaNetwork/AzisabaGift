package net.azisaba.gift.spigot.commands

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.JSON
import net.azisaba.gift.objects.Codes
import net.azisaba.gift.objects.CodesData
import net.azisaba.gift.objects.CodesTable
import net.azisaba.gift.objects.HandlerList
import net.azisaba.gift.objects.Nobody
import net.azisaba.gift.registry.Registry
import net.azisaba.gift.registry.findSerializerBySerialName
import net.azisaba.gift.spigot.SpigotPlugin
import net.azisaba.gift.spigot.toFriendlyOutput
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

@OptIn(ExperimentalSerializationApi::class)
private val commandMap by lazy {
    mapOf<String, Any>(
        "createcode" to mapOf("<code>" to Unit),
        "code" to mapOf(
            ":__any__:<code>" to mapOf(
                "info" to Unit,
                "clearusage" to Unit,
                "set" to mapOf(
                    "selector" to Registry.SELECTOR
                        .getReadonlyMap()
                        .entries
                        .associateWith { (clazz, _) -> mapOf((Registry.SELECTOR_DEFAULT_VALUE.get(clazz) ?: "{}") to Unit) }
                        .mapKeys { (entry, _) -> entry.value.descriptor.serialName },
                    "expirationstatus" to Registry.EXPIRATION_STATUS
                        .getReadonlyMap()
                        .entries
                        .associateWith { (clazz, _) -> mapOf((Registry.EXPIRATION_STATUS_DEFAULT_VALUE.get(clazz) ?: "{}") to Unit) }
                        .mapKeys { (entry, _) -> entry.value.descriptor.serialName },
                    "allowedserver" to mapOf(".*" to Unit),
                ),
                "handlers" to mapOf(
                    "clear" to Unit,
                    "info" to Unit,
                    "remove" to mapOf("<position-from-1>" to Unit),
                    "insert" to mapOf(
                        ":__any__:<position-from-1>" to Registry.HANDLER
                            .getReadonlyMap()
                            .entries
                            .associateWith { (clazz, _) -> mapOf((Registry.HANDLER_DEFAULT_VALUE.get(clazz) ?: "{}") to Unit) }
                            .mapKeys { (entry, _) -> entry.value.descriptor.serialName },
                    ),
                    "append" to Registry.HANDLER
                        .getReadonlyMap()
                        .entries
                        .associateWith { (clazz, _) -> mapOf((Registry.HANDLER_DEFAULT_VALUE.get(clazz) ?: "{}") to Unit) }
                        .mapKeys { (entry, _) -> entry.value.descriptor.serialName },
                    "itemhandler" to mapOf(
                        ":__any__:<position-from-1>" to mapOf(
                            "add" to Unit,
                            "remove" to Unit,
                            "info" to mapOf("[<position-from-1>]" to Unit),
                            "getitem" to mapOf("<position-from-1>" to Unit),
                        ),
                    ),
                ),
            ),
        ),
    )
}

private fun Map<String, Any>.getSuggestionsFor(args: Array<String>): List<String> =
    getSuggestionsForRecursive(args).filter(args.last())

private fun Map<String, Any>.getSuggestionsForRecursive(args: Array<String>, offset: Int = 0): Collection<String> {
    if (args.isEmpty()) {
        return this.keys
    }
    if (args.size == offset + 1) {
        return this.keys.map { it.removePrefix(":__any__:") }.filter { it.isNotBlank() }
    }
    val key = args[offset]
    return when (val value = this[key] ?: this.entries.find { (k) -> k.startsWith(":__any__:") }?.value) {
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            (value as Map<String, Any>).getSuggestionsForRecursive(args, offset + 1)
        }
        is Collection<*> -> value.map { it.toString() }
        else -> emptyList()
    }
}

private fun Collection<String>.filter(s: String) = filter { matchesSubStr(s, it) }

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

@Suppress("UNCHECKED_CAST")
private fun <R> Map<String, Any>.getAs(key: String) = this[key] as R

class AzisabaGiftCommand : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>,
    ): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("${ChatColor.RED}/azisabagift (${commandMap.keys.joinToString("|")})")
            return true
        }
        Bukkit.getScheduler().runTaskAsynchronously(SpigotPlugin.instance) {
            runBlocking {
                try {
                    when (args[0]) {
                        "createcode" -> {
                            if (sender.requires("azisabagift.createcode")) return@runBlocking
                            if (args.size < 2) {
                                sender.sendMessage("${ChatColor.RED}/azisabagift createcode <code>")
                                return@runBlocking
                            }
                            Impl.createCode(sender, args[1])
                        }
                        "code" -> {
                            if (sender.requires("azisabagift.code")) return@runBlocking
                            if (args.size < 3) {
                                val choices = commandMap
                                    .getAs<Map<String, Any>>("code")
                                    .getAs<Map<String, Any>>(":__any__:<code>")
                                    .keys
                                    .joinToString("|")
                                sender.sendMessage("${ChatColor.RED}/azisabagift code <code> ($choices)")
                                return@runBlocking
                            }
                            Impl.Code.execute(sender, args[1], args[2], args.drop(3))
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    sender.sendMessage("${ChatColor.RED}${e.javaClass.simpleName}: ${e.message}")
                    throw e
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>,
    ): List<String> {
        return commandMap.getSuggestionsFor(args)
    }
}

internal object Impl {
    suspend fun createCode(
        sender: CommandSender,
        code: String,
    ) {
        val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
        if (codes != null) {
            sender.sendMessage("${ChatColor.RED}指定されたコードはすでに存在します。")
            return
        }
        val newCodes = Codes(0L, code, Nobody, HandlerList(), CodesData())
        CodesTable.insertB("codes", newCodes) { put("id", null); put("0", null) } // for consistency between different configurations
        sender.sendMessage("${ChatColor.GREEN}コードを作成しました。")
    }

    object Code {
        suspend fun execute(sender: CommandSender, code: String, action: String, args: List<String>) {
            val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
            if (codes == null) {
                sender.sendMessage("${ChatColor.RED}コードが見つかりません。")
                return
            }
            when (action) {
                "info" -> info(sender, codes)
                "clearusage" -> clearUsage(sender, codes)
                "set" -> Set.execute(sender, codes, args)
                "handlers" -> Handlers.execute(sender, codes, args)
            }
        }

        private fun info(sender: CommandSender, codes: Codes) {
            if (sender.requires("azisabagift.code.info")) return
            sender.sendMessage(
                "${ChatColor.GREEN}コード「${ChatColor.YELLOW}${codes.code}${ChatColor.GREEN}」"
            )
            sender.sendMessage(
                "${ChatColor.GRAY} - ${ChatColor.GREEN}ID: ${ChatColor.YELLOW}${codes.id}"
            )
            sender.sendMessage(
                "${ChatColor.GRAY} - ${ChatColor.GREEN}Selector (コードを使用できるプレイヤー): ${ChatColor.YELLOW}${codes.selector}"
            )
            sender.sendMessage(
                "${ChatColor.GRAY} - ${ChatColor.GREEN}Handlerの数: ${ChatColor.YELLOW}${codes.handler.handlers.size}"
            )
            sender.sendMessage(
                "${ChatColor.GRAY} - ${ChatColor.GREEN}使用できるサーバー: ${ChatColor.YELLOW}${codes.data.allowedOnServer}"
            )
            sender.sendMessage(
                "${ChatColor.GRAY} - ${ChatColor.GREEN}ステータス: ${ChatColor.YELLOW}${codes.data.expirationStatus}"
            )
        }

        private suspend fun clearUsage(
            sender: CommandSender,
            codes: Codes,
        ) {
            if (sender.requires("azisabagift.code.clearusage")) return
            DatabaseManager.executeUpdate("DELETE FROM `used_codes` WHERE `code` = ?", codes.code).close()
            sender.sendMessage("${ChatColor.GREEN}コードの使用回数(使用したプレイヤー)をリセットしました。")
        }

        object Set {
            suspend fun execute(sender: CommandSender, codes: Codes, args: List<String>) {
                if (sender.requires("azisabagift.code.set")) return
                if (args.isEmpty()) {
                    val choices = commandMap
                        .getAs<Map<String, Any>>("code")
                        .getAs<Map<String, Any>>(":__any__:<code>")
                        .getAs<Map<String, Any>>("set")
                        .keys
                        .joinToString("|")
                    sender.sendMessage("${ChatColor.RED}/azisabagift code <code> set (${choices})")
                    return
                }
                when (args[0].lowercase()) {
                    "selector" -> {
                        if (sender.requires("azisabagift.code.set.selector")) return
                        if (args.size < 2) {
                            sender.sendMessage("${ChatColor.RED}/azisabagift code <code> set selector <type> [<data>]")
                            return
                        }
                        val type = args[1]
                        val data = args.drop(2).let { if (it.isEmpty()) "{}" else it.joinToString(" ") }
                        setSelector(sender, codes, type, data)
                    }
                    "expirationstatus" -> {
                        if (sender.requires("azisabagift.code.set.expirationstatus")) return
                        if (args.size < 2) {
                            sender.sendMessage("${ChatColor.RED}/azisabagift code <code> set expirationstatus <type> [<data>]")
                            return
                        }
                        val type = args[1]
                        val data = args.drop(2).let { if (it.isEmpty()) "{}" else it.joinToString(" ") }
                        setExpirationStatus(sender, codes, type, data)
                    }
                    "allowedserver" -> {
                        if (sender.requires("azisabagift.code.set.allowedserver")) return
                        if (args.size == 1) {
                            sender.sendMessage("${ChatColor.RED}/azisabagift code <code> set allowedserver <pattern>")
                            return
                        }
                        val pattern = args[1]
                        setAllowedServer(sender, codes, pattern)
                    }
                }
            }

            private suspend fun setSelector(
                sender: CommandSender,
                codes: Codes,
                type: String,
                data: String = "{}",
            ) {
                val serializer = Registry.SELECTOR.findSerializerBySerialName(type)
                if (serializer == null) {
                    sender.sendMessage("${ChatColor.RED}指定されたSelectorは無効です。")
                    return
                }
                val selector = JSON.decodeFromString(serializer, data)
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `selector` = ? WHERE `id` = ?",
                    JSON.encodeToString(selector),
                    codes.id,
                ).close()
                sender.sendMessage("${ChatColor.GREEN}Selectorを${selector}に変更しました。")
            }

            private suspend fun setExpirationStatus(
                sender: CommandSender,
                codes: Codes,
                type: String,
                data: String = "{}",
            ) {
                val serializer = Registry.EXPIRATION_STATUS.findSerializerBySerialName(type)
                if (serializer == null) {
                    sender.sendMessage("${ChatColor.RED}指定されたExpirationStatusは無効です。")
                    return
                }
                val expirationStatus = try {
                    JSON.decodeFromString(serializer, data)
                } catch (e: Exception) {
                    sender.sendMessage("${ChatColor.RED}${e.javaClass.simpleName}: ${e.message}")
                    throw e
                }
                val newData = codes.data.copy(expirationStatus = expirationStatus)
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `data` = ? WHERE `id` = ?",
                    JSON.encodeToString(newData),
                    codes.id,
                ).close()
                sender.sendMessage("${ChatColor.GREEN}コードのステータスを${expirationStatus}に変更しました。")
            }

            private suspend fun setAllowedServer(
                sender: CommandSender,
                codes: Codes,
                regex: String,
            ) {
                try {
                    regex.toRegex()
                } catch (e: Exception) {
                    sender.sendMessage("${ChatColor.RED}指定された正規表現は無効です。")
                    return
                }
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `data` = ? WHERE `id` = ?",
                    JSON.encodeToString(codes.data.copy(allowedOnServer = regex)),
                    codes.id,
                ).close()
                sender.sendMessage("${ChatColor.GREEN}使用できるサーバーを${ChatColor.YELLOW}$regex${ChatColor.GREEN}に変更しました。")
            }
        }

        object Handlers {
            suspend fun execute(sender: CommandSender, codes: Codes, args: List<String>) {
                if (sender.requires("azisabagift.code.handlers")) return
                if (args.isEmpty()) {
                    val choices = commandMap
                        .getAs<Map<String, Any>>("code")
                        .getAs<Map<String, Any>>(":__any__:<code>")
                        .getAs<Map<String, Any>>("handlers")
                        .keys
                        .joinToString("|")
                    sender.sendMessage("${ChatColor.RED}/azisabagift code <code> handlers (${choices})")
                    return
                }
                when (args[0]) {
                    "info" -> info(sender, codes)
                    "clear" -> clear(sender, codes)
                    "remove" -> {
                        if (args.size < 2) {
                            sender.sendMessage("${ChatColor.RED}/azisabagift code <code> handlers remove <position-from-1>")
                            return
                        }
                        val position = args[1].toIntOrNull() ?: run {
                            sender.sendMessage("${ChatColor.RED}指定された数値は無効です。")
                            return
                        }
                        remove(sender, codes, position)
                    }
                    "insert" -> {
                        if (args.size < 3) {
                            sender.sendMessage("${ChatColor.RED}/azisabagift code <code> handlers insert <position-from-1> <type> [<data>]")
                            return
                        }
                        val position = args[1].toIntOrNull() ?: run {
                            sender.sendMessage("${ChatColor.RED}指定された数値は無効です。")
                            return
                        }
                        val data = args.drop(3).let { if (it.isEmpty()) "{}" else it.joinToString(" ") }
                        insert(sender, codes, position, args[2], data)
                    }
                    "append" -> {
                        if (args.size < 2) {
                            sender.sendMessage("${ChatColor.RED}/azisabagift code <code> handlers append <type> [<data>]")
                            return
                        }
                        val data = args.drop(2).let { if (it.isEmpty()) "{}" else it.joinToString(" ") }
                        append(sender, codes, args[1], data)
                    }
                    "itemhandler" -> {
                        if (args.size < 3) {
                            val choices = commandMap
                                .getAs<Map<String, Any>>("code")
                                .getAs<Map<String, Any>>(":__any__:<code>")
                                .getAs<Map<String, Any>>("handlers")
                                .getAs<Map<String, Any>>("itemhandler")
                                .getAs<Map<String, Any>>(":__any__:<position-from-1>")
                                .keys
                                .joinToString("|")
                            sender.sendMessage("${ChatColor.RED}/azisabagift code <code> handlers itemhandler <position-from-1> ($choices)")
                            return
                        }
                        val position = args[1].toIntOrNull() ?: run {
                            sender.sendMessage("${ChatColor.RED}指定された数値は無効です。")
                            return
                        }
                        val action = args[2]
                        ItemHandler.execute(sender, codes, position, action, args.drop(3))
                    }
                }
            }

            private fun info(sender: CommandSender, codes: Codes) {
                if (sender.requires("azisabagift.code.handlers.info")) return
                sender.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}> ${ChatColor.RESET}${ChatColor.GREEN}Handler一覧:")
                codes.handler.handlers.forEachIndexed { index, handler ->
                    sender.sendMessage("${ChatColor.GRAY} ${index + 1}. ${ChatColor.YELLOW}$handler")
                }
            }

            private suspend fun clear(sender: CommandSender, codes: Codes) {
                if (sender.requires("azisabagift.code.handlers.clear")) return
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `handler` = ? WHERE `id` = ?",
                    JSON.encodeToString(codes.handler.copy(handlers = emptyList())),
                    codes.id,
                ).close()
                sender.sendMessage("${ChatColor.GREEN}${codes.handler.handlers.size}個のHandlerを削除しました。")
                codes.handler.handlers.forEachIndexed { index, handler ->
                    sender.sendMessage("${ChatColor.GRAY} ${index + 1}. ${ChatColor.YELLOW}$handler")
                }
            }

            private suspend fun remove(
                sender: CommandSender,
                codes: Codes,
                position: Int, // first element is 1
            ) {
                if (sender.requires("azisabagift.code.handlers.remove")) return
                if (position < 1 || position > codes.handler.handlers.size) {
                    sender.sendMessage("${ChatColor.RED}位置は1以上${codes.handler.handlers.size}以下にしてください。")
                    return
                }
                val oldHandler = codes.handler.handlers[position - 1]
                val newHandlerList = codes.handler.copy(handlers = codes.handler.handlers.toMutableList().apply { removeAt(position - 1) })
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `handler` = ? WHERE `id` = ?",
                    JSON.encodeToString(newHandlerList),
                    codes.id,
                ).close()
                sender.sendMessage("${ChatColor.GREEN}#${position} (${oldHandler})を削除しました。")
            }

            private suspend fun insert(
                sender: CommandSender,
                codes: Codes,
                position: Int, // first element is 1
                handlerType: String,
                data: String = "{}",
            ) {
                if (sender.requires("azisabagift.code.handlers.insert")) return
                if (position < 1 || position > codes.handler.handlers.size) {
                    sender.sendMessage("${ChatColor.RED}位置は1以上${codes.handler.handlers.size}以下にしてください。")
                    return
                }
                val serializer = Registry.HANDLER.findSerializerBySerialName(handlerType)
                if (serializer == null) {
                    sender.sendMessage("${ChatColor.RED}Handlerが見つかりません。")
                    return
                }
                val handler = try {
                    JSON.decodeFromString(serializer, data)
                } catch (e: Exception) {
                    sender.sendMessage("${ChatColor.RED}${e.javaClass.simpleName}: ${e.message}")
                    return
                }
                val newHandlerList = codes.handler.copy(
                    handlers = codes.handler.handlers.toMutableList().apply { add(position - 1, handler) })
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `handler` = ? WHERE `id` = ?",
                    JSON.encodeToString(newHandlerList),
                    codes.id,
                ).close()
                sender.sendMessage("${ChatColor.GREEN}${handler}を追加しました。")
            }

            object ItemHandler {
                suspend fun execute(
                    sender: CommandSender,
                    codes: Codes,
                    position: Int, // first element is 1
                    action: String,
                    args: List<String>,
                ) {
                    if (sender.requires("azisabagift.code.handlers.itemhandler")) return
                    if (position < 1 || position > codes.handler.handlers.size) {
                        sender.sendMessage("${ChatColor.RED}位置は1以上${codes.handler.handlers.size}以下にしてください。")
                        return
                    }
                    val itemHandler = codes.handler.handlers[position - 1] as? net.azisaba.gift.spigot.handlers.ItemHandler ?: run {
                        sender.sendMessage("${ChatColor.RED}指定されたHandlerはItemHandlerではありません。")
                        return
                    }
                    when (action.lowercase()) {
                        "add" -> add(sender, codes, itemHandler)
                        "remove" -> {
                            val itemPosition = args[0].toIntOrNull() ?: run {
                                sender.sendMessage("${ChatColor.RED}指定された数値は無効です。")
                                return
                            }
                            remove(sender, codes, itemHandler, itemPosition)
                        }
                        "info" -> {
                            if (args.isEmpty()) {
                                info(sender, itemHandler)
                            } else {
                                val itemPosition = args[0].toIntOrNull() ?: run {
                                    sender.sendMessage("${ChatColor.RED}指定された数値は無効です。")
                                    return
                                }
                                infoItem(sender, itemHandler, itemPosition)
                            }
                        }
                        "getitem" -> {
                            val itemPosition = args[0].toIntOrNull() ?: run {
                                sender.sendMessage("${ChatColor.RED}指定された数値は無効です。")
                                return
                            }
                            getItem(sender, itemHandler, itemPosition)
                        }
                    }
                }

                private suspend fun add(
                    sender: CommandSender,
                    codes: Codes,
                    itemHandler: net.azisaba.gift.spigot.handlers.ItemHandler,
                ) {
                    if (sender !is Player) {
                        sender.sendMessage("${ChatColor.RED}このコマンドはプレイヤーのみ実行できます。")
                        return
                    }
                    if (sender.requires("azisabagift.code.handlers.itemhandler.add")) return
                    itemHandler.getMaxItemListSize()?.let {
                        if (itemHandler.getItemList().size >= it) {
                            sender.sendMessage("${ChatColor.RED}このHandlerには追加できるアイテム数は${it}個までです。")
                            return
                        }
                    }
                    val item = sender.inventory.itemInMainHand
                    if (item == null || item.type == Material.AIR) {
                        sender.sendMessage("${ChatColor.RED}手に持っているアイテムがありません。")
                        return
                    }
                    // this method mutates itself
                    itemHandler.setItemList(itemHandler.getItemList().toMutableList().apply { add(item) })
                    DatabaseManager.executeUpdate(
                        "UPDATE `codes` SET `handler` = ? WHERE `id` = ?",
                        JSON.encodeToString(codes.handler),
                        codes.id,
                    ).close()
                    sender.sendMessage("${ChatColor.YELLOW}${item.toFriendlyOutput()}${ChatColor.GREEN}を追加しました。")
                }

                private suspend fun remove(
                    sender: CommandSender,
                    codes: Codes,
                    itemHandler: net.azisaba.gift.spigot.handlers.ItemHandler,
                    position: Int, // first element is 1
                ) {
                    if (sender.requires("azisabagift.code.handlers.itemhandler.remove")) return
                    if (position < 1 || position > itemHandler.getItemList().size) {
                        sender.sendMessage("${ChatColor.RED}位置は1以上${itemHandler.getItemList().size}以下にしてください。")
                        return
                    }
                    val oldItem = itemHandler.getItemList()[position - 1]
                    itemHandler.setItemList(itemHandler.getItemList().toMutableList().apply { removeAt(position - 1) })
                    DatabaseManager.executeUpdate(
                        "UPDATE `codes` SET `handler` = ? WHERE `id` = ?",
                        JSON.encodeToString(codes.handler),
                        codes.id,
                    ).close()
                    sender.sendMessage(
                        "${ChatColor.GREEN}#${position} (${ChatColor.YELLOW}${oldItem.toFriendlyOutput()}${ChatColor.GREEN})を削除しました。"
                    )
                }

                private fun info(
                    sender: CommandSender,
                    itemHandler: net.azisaba.gift.spigot.handlers.ItemHandler,
                ) {
                    if (sender.requires("azisabagift.code.handlers.itemhandler.info")) return
                    itemHandler.getMaxItemListSize().apply {
                        if (this == null) {
                            sender.sendMessage("${ChatColor.GREEN}アイテム一覧:")
                        } else {
                            sender.sendMessage("${ChatColor.GREEN}アイテム一覧 (最大: $this):")
                        }
                    }
                    itemHandler.getItemList().forEachIndexed { index, stack ->
                        sender.sendMessage("${ChatColor.GRAY} ${index + 1}. ${ChatColor.YELLOW}${stack.toFriendlyOutput()}")
                    }
                }

                private fun infoItem(
                    sender: CommandSender,
                    itemHandler: net.azisaba.gift.spigot.handlers.ItemHandler,
                    position: Int, // first element is 1
                ) {
                    if (sender.requires("azisabagift.code.handlers.itemhandler.info")) return
                    if (position < 1 || position > itemHandler.getItemList().size) {
                        sender.sendMessage("${ChatColor.RED}位置は1以上${itemHandler.getItemList().size}以下にしてください。")
                        return
                    }
                    val item = itemHandler.getItemList()[position - 1]
                    sender.sendMessage("${ChatColor.GREEN}#${position} (${ChatColor.YELLOW}${itemHandler.getItemList()[position - 1].toFriendlyOutput()}${ChatColor.GREEN}):")
                    sender.sendMessage("${ChatColor.GREEN} - Type: ${ChatColor.RESET}${item.type}")
                    sender.sendMessage("${ChatColor.GREEN} - Amount: ${ChatColor.RESET}${item.amount}")
                    sender.sendMessage("${ChatColor.GREEN} - Display name: ${ChatColor.RESET}${item.itemMeta?.displayName}")
                }

                private fun getItem(
                    sender: CommandSender,
                    itemHandler: net.azisaba.gift.spigot.handlers.ItemHandler,
                    position: Int, // first element is 1
                ) {
                    val player = sender as? Player ?: run {
                        sender.sendMessage("${ChatColor.RED}プレイヤーのみ実行できます。")
                        return
                    }
                    if (sender.requires("azisabagift.code.handlers.itemhandler.getitem")) return
                    if (position < 1 || position > itemHandler.getItemList().size) {
                        sender.sendMessage("${ChatColor.RED}位置は1以上${itemHandler.getItemList().size}以下にしてください。")
                        return
                    }
                    val item = itemHandler.getItemList()[position - 1]
                    player.inventory.addItem(item)
                }
            }
        }

        suspend fun append(sender: CommandSender, codes: Codes, handlerType: String, data: String = "{}") {
            if (sender.requires("azisabagift.code.handlers.append")) return
            val serializer = Registry.HANDLER.findSerializerBySerialName(handlerType)
            if (serializer == null) {
                sender.sendMessage("${ChatColor.RED}Handlerが見つかりません。")
                return
            }
            val handler = try {
                JSON.decodeFromString(serializer, data)
            } catch (e: Exception) {
                sender.sendMessage("${ChatColor.RED}${e.javaClass.simpleName}: ${e.message}")
                return
            }
            val newHandlerList = codes.handler.copy(handlers = codes.handler.handlers + handler)
            DatabaseManager.executeUpdate(
                "UPDATE `codes` SET `handler` = ? WHERE `id` = ?",
                JSON.encodeToString(newHandlerList),
                codes.id,
            ).close()
            sender.sendMessage("${ChatColor.GREEN}${handler}を追加しました。")
        }
    }
}

private fun CommandSender.requires(permission: String): Boolean {
    if (hasPermission(permission)) return false
    sendMessage("${ChatColor.RED}You don't have permission to do this.")
    return true
}
