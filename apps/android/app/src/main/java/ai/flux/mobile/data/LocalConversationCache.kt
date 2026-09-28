package ai.flux.mobile.data

import android.content.Context
import ai.flux.mobile.model.Role
import ai.flux.mobile.model.UiMessage
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalConversationCache(context: Context) {
    private val preferences = context.getSharedPreferences("flux_local_cache", Context.MODE_PRIVATE)

    fun conversationId(): String {
        val existing = preferences.getString("conversation_id", null)
        if (existing != null) return existing
        return UUID.randomUUID().toString().also {
            preferences.edit().putString("conversation_id", it).apply()
        }
    }

    fun loadMessages(): List<UiMessage> = runCatching {
        val array = JSONArray(preferences.getString("recent_messages", "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(UiMessage(
                    id = item.getString("id"),
                    role = Role.valueOf(item.getString("role")),
                    content = item.getString("content"),
                    mode = item.optString("mode").takeIf(String::isNotBlank),
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun saveMessages(messages: List<UiMessage>) {
        val array = JSONArray()
        messages.takeLast(50).forEach { message ->
            array.put(JSONObject().apply {
                put("id", message.id)
                put("role", message.role.name)
                put("content", message.content)
                put("mode", message.mode ?: "")
            })
        }
        preferences.edit().putString("recent_messages", array.toString()).apply()
    }
}
