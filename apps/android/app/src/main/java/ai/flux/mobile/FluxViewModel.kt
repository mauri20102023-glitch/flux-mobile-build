package ai.flux.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.flux.mobile.data.FluxApiClient
import ai.flux.mobile.data.FluxApiException
import ai.flux.mobile.data.FluxConnectionSettings
import ai.flux.mobile.data.FluxLocalWorkspace
import ai.flux.mobile.data.LocalFacts
import ai.flux.mobile.data.FluxNetworkMonitor
import ai.flux.mobile.data.LocalConversationCache
import ai.flux.mobile.model.FluxUiState
import ai.flux.mobile.model.LocalProject
import ai.flux.mobile.model.LocalTask
import ai.flux.mobile.model.PendingChat
import ai.flux.mobile.model.Role
import ai.flux.mobile.model.UiMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FluxViewModel(
    private val api: FluxApiClient,
    private val cache: LocalConversationCache,
    private val connectionSettings: FluxConnectionSettings,
    private val workspace: FluxLocalWorkspace,
    private val networkMonitor: FluxNetworkMonitor,
) : ViewModel() {
    private val pendingChats = workspace.loadPendingChats().toMutableList()
    private val connectionMutex = Mutex()
    private val sendMutex = Mutex()
    private val _state = MutableStateFlow(FluxUiState(
        messages = cache.loadMessages(),
        coreUrl = connectionSettings.coreUrl(),
        coreAuthConfigured = connectionSettings.authTokenConfigured(),
        geminiKeyConfigured = connectionSettings.geminiApiKeyConfigured(),
        voiceConfigured = false,
        voiceProvider = "unavailable",
        voiceOfficial = false,
        tasks = workspace.loadTasks(),
        projects = workspace.loadProjects(),
        memoryEnabled = workspace.memoryEnabled(),
        moderateProactivity = workspace.moderateProactivity(),
        wakeWordEnabled = workspace.wakeWordEnabled(),
        accentKey = workspace.accentKey(),
        pendingMessageCount = pendingChats.size,
        showAssistantOnboarding = !workspace.assistantPromptDismissed(),
    ))
    val state = _state.asStateFlow()

    fun updateDailyFacts(facts: LocalFacts) {
        _state.update { it.copy(calendarHeadline = facts.calendarHeadline,
            weatherHeadline = facts.weatherHeadline) }
    }
    private var networkAvailable = false
    private var consecutiveConnectionFailures = 0
    private var hasConnectedOnce = false

    init {
        viewModelScope.launch {
            if (connectionSettings.coreUrl() == LEGACY_CORE && connectionSettings.authTokenConfigured()) {
                val oldToken = connectionSettings.authToken()
                runCatching { api.migrateLegacy(oldToken) }.onSuccess { paired ->
                    connectionSettings.updateCoreUrl(NEW_CORE)
                    connectionSettings.updateAuthToken(paired.deviceToken)
                    _state.update { it.copy(coreUrl = NEW_CORE, coreAuthConfigured = true,
                        error = null) }
                    reconnect(showFailure = false)
                }.onFailure {
                    _state.update { current -> current.copy(error =
                        "A migração automática do Core não terminou. O pareamento antigo continua salvo.") }
                }
            }
        }
        viewModelScope.launch {
            networkMonitor.available.collectLatest { available ->
                networkAvailable = available
                _state.update { it.copy(networkAvailable = available) }
                if (available) {
                    if (connectOnce(showFailure = false)) flushPendingChats()
                } else {
                    // Não apague um estado saudável durante a troca rápida entre
                    // Wi-Fi e 4G/5G. As mensagens permanecem na fila e a conexão
                    // é retomada assim que o Android anunciar outra rede.
                    _state.update { it.copy(isConnecting = false) }
                }
            }
        }
        viewModelScope.launch {
            var retryDelay = 2_000L
            while (isActive) {
                if (networkAvailable && state.value.coreUrl.isNotBlank()) {
                    val connected = connectOnce(showFailure = false)
                    if (connected) {
                        flushPendingChats()
                        retryDelay = 25_000L
                    } else {
                        retryDelay = (retryDelay * 2).coerceAtMost(30_000L)
                    }
                } else {
                    retryDelay = 5_000L
                }
                delay(retryDelay)
            }
        }
    }

    companion object {
        private const val LEGACY_CORE = "https://flux-core-12.mauri20102023.workers.dev"
        private const val NEW_CORE = "https://flux-mobile-build2.mauri20102023.workers.dev"
    }

    fun reconnect(showFailure: Boolean = true) {
        if (state.value.coreUrl.isBlank()) {
            _state.update {
                it.copy(
                    coreOnline = false,
                    aiReady = false,
                    aiVerified = false,
                    activationRequired = false,
                    isConnecting = false,
                    voiceConfigured = false,
                    voiceProvider = "unavailable",
                    voiceOfficial = false,
                    error = if (showFailure) "Configure o endereço HTTPS do Flux Core." else null,
                )
            }
            return
        }
        viewModelScope.launch {
            if (connectOnce(showFailure)) flushPendingChats()
        }
    }

    private suspend fun connectOnce(showFailure: Boolean): Boolean = connectionMutex.withLock {
        if (!networkAvailable || state.value.coreUrl.isBlank()) return@withLock false
        if (!connectionSettings.authTokenConfigured()) {
            _state.update {
                it.copy(
                    coreOnline = false,
                    isConnecting = false,
                    error = if (showFailure) {
                        "Pareie este aparelho com o FLUX Core ou ative o Gemini pessoal para voz e chat."
                    } else it.error,
                )
            }
            return@withLock false
        }
        _state.update { it.copy(isConnecting = true) }
        runCatching {
            val diagnostics = api.diagnostics()
            runCatching { api.registerMobile() }
            diagnostics
        }.fold(
            onSuccess = { diagnostics ->
                consecutiveConnectionFailures = 0
                hasConnectedOnce = true
                _state.update {
                    it.copy(
                        coreOnline = diagnostics.coreOk,
                        aiReady = diagnostics.aiReady,
                        activationRequired = diagnostics.activationRequired,
                        isConnecting = false,
                        voiceConfigured = diagnostics.voiceProvider == "inworld-tts" || diagnostics.voiceProvider == "elevenlabs-tts",
                        voiceProvider = diagnostics.voiceProvider,
                        voiceOfficial = diagnostics.voiceOfficial,
                        error = null,
                    )
                }
                diagnostics.coreOk
            },
            onFailure = { failure ->
                consecutiveConnectionFailures += 1
                _state.update { current ->
                    current.copy(
                        // Um único timeout não representa desconexão. Mantemos o
                        // último estado bom e só declaramos offline após três falhas.
                        coreOnline = current.coreOnline && consecutiveConnectionFailures < 3,
                        isConnecting = false,
                        error = if (showFailure) failure.message ?: "Não foi possível alcançar o FLUX Core. Vou continuar tentando." else current.error,
                    )
                }
                false
            },
        )
    }

    fun pairWithCode(value: String) {
        if (_state.value.isConnecting) return
        val code = value.trim()
        if (!Regex("^[A-Za-z0-9_-]{32}$").matches(code)) {
            reportError("O código de pareamento tem 32 caracteres. Confira e tente novamente.")
            return
        }
        _state.update { it.copy(isConnecting = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val paired = api.redeemPairingCode(code)
                connectionSettings.updateAuthToken(paired.deviceToken)
            }.onSuccess {
                _state.update { it.copy(coreAuthConfigured = true, isConnecting = false) }
                reconnect(showFailure = true)
            }.onFailure { failure ->
                _state.update {
                    it.copy(isConnecting = false,
                        error = failure.message ?: "Não foi possível parear o aparelho com o FLUX Core.")
                }
            }
        }
    }

    fun updateCoreUrl(value: String) {
        runCatching { connectionSettings.updateCoreUrl(value) }
            .onSuccess {
                _state.update { it.copy(coreUrl = connectionSettings.coreUrl(), coreOnline = false,
                    coreAuthConfigured = connectionSettings.authTokenConfigured(),
                    voiceVerified = false, aiVerified = false, error = null) }
                reconnect(showFailure = true)
            }
            .onFailure { reportError(it.message ?: "Endereço inválido.") }
    }

    fun updateCoreAuthToken(value: String) {
        runCatching { connectionSettings.updateAuthToken(value) }
            .onSuccess { _state.update { it.copy(coreAuthConfigured = connectionSettings.authTokenConfigured(), error = null) } }
            .onFailure { reportError("Não foi possível proteger o token neste aparelho.") }
    }

    fun updateGeminiApiKey(value: String) {
        runCatching { connectionSettings.updateGeminiApiKey(value) }
            .onSuccess {
                val configured = connectionSettings.geminiApiKeyConfigured()
                _state.update {
                    it.copy(
                        geminiKeyConfigured = configured,
                        aiVerified = false,
                        voiceVerified = false,
                        aiReady = if (configured && !it.coreOnline) false else it.aiReady,
                        voiceConfigured = configured || it.voiceConfigured,
                        voiceProvider = if (configured) "Gemini Live • protegido no aparelho" else it.voiceProvider,
                        voiceOfficial = configured || it.voiceOfficial,
                        error = null,
                    )
                }
            }
            .onFailure { reportError(it.message ?: "Não foi possível proteger a chave neste aparelho.") }
    }

    fun clearGeminiApiKey() {
        connectionSettings.clearGeminiApiKey()
        _state.update {
            it.copy(
                geminiKeyConfigured = false,
                aiVerified = false,
                voiceVerified = false,
                voiceConfigured = false,
                voiceProvider = "unavailable",
                voiceOfficial = false,
                error = null,
            )
        }
        reconnect(showFailure = false)
    }

    fun setVoiceConnecting(value: Boolean) = _state.update { it.copy(voiceConnecting = value) }
    fun setListening(value: Boolean) = _state.update {
        it.copy(isListening = value, voiceConnecting = false, voiceVerified = it.voiceVerified || value)
    }
    fun setGlasses(name: String?) = _state.update { it.copy(glassesName = name) }
    fun reportError(message: String?) = _state.update { it.copy(error = message) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun appendVoiceUser(text: String) = appendVoiceMessage(Role.USER, text, "VOICE")

    fun appendVoiceAgent(text: String) = appendVoiceMessage(Role.FLUX, text, "FLUX LIVE")

    private fun appendVoiceMessage(role: Role, text: String, mode: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val messages = state.value.messages + UiMessage(role = role, content = clean, mode = mode)
        cache.saveMessages(messages)
        _state.update { it.copy(messages = messages, isResponding = false, error = null) }
    }

    fun setAssistantRoleState(available: Boolean, held: Boolean) {
        if (held) workspace.setAssistantPromptDismissed(true)
        _state.update {
            it.copy(
                assistantCheckComplete = true,
                assistantAvailable = available,
                assistantRoleHeld = held,
                showAssistantOnboarding = available && !held && !workspace.assistantPromptDismissed(),
            )
        }
    }

    fun dismissAssistantPrompt() {
        workspace.setAssistantPromptDismissed(true)
        _state.update { it.copy(showAssistantOnboarding = false) }
    }

    fun addTask(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val tasks = state.value.tasks + LocalTask(title = clean)
        workspace.saveTasks(tasks)
        _state.update { it.copy(tasks = tasks) }
    }

    fun toggleTask(id: String) {
        val tasks = state.value.tasks.map { task -> if (task.id == id) task.copy(completed = !task.completed) else task }
        workspace.saveTasks(tasks)
        _state.update { it.copy(tasks = tasks) }
    }

    fun deleteTask(id: String) {
        val tasks = state.value.tasks.filterNot { it.id == id }
        workspace.saveTasks(tasks)
        _state.update { it.copy(tasks = tasks) }
    }

    fun addProject(name: String, objective: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val projects = state.value.projects + LocalProject(
            name = cleanName,
            objective = objective.trim().ifBlank { "Projeto criado no Flux Mobile" },
        )
        workspace.saveProjects(projects)
        _state.update { it.copy(projects = projects) }
    }

    fun setMemoryEnabled(value: Boolean) {
        workspace.setMemoryEnabled(value)
        cache.rotateConversation()
        _state.update { it.copy(memoryEnabled = value) }
    }

    fun setModerateProactivity(value: Boolean) {
        workspace.setModerateProactivity(value)
        _state.update { it.copy(moderateProactivity = value) }
    }

    fun setWakeWordEnabled(value: Boolean) {
        workspace.setWakeWordEnabled(value)
        _state.update { it.copy(wakeWordEnabled = value) }
    }

    fun setAccentKey(value: String) {
        val clean = value.takeIf { it in setOf("red", "cyan", "purple", "gold") } ?: "red"
        workspace.setAccentKey(clean)
        _state.update { it.copy(accentKey = clean) }
    }

    fun beginDirectMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val messages = state.value.messages + UiMessage(role = Role.USER, content = clean, mode = "FLUX LIVE")
        cache.saveMessages(messages)
        _state.update { it.copy(messages = messages, isResponding = true, error = null) }
    }

    fun appendLocalBriefing(request: String, response: String) {
        val messages = state.value.messages +
            UiMessage(role = Role.USER, content = request, mode = "LOCAL") +
            UiMessage(role = Role.FLUX, content = response, mode = "RESUMO LOCAL")
        cache.saveMessages(messages)
        _state.update { it.copy(messages = messages, isResponding = false, error = null) }
    }

    fun completeDirectMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val messages = state.value.messages + UiMessage(role = Role.FLUX, content = clean, mode = "FLUX LIVE")
        cache.saveMessages(messages)
        _state.update { it.copy(messages = messages, isResponding = false,
            aiReady = if (it.geminiKeyConfigured) true else it.aiReady,
            aiVerified = true, error = null) }
    }

    fun failDirectMessage(message: String) {
        _state.update { it.copy(isResponding = false,
            aiReady = if (it.geminiKeyConfigured && !it.coreOnline) false else it.aiReady,
            error = message) }
    }

    fun generateImage(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty() || state.value.imageGenerating) return
        viewModelScope.launch {
            _state.update { it.copy(imageGenerating = true, error = null) }
            if (!state.value.coreOnline) connectOnce(showFailure = false)
            if (!state.value.coreOnline) {
                _state.update { it.copy(imageGenerating = false, error = "Conecte o FLUX Core para gerar imagens com segurança.") }
                return@launch
            }
            runCatching { api.generateImage(clean) }
                .onSuccess { image ->
                    _state.update { it.copy(imageGenerating = false, generatedImageBase64 = image, error = null) }
                }
                .onFailure { failure ->
                    _state.update {
                        it.copy(imageGenerating = false, error = failure.message ?: "O FLUX Studio não conseguiu gerar a imagem.")
                    }
                }
        }
    }

    fun clearConversation() {
        pendingChats.clear()
        workspace.savePendingChats(emptyList())
        cache.saveMessages(emptyList())
        cache.rotateConversation()
        _state.update { it.copy(messages = emptyList(), pendingMessageCount = 0, error = null) }
    }

    fun send(text: String, voice: Boolean = false) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        // Toda conversa passa pela inteligência real. A antiga resposta local
        // baseada em frases prontas foi removida porque soava artificial.
        val user = UiMessage(role = Role.USER, content = clean, mode = "PENDING")
        val withUser = state.value.messages + user
        cache.saveMessages(withUser)
        _state.update { it.copy(messages = withUser, error = null) }

        val pending = PendingChat(user.id, clean, voice)
        pendingChats.add(pending)
        workspace.savePendingChats(pendingChats)
        _state.update { it.copy(pendingMessageCount = pendingChats.size, isResponding = true) }
        viewModelScope.launch { deliverPending(pending) }
    }

    private suspend fun flushPendingChats() {
        for (pending in pendingChats.toList()) {
            if (!networkAvailable) return
            deliverPending(pending)
        }
    }

    private suspend fun deliverPending(pending: PendingChat) = sendMutex.withLock {
        if (pendingChats.none { it.messageId == pending.messageId }) return@withLock
        if (state.value.coreUrl.isBlank()) {
            markDeliveryWaiting("Configure o servidor HTTPS do Flux para enviar a mensagem.")
            return@withLock
        }
        if (!networkAvailable) {
            markDeliveryWaiting("A mensagem está salva. O FLUX enviará assim que a internet voltar.", keepCoreOnline = true)
            return@withLock
        }

        _state.update { it.copy(isResponding = true, error = null) }
        if (!state.value.coreOnline) connectOnce(showFailure = false)
        if (state.value.coreOnline && state.value.activationRequired && !state.value.aiReady) {
            markDeliveryWaiting(
                message = "O FLUX Core está online. Falta ativar a IA para eu responder.",
                keepCoreOnline = true,
            )
            return@withLock
        }
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            if (!networkAvailable) return@repeat
            if (!state.value.coreOnline) connectOnce(showFailure = false)
            if (!state.value.coreOnline) {
                delay(1_000L shl attempt)
                return@repeat
            }

            runCatching {
                api.chat(
                    requestId = pending.messageId,
                    conversationId = if (workspace.memoryEnabled()) cache.conversationId()
                        else java.util.UUID.randomUUID().toString(),
                    message = pending.content,
                    voice = pending.voice,
                )
            }.onSuccess { result ->
                pendingChats.removeAll { it.messageId == pending.messageId }
                workspace.savePendingChats(pendingChats)
                val completed = state.value.messages.map { message ->
                    if (message.id == pending.messageId) message.copy(mode = null) else message
                } + UiMessage(role = Role.FLUX, content = result.content, mode = result.mode)
                cache.saveMessages(completed)
                _state.update {
                    it.copy(
                        messages = completed,
                        isResponding = false,
                        coreOnline = true,
                        aiReady = true,
                        aiVerified = true,
                        pendingMessageCount = pendingChats.size,
                        error = null,
                    )
                }
                return@withLock
            }.onFailure { failure ->
                lastError = failure
                if (failure is FluxApiException) {
                    _state.update { it.copy(coreOnline = true) }
                    if (failure.statusCode == 503) {
                        _state.update { it.copy(aiReady = false, activationRequired = true) }
                    }
                    if (!failure.retryable) {
                        markDeliveryWaiting(failure.message ?: "A IA não aceitou esta solicitação.", keepCoreOnline = true)
                        return@withLock
                    }
                } else {
                    consecutiveConnectionFailures += 1
                    _state.update {
                        it.copy(coreOnline = it.coreOnline && consecutiveConnectionFailures < 3)
                    }
                }
            }

            delay(1_000L shl attempt)
        }
        markDeliveryWaiting(
            message = if (lastError == null) "Sem internet agora. A mensagem está salva e será enviada automaticamente."
            else lastError?.message ?: "A mensagem ficou salva. O FLUX tentará novamente.",
            keepCoreOnline = lastError is FluxApiException || (hasConnectedOnce && consecutiveConnectionFailures < 3),
        )
    }

    private fun markDeliveryWaiting(message: String, keepCoreOnline: Boolean = false) {
        val restored = state.value.messages.map { item ->
            if (pendingChats.any { it.messageId == item.id }) item.copy(mode = "PENDING") else item
        }
        cache.saveMessages(restored)
        _state.update {
            it.copy(
                messages = restored,
                isResponding = false,
                coreOnline = if (keepCoreOnline) it.coreOnline else false,
                pendingMessageCount = pendingChats.size,
                error = message,
            )
        }
    }

}
