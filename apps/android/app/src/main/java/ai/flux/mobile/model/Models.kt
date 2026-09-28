package ai.flux.mobile.model

import java.util.UUID

enum class Role { USER, FLUX }

data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val mode: String? = null,
)

data class PendingChat(
    val messageId: String,
    val content: String,
    val voice: Boolean = false,
)

data class LocalTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val completed: Boolean = false,
)

data class LocalProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val objective: String,
    val progress: Int = 0,
)

data class FluxUiState(
    val messages: List<UiMessage> = emptyList(),
    val isResponding: Boolean = false,
    val isListening: Boolean = false,
    val networkAvailable: Boolean = true,
    val coreOnline: Boolean = false,
    val aiReady: Boolean = false,
    val activationRequired: Boolean = false,
    val isConnecting: Boolean = false,
    val voiceConfigured: Boolean = false,
    val voiceProvider: String = "unavailable",
    val voiceOfficial: Boolean = false,
    val glassesName: String? = null,
    val coreUrl: String = "",
    val coreAuthConfigured: Boolean = false,
    val tasks: List<LocalTask> = emptyList(),
    val projects: List<LocalProject> = emptyList(),
    val memoryEnabled: Boolean = true,
    val moderateProactivity: Boolean = true,
    val pendingMessageCount: Int = 0,
    val assistantCheckComplete: Boolean = false,
    val assistantAvailable: Boolean = false,
    val assistantRoleHeld: Boolean = false,
    val showAssistantOnboarding: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val accentKey: String = "red",
    val imageGenerating: Boolean = false,
    val generatedImageBase64: String? = null,
    val error: String? = null,
)
