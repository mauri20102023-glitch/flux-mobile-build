package ai.flux.mobile

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import ai.flux.mobile.audio.BluetoothAudioRouter
import ai.flux.mobile.audio.FluxTextBridge
import ai.flux.mobile.audio.FluxVoiceBridge
import ai.flux.mobile.assistant.FluxVoiceInteractionService
import ai.flux.mobile.assistant.FluxWakeWordService
import ai.flux.mobile.integrations.ExternalAppRouter
import ai.flux.mobile.data.FluxLocalContext
import kotlinx.coroutines.launch
import android.content.Context
import java.util.Locale

class MainActivity : ComponentActivity() {
    private enum class PermissionAction { VOICE, WAKE, DEVICE_SCAN, CONTEXT }

    private lateinit var viewModel: FluxViewModel
    // O canal de áudio em tempo real não participa da abertura do aplicativo.
    // A instância só existe depois que Maurício toca no microfone.
    private var voice: FluxVoiceBridge? = null
    private var text: FluxTextBridge? = null
    private var pendingContextMessage: String? = null
    private lateinit var bluetooth: BluetoothAudioRouter
    private lateinit var externalApps: ExternalAppRouter
    private var pendingVoiceStart = false
    private var pendingVisionStart = false
    private var permissionAction: PermissionAction? = null

    private val assistantRoleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateAssistantRoleState()
        if (!isAssistantRoleHeld()) {
            viewModel.reportError("O Flux ainda não foi selecionado como assistente padrão.")
        } else {
            viewModel.clearError()
            if (pendingVisionStart) {
                pendingVisionStart = false
                openVision()
            }
        }
    }

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val action = permissionAction
        permissionAction = null
        when (action) {
            PermissionAction.VOICE -> {
                if (hasPermission(Manifest.permission.RECORD_AUDIO)) startListening()
                else viewModel.reportError("Autorize o microfone para conversar por voz.")
            }
            PermissionAction.WAKE -> {
                if (hasPermission(Manifest.permission.RECORD_AUDIO) && hasNotificationPermission()) {
                    startWakeWordService()
                } else {
                    viewModel.setWakeWordEnabled(false)
                    viewModel.reportError("Autorize microfone e notificações para ativar por “Flux”.")
                }
            }
            PermissionAction.DEVICE_SCAN -> scanGlasses()
            PermissionAction.CONTEXT -> pendingContextMessage?.let { message ->
                pendingContextMessage = null
                sendWithContext(message)
            }
            null -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as FluxApplication
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FluxViewModel(
                application.api,
                application.cache,
                application.connectionSettings,
                application.workspace,
                application.networkMonitor,
            ) as T
        })[FluxViewModel::class.java]

        bluetooth = BluetoothAudioRouter(this)
        externalApps = ExternalAppRouter(this, viewModel::reportError)
        pendingVoiceStart = intent.getBooleanExtra("start_voice", false) || intent.action == Intent.ACTION_ASSIST
        pendingVisionStart = intent.getBooleanExtra("start_vision", false)
        updateAssistantRoleState()

        setContent {
            val state by viewModel.state.collectAsState()
            FluxTheme(state.accentKey) {
                FluxMobileApp(
                    state = state,
                    onSend = ::sendText,
                    onBriefing = { sendText("Bom dia") },
                    onVoice = ::requestVoice,
                    onStop = ::stopListening,
                    onAssistantSetup = ::requestAssistantRole,
                    onAssistantDismiss = viewModel::dismissAssistantPrompt,
                    onAppSettings = ::openAppSettings,
                    onNotificationSettings = ::openNotificationSettings,
                    onScanDevices = ::requestDeviceScan,
                    onTestVoice = ::requestVoice,
                    onOpenSpotify = externalApps::openSpotify,
                    onOpenWhatsApp = externalApps::prepareWhatsApp,
                    onOpenMercadoLivre = externalApps::openMercadoLivre,
                    onOpenInstagram = externalApps::openInstagram,
                    onOpenEmail = externalApps::composeEmail,
                    onOpenCalendar = externalApps::openCalendar,
                    onOpenCanva = externalApps::openCanva,
                    onOpenDrive = externalApps::openDrive,
                    onOpenYouTube = externalApps::openYouTube,
                    onOpenSmartThings = externalApps::openSmartThings,
                    onGenerateImage = viewModel::generateImage,
                    onWakeWordEnabled = ::setWakeWordEnabled,
                    onAccentChange = viewModel::setAccentKey,
                    onOpenVision = ::openVision,
                    onCoreUrlChange = viewModel::updateCoreUrl,
                    onCoreTest = { viewModel.reconnect(showFailure = true) },
                    onPairCode = viewModel::pairWithCode,
                    onGeminiKeyChange = viewModel::updateGeminiApiKey,
                    onGeminiKeyClear = viewModel::clearGeminiApiKey,
                    onAddTask = viewModel::addTask,
                    onToggleTask = viewModel::toggleTask,
                    onDeleteTask = viewModel::deleteTask,
                    onAddProject = viewModel::addProject,
                    onMemoryEnabled = viewModel::setMemoryEnabled,
                    onClearConversation = viewModel::clearConversation,
                    onClearError = viewModel::clearError,
                )
                LaunchedEffect(pendingVoiceStart) {
                    if (pendingVoiceStart) {
                        pendingVoiceStart = false
                        requestVoice()
                    }
                }
                LaunchedEffect(pendingVisionStart) {
                    if (pendingVisionStart) openVision()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("start_voice", false) || intent.action == Intent.ACTION_ASSIST) requestVoice()
        if (intent.getBooleanExtra("start_vision", false)) {
            pendingVisionStart = true
            openVision()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            updateAssistantRoleState()
            viewModel.reconnect(showFailure = false)
            if (viewModel.state.value.wakeWordEnabled) {
                if (hasPermission(Manifest.permission.RECORD_AUDIO) && hasNotificationPermission()) {
                    startWakeWordService()
                } else if (permissionAction == null) {
                    // FLUX is hands-free by default. Android still keeps the user
                    // in control through its native runtime permission dialog.
                    setWakeWordEnabled(true)
                }
            }
        }
    }

    override fun onDestroy() {
        runCatching { voice?.destroy() }
        if (::viewModel.isInitialized &&
            (viewModel.state.value.isListening || viewModel.state.value.voiceConnecting)) {
            FluxWakeWordService.resumeAfterConversation()
        }
        runCatching { text?.destroy() }
        runCatching { bluetooth.release() }
        super.onDestroy()
    }

    private fun requestVoice() {
        val required = buildList {
            add(Manifest.permission.RECORD_AUDIO)
        }.filterNot(::hasPermission)

        if (required.isEmpty()) startListening()
        else {
            permissionAction = PermissionAction.VOICE
            permissions.launch(required.toTypedArray())
        }
    }

    private fun requestDeviceScan() {
        val required = if (Build.VERSION.SDK_INT >= 31 && !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else emptyList()

        if (required.isEmpty()) scanGlasses()
        else {
            permissionAction = PermissionAction.DEVICE_SCAN
            permissions.launch(required.toTypedArray())
        }
    }

    private fun startListening() {
        if (viewModel.state.value.isListening || viewModel.state.value.voiceConnecting) return
        runCatching {
            // The wake listener and the conversation cannot own the microphone together.
            FluxWakeWordService.pauseForConversation()
            viewModel.setVoiceConnecting(true)
            val route = bluetooth.detectAndSelect()
            viewModel.setGlasses(route.name)
            voiceController().startSession()
        }.onFailure { failure ->
            onVoiceSessionChanged(false)
            viewModel.reportError(
                failure.message?.takeIf(String::isNotBlank)
                    ?: "Não foi possível iniciar a voz FLUX neste aparelho.",
            )
        }
    }

    private fun stopListening() {
        runCatching { voice?.endSession() }
        onVoiceSessionChanged(false)
    }

    private fun onVoiceSessionChanged(connected: Boolean) {
        viewModel.setListening(connected)
        if (connected) FluxWakeWordService.pauseForConversation()
        else FluxWakeWordService.resumeAfterConversation()
    }

    private fun sendText(message: String) {
        val normalized = message.trim().lowercase(Locale.forLanguageTag("pt-BR"))
        val overview = listOf("bom dia", "me atualize", "resumo do dia", "meu dia").any(normalized::contains)
        val calendar = overview || listOf("agenda", "compromisso", "evento", "reunião").any(normalized::contains)
        val weather = overview || listOf("clima", "tempo", "previsão", "chuva", "temperatura").any(normalized::contains)
        val needed = buildList {
            if (calendar && !hasPermission(Manifest.permission.READ_CALENDAR)) add(Manifest.permission.READ_CALENDAR)
            if (weather && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (needed.isNotEmpty()) {
            pendingContextMessage = message
            permissionAction = PermissionAction.CONTEXT
            permissions.launch(needed.toTypedArray())
            return
        }
        sendWithContext(message)
    }

    private fun sendWithContext(message: String) {
        viewModel.beginDirectMessage(message)
        lifecycleScope.launch {
            runCatching {
                val facts = FluxLocalContext.collect(this@MainActivity,
                    (application as FluxApplication).api, message)
                viewModel.updateDailyFacts(facts)
                textController().send(message, facts.context)
            }.onFailure { viewModel.failDirectMessage(it.message ?: "Não consegui consultar seus dados agora.") }
        }
    }

    private fun textController(): FluxTextBridge = text ?: createTextController().also { text = it }

    @Suppress("UNCHECKED_CAST")
    private fun createTextController(): FluxTextBridge {
        val implementation = Class.forName("ai.flux.mobile.audio.FluxTextController")
        val callback = kotlin.jvm.functions.Function1::class.java
        val constructor = implementation.getConstructor(Context::class.java, callback, callback)
        return constructor.newInstance(
            this,
            viewModel::completeDirectMessage,
            viewModel::failDirectMessage,
        ) as FluxTextBridge
    }

    private fun setWakeWordEnabled(enabled: Boolean) {
        if (!enabled) {
            viewModel.setWakeWordEnabled(false)
            stopService(Intent(this, FluxWakeWordService::class.java))
            return
        }
        if (!onDeviceWakeAvailable()) {
            viewModel.setWakeWordEnabled(false)
            viewModel.reportError("A ativação por voz local não está disponível neste Android. Use o gesto do assistente ou o botão de voz.")
            return
        }
        val required = buildList {
            if (!hasPermission(Manifest.permission.RECORD_AUDIO)) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33 && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        viewModel.setWakeWordEnabled(true)
        if (required.isEmpty()) startWakeWordService()
        else {
            permissionAction = PermissionAction.WAKE
            permissions.launch(required.toTypedArray())
        }
    }

    private fun startWakeWordService() {
        if (!viewModel.state.value.wakeWordEnabled) return
        if (!onDeviceWakeAvailable()) {
            viewModel.setWakeWordEnabled(false)
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, FluxWakeWordService::class.java))
    }

    private fun onDeviceWakeAvailable(): Boolean =
        Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 || hasPermission(Manifest.permission.POST_NOTIFICATIONS)

    private fun openVision() {
        if (!isAssistantRoleHeld()) {
            pendingVisionStart = true
            viewModel.reportError("Defina o FLUX como assistente para ele receber a tela atual.")
            requestAssistantRole()
            return
        }
        pendingVisionStart = false
        if (!FluxVoiceInteractionService.requestSession(vision = true)) {
            viewModel.reportError("O assistente está iniciando. Segure o gesto do assistente novamente em alguns segundos.")
        }
    }

    private fun voiceController(): FluxVoiceBridge = voice ?: createVoiceController().also { voice = it }

    /**
     * O controlador de voz é carregado ao tocar no microfone, para que uma
     * falha de áudio não impeça a abertura da interface principal.
     */
    @Suppress("UNCHECKED_CAST")
    private fun createVoiceController(): FluxVoiceBridge {
        val implementation = Class.forName("ai.flux.mobile.audio.FluxConversationalVoice")
        val callback = kotlin.jvm.functions.Function1::class.java
        val constructor = implementation.getConstructor(
            Context::class.java,
            callback,
            callback,
            callback,
            callback,
        )
        return constructor.newInstance(
            this,
            ::onVoiceSessionChanged,
            viewModel::appendVoiceUser,
            viewModel::appendVoiceAgent,
            viewModel::reportError,
        ) as FluxVoiceBridge
    }

    private fun scanGlasses() {
        val route = bluetooth.detectAndSelect()
        viewModel.setGlasses(route.name)
        if (route.name == null) viewModel.reportError("Nenhum dispositivo Bluetooth de áudio foi encontrado.")
        else viewModel.clearError()
    }

    private fun requestAssistantRole() {
        viewModel.dismissAssistantPrompt()
        if (Build.VERSION.SDK_INT >= 29) {
            val manager = getSystemService(RoleManager::class.java)
            if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                if (manager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                    updateAssistantRoleState()
                } else {
                    launchAssistantIntent(manager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT))
                }
                return
            }
        }
        launchAssistantIntent(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
    }

    private fun updateAssistantRoleState() {
        runCatching {
            val available = if (Build.VERSION.SDK_INT >= 29) {
                getSystemService(RoleManager::class.java)
                    ?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true
            } else {
                true
            }
            viewModel.setAssistantRoleState(available, isAssistantRoleHeld())
        }.onFailure {
            // A tela principal continua funcionando mesmo que a fabricante do
            // aparelho não implemente corretamente o papel de assistente.
            viewModel.setAssistantRoleState(available = false, held = false)
        }
    }

    private fun isAssistantRoleHeld(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            val manager = getSystemService(RoleManager::class.java)
            return manager != null && manager.isRoleAvailable(RoleManager.ROLE_ASSISTANT) &&
                manager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
        }
        return Settings.Secure.getString(contentResolver, "voice_interaction_service")
            ?.startsWith(packageName) == true
    }

    private fun launchAssistantIntent(intent: Intent) {
        if (intent.resolveActivity(packageManager) != null) assistantRoleRequest.launch(intent)
        else launchSafely(Intent(Settings.ACTION_SETTINGS))
    }

    private fun openAppSettings() {
        launchSafely(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun openNotificationSettings() {
        launchSafely(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        })
    }

    private fun launchSafely(intent: Intent) {
        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
        else startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
