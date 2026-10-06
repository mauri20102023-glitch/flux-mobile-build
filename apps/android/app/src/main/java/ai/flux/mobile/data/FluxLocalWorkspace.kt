package ai.flux.mobile.data

import android.content.Context
import ai.flux.mobile.model.LocalProject
import ai.flux.mobile.model.LocalTask
import ai.flux.mobile.model.PendingChat
import org.json.JSONArray
import org.json.JSONObject

class FluxLocalWorkspace(context: Context) {
    private val preferences = context.getSharedPreferences("flux_workspace", Context.MODE_PRIVATE)

    fun loadTasks(): List<LocalTask> = runCatching {
        val stored = preferences.getString("tasks", null) ?: return emptyList()
        val array = JSONArray(stored)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(LocalTask(item.getString("id"), item.getString("title"), item.optBoolean("completed")))
            }
        }
    }.getOrElse { emptyList() }

    fun saveTasks(tasks: List<LocalTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("completed", task.completed)
            })
        }
        preferences.edit().putString("tasks", array.toString()).apply()
    }

    fun loadProjects(): List<LocalProject> = runCatching {
        val stored = preferences.getString("projects", null) ?: return emptyList()
        val array = JSONArray(stored)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(LocalProject(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    objective = item.getString("objective"),
                    progress = item.optInt("progress"),
                ))
            }
        }
    }.getOrElse { emptyList() }

    fun saveProjects(projects: List<LocalProject>) {
        val array = JSONArray()
        projects.forEach { project ->
            array.put(JSONObject().apply {
                put("id", project.id)
                put("name", project.name)
                put("objective", project.objective)
                put("progress", project.progress)
            })
        }
        preferences.edit().putString("projects", array.toString()).apply()
    }

    fun memoryEnabled(): Boolean = preferences.getBoolean("memory_enabled", true)
    fun moderateProactivity(): Boolean = preferences.getBoolean("moderate_proactivity", true)
    // Hands-free is the primary FLUX experience. The first launch asks for the
    // Android microphone/notification permissions; the user can disable it.
    fun wakeWordEnabled(): Boolean = preferences.getBoolean("wake_word_enabled", true)
    fun accentKey(): String {
        val stored = preferences.getString("accent_key", null)
        if (stored == null || stored == "red" && !preferences.getBoolean("blue_upgrade", false)) {
            preferences.edit().putString("accent_key", "blue").putBoolean("blue_upgrade", true).apply()
            return "blue"
        }
        return stored
    }

    fun setMemoryEnabled(value: Boolean) = preferences.edit().putBoolean("memory_enabled", value).apply()
    fun setModerateProactivity(value: Boolean) = preferences.edit().putBoolean("moderate_proactivity", value).apply()
    fun setWakeWordEnabled(value: Boolean) = preferences.edit().putBoolean("wake_word_enabled", value).apply()
    fun setAccentKey(value: String) = preferences.edit().putString("accent_key", value).apply()

    fun assistantPromptDismissed(): Boolean = preferences.getBoolean("assistant_prompt_dismissed", false)

    fun setAssistantPromptDismissed(value: Boolean) =
        preferences.edit().putBoolean("assistant_prompt_dismissed", value).apply()

    fun loadPendingChats(): List<PendingChat> = runCatching {
        val array = JSONArray(preferences.getString("pending_chats", "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(PendingChat(
                    messageId = item.getString("messageId"),
                    content = item.getString("content"),
                    voice = item.optBoolean("voice"),
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun savePendingChats(messages: List<PendingChat>) {
        val array = JSONArray()
        messages.forEach { message ->
            array.put(JSONObject().apply {
                put("messageId", message.messageId)
                put("content", message.content)
                put("voice", message.voice)
            })
        }
        preferences.edit().putString("pending_chats", array.toString()).apply()
    }

}
