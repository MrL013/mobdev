package data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatCache(context: Context) {
    private val prefs = context.getSharedPreferences("chat_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun loadChannels(): List<String> {
        val raw = prefs.getString(KEY_CHANNELS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    fun saveChannels(channels: List<String>) {
        prefs.edit().putString(KEY_CHANNELS, json.encodeToString(channels.distinct())).apply()
    }

    fun loadMessagePage(channel: String, lastKnownId: String?, limit: Int): List<MessageDto> {
        val messages = loadMessages(channel).sortedByDescending { it.sortId() }
        val page = if (lastKnownId == null) {
            messages
        } else {
            val lastId = lastKnownId.toLongOrNull()
            messages.filter { message ->
                val messageId = message.id?.toLongOrNull()
                lastId == null || messageId == null || messageId < lastId
            }
        }
        return page.take(limit)
    }

    fun mergeAndSaveMessages(channel: String, incoming: List<MessageDto>) {
        val merged = (loadMessages(channel) + incoming)
            .distinctBy { message -> message.id ?: message.contentKey() }
            .sortedBy { it.sortId() }
        saveMessages(channel, merged)
    }

    fun saveMessages(channel: String, messages: List<MessageDto>) {
        val merged = messages
            .distinctBy { message -> message.id ?: message.contentKey() }
            .sortedBy { it.sortId() }
        prefs.edit().putString(messageKey(channel), json.encodeToString(merged)).apply()
    }

    private fun loadMessages(channel: String): List<MessageDto> {
        val raw = prefs.getString(messageKey(channel), null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<MessageDto>>(raw) }.getOrDefault(emptyList())
    }

    private fun MessageDto.contentKey(): List<String?> {
        return listOf(from, to, time, data.text?.text, data.image?.link)
    }

    private fun MessageDto.sortId(): Long {
        return id?.toLongOrNull() ?: time?.toLongOrNull() ?: Long.MAX_VALUE
    }

    private fun messageKey(channel: String): String = "$KEY_MESSAGES_PREFIX$channel"

    companion object {
        private const val KEY_CHANNELS = "channels"
        private const val KEY_MESSAGES_PREFIX = "messages:"
    }
}
