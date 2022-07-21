package net.azisaba.gift.velocity.commands

import com.velocitypowered.api.command.CommandSource
import kotlinx.serialization.encodeToString
import net.azisaba.gift.DatabaseManager
import net.azisaba.gift.JSON
import net.azisaba.gift.objects.Codes
import net.azisaba.gift.objects.CodesData
import net.azisaba.gift.objects.CodesTable
import net.azisaba.gift.objects.DebugMessage
import net.azisaba.gift.objects.Everyone
import net.azisaba.gift.objects.ExpirationStatus
import net.azisaba.gift.objects.HandlerList
import net.azisaba.gift.objects.Nobody
import net.azisaba.gift.objects.Selector
import net.azisaba.gift.registry.Registry
import net.azisaba.gift.registry.findSerializerBySerialName
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

// Implementation for /vazisabagift
internal object AzisabaGiftCommandImpl {
    suspend fun createCode(source: CommandSource, code: String): Int {
        val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
        if (codes != null) {
            source.sendMessage(Component.text("指定されたコードはすでに存在します。", NamedTextColor.RED))
            return 0
        }
        val newCodes = Codes(0L, code, Nobody, HandlerList(), CodesData())
        CodesTable.insertB("codes", newCodes) { put("id", null); put("0", null) } // for consistency between different configurations
        source.sendMessage(Component.text("コードを作成しました。", NamedTextColor.GREEN))
        return 0
    }

    internal object Code {
        suspend fun info(
            source: CommandSource,
            code: String,
        ): Int {
            val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
            if (codes == null) {
                source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                return 0
            }
            source.sendMessage(Component.text("コード「", NamedTextColor.GREEN)
                .append(Component.text(code, NamedTextColor.YELLOW))
                .append(Component.text("」", NamedTextColor.GREEN)))
            source.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                .append(Component.text("ID: ", NamedTextColor.GREEN))
                .append(Component.text(codes.id, NamedTextColor.YELLOW)))
            source.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                .append(Component.text("Selector (コードを使用できるプレイヤー): ", NamedTextColor.GREEN))
                .append(Component.text(codes.selector.toString(), NamedTextColor.YELLOW)))
            source.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                .append(Component.text("Handlerの数: ", NamedTextColor.GREEN))
                .append(Component.text(codes.handler.handlers.size, NamedTextColor.YELLOW)))
            source.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                .append(Component.text("使用できるサーバー: ", NamedTextColor.GREEN))
                .append(Component.text(codes.data.allowedOnServer, NamedTextColor.YELLOW)))
            source.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                .append(Component.text("ステータス: ", NamedTextColor.GREEN))
                .append(Component.text(codes.data.expirationStatus.toString(), NamedTextColor.YELLOW)))
            return 0
        }

        suspend fun clearUsage(
            source: CommandSource,
            code: String,
        ): Int {
            val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
            if (codes == null) {
                source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                return 0
            }
            DatabaseManager.executeUpdate("DELETE FROM `used_codes` WHERE `code` = ?", codes.code).close()
            source.sendMessage(Component.text("コードの使用回数(使用したプレイヤー)をリセットしました。", NamedTextColor.GREEN))
            return 0
        }

        internal object Set {
            suspend fun setSelector(
                source: CommandSource,
                code: String,
                type: String,
                data: String = "{}",
            ): Int {
                val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
                if (codes == null) {
                    source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                val serializer = Registry.SELECTOR.findSerializerBySerialName(type)
                if (serializer == null) {
                    source.sendMessage(Component.text("指定されたSelectorは無効です。", NamedTextColor.RED))
                    return 0
                }
                val selector = JSON.decodeFromString(serializer, data)
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `selector` = ? WHERE `id` = ?",
                    JSON.encodeToString(selector),
                    codes.id,
                ).close()
                source.sendMessage(Component.text("Selectorを${selector}に変更しました。", NamedTextColor.GREEN))
                return 0
            }

            suspend fun setExpirationStatus(
                source: CommandSource,
                code: String,
                type: String,
                data: String = "{}",
            ): Int {
                val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
                if (codes == null) {
                    source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                val serializer = Registry.EXPIRATION_STATUS.findSerializerBySerialName(type)
                if (serializer == null) {
                    source.sendMessage(Component.text("指定されたExpirationStatusは無効です。", NamedTextColor.RED))
                    return 0
                }
                val expirationStatus = JSON.decodeFromString(serializer, data)
                val newData = codes.data.copy(expirationStatus = expirationStatus)
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `data` = ? WHERE `id` = ?",
                    JSON.encodeToString(newData),
                    codes.id,
                ).close()
                source.sendMessage(Component.text("コードのステータスを${expirationStatus}に変更しました。", NamedTextColor.GREEN))
                return 0
            }

            suspend fun setAllowedServer(
                source: CommandSource,
                code: String,
                regex: String,
            ): Int {
                val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
                if (codes == null) {
                    source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                try {
                    regex.toRegex()
                } catch (e: Exception) {
                    source.sendMessage(Component.text("指定された正規表現は無効です。", NamedTextColor.RED))
                    return 0
                }
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `data` = ? WHERE `id` = ?",
                    JSON.encodeToString(codes.data.copy(allowedOnServer = regex)),
                    codes.id,
                ).close()
                source.sendMessage(Component.text("使用できるサーバーを", NamedTextColor.GREEN)
                    .append(Component.text(regex, NamedTextColor.YELLOW))
                    .append(Component.text("に変更しました。", NamedTextColor.GREEN)))
                return 0
            }
        }

        internal object Handlers {
            suspend fun info(
                source: CommandSource,
                code: String,
            ): Int {
                val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
                if (codes == null) {
                    source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                source.sendMessage(Component.empty()
                    .append(Component.text("> ", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                    .append(Component.text("Handler一覧: ", NamedTextColor.GREEN)))
                codes.handler.handlers.forEachIndexed { index, handler ->
                    source.sendMessage(Component.text(" ${index + 1}. ", NamedTextColor.GRAY)
                        .append(Component.text(handler.toString(), NamedTextColor.YELLOW)))
                }
                return 0
            }

            suspend fun clear(
                source: CommandSource,
                code: String,
            ): Int {
                val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
                if (codes == null) {
                    source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `handler` = ? WHERE `code` = ?",
                    JSON.encodeToString(codes.handler.copy(handlers = emptyList())),
                    code,
                ).close()
                source.sendMessage(Component.text("${codes.handler.handlers.size}個のHandlerを削除しました。", NamedTextColor.GREEN))
                codes.handler.handlers.forEachIndexed { index, handler ->
                    source.sendMessage(Component.text(" ${index + 1}. ", NamedTextColor.GRAY)
                        .append(Component.text(handler.toString(), NamedTextColor.AQUA)))
                }
                return 0
            }

            suspend fun remove(
                source: CommandSource,
                code: String,
                position: Int, // first element is 1
            ): Int {
                val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
                if (codes == null) {
                    source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                if (position > codes.handler.handlers.size) {
                    source.sendMessage(
                        Component.text(
                            "位置は${codes.handler.handlers.size}以下にしてください。",
                            NamedTextColor.RED
                        )
                    )
                    return 0
                }
                val oldHandler = codes.handler.handlers[position - 1]
                val newHandlerList = codes.handler.copy(handlers = codes.handler.handlers.toMutableList().apply { removeAt(position - 1) })
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `handler` = ? WHERE `code` = ?",
                    JSON.encodeToString(newHandlerList),
                    code
                ).close()
                source.sendMessage(Component.text("#${position} (${oldHandler})を削除しました。", NamedTextColor.GREEN))
                return 0
            }

            suspend fun insert(
                source: CommandSource,
                code: String,
                position: Int, // first element is 1
                handlerType: String,
                data: String = "{}",
            ): Int {
                val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
                if (codes == null) {
                    source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                if (position > codes.handler.handlers.size) {
                    source.sendMessage(
                        Component.text(
                            "位置は${codes.handler.handlers.size}以下にしてください。",
                            NamedTextColor.RED,
                        )
                    )
                    return 0
                }
                val serializer = Registry.HANDLER.findSerializerBySerialName(handlerType)
                if (serializer == null) {
                    source.sendMessage(Component.text("Handlerが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                val handler = JSON.decodeFromString(serializer, data)
                val newHandlerList = codes.handler.copy(
                    handlers = codes.handler.handlers.toMutableList().apply { add(position - 1, handler) })
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `handler` = ? WHERE `code` = ?",
                    JSON.encodeToString(newHandlerList),
                    code
                ).close()
                source.sendMessage(Component.text("${handler}を追加しました。", NamedTextColor.GREEN))
                return 0
            }

            suspend fun append(source: CommandSource, code: String, handlerType: String, data: String = "{}"): Int {
                val codes = CodesTable.select("SELECT * FROM `codes` WHERE `code` = ?", code).firstOrNull()
                if (codes == null) {
                    source.sendMessage(Component.text("コードが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                val serializer = Registry.HANDLER.findSerializerBySerialName(handlerType)
                if (serializer == null) {
                    source.sendMessage(Component.text("Handlerが見つかりません。", NamedTextColor.RED))
                    return 0
                }
                val handler = JSON.decodeFromString(serializer, data)
                val newHandlerList = codes.handler.copy(handlers = codes.handler.handlers + handler)
                DatabaseManager.executeUpdate(
                    "UPDATE `codes` SET `handler` = ? WHERE `code` = ?",
                    JSON.encodeToString(newHandlerList),
                    code
                ).close()
                source.sendMessage(Component.text("${handler}を追加しました。", NamedTextColor.GREEN))
                return 0
            }
        }
    }

    internal suspend fun debugGenerate(source: CommandSource, code: String): Int {
        DatabaseManager.executeUpdate(
            "INSERT INTO `codes` (`code`, `selector`, `handler`, `data`) VALUES (?, ?, ?, ?)",
            code,
            JSON.encodeToString<Selector>(Everyone),
            JSON.encodeToString(HandlerList(listOf(DebugMessage("Hello, world!")))),
            JSON.encodeToString(CodesData(ExpirationStatus.NeverExpire))
        ).close()
        source.sendMessage(Component.text("Generated new code: $code", NamedTextColor.GREEN))
        return 0
    }
}
