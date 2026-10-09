package ai.flux.mobile

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.flux.mobile.model.FluxUiState
import ai.flux.mobile.model.Role
import ai.flux.mobile.model.UiMessage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val PulseRed: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
private val PulseRedDark: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
private val Ink = Color(0xFF06090E)
private val Panel = Color(0xFF10161F)
private val PanelRaised = Color(0xFF192330)
private val Line = Color(0xFF293744)
private val White = Color(0xFFF4F6FA)
private val Muted = Color(0xFF9BA2AD)
private val Green = Color(0xFF52D27F)
private val Amber = Color(0xFFFFBC52)

private enum class FluxTab(val label: String, val icon: ImageVector) {
    HOME("Início", Icons.Default.Home),
    CHAT("Chat", Icons.Default.ChatBubbleOutline),
    AGENDA("Planos", Icons.Default.CheckCircleOutline),
    DEVICES("Dispositivos", Icons.Default.Devices),
    LAB("Workspace", Icons.Default.Build),
    CONTROL("Ajustes", Icons.Default.Settings),
}

@Composable
fun FluxTheme(accentKey: String = "blue", content: @Composable () -> Unit) {
    val (accent, accentDark) = when (accentKey) {
        "blue" -> Color(0xFF17B8FF) to Color(0xFF1255C7)
        "cyan" -> Color(0xFF00D9F5) to Color(0xFF006D7A)
        "purple" -> Color(0xFF8D73FF) to Color(0xFF4932A3)
        "gold" -> Color(0xFFFFB840) to Color(0xFF865700)
        else -> Color(0xFFFF304A) to Color(0xFF9A1025)
    }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            secondary = accentDark,
            onPrimary = Color.White,
            background = Ink,
            onBackground = White,
            surface = Panel,
            onSurface = White,
            surfaceVariant = PanelRaised,
            onSurfaceVariant = Muted,
            outline = Line,
            error = Color(0xFFFF7D88),
        ),
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp),
        ),
        content = content,
    )
}

@Composable
fun FluxMobileApp(
    state: FluxUiState,
    onSend: (String) -> Unit,
    onBriefing: () -> Unit,
    onVoice: () -> Unit,
    onStop: () -> Unit,
    onAssistantSetup: () -> Unit,
    onAssistantDismiss: () -> Unit,
    onAppSettings: () -> Unit,
    onNotificationSettings: () -> Unit,
    onScanDevices: () -> Unit,
    onTestVoice: () -> Unit,
    onOpenSpotify: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onOpenMercadoLivre: () -> Unit,
    onOpenInstagram: () -> Unit,
    onOpenEmail: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenCanva: () -> Unit,
    onOpenDrive: () -> Unit,
    onOpenYouTube: () -> Unit,
    onOpenSmartThings: () -> Unit,
    onGenerateImage: (String) -> Unit,
    onWakeWordEnabled: (Boolean) -> Unit,
    onAccentChange: (String) -> Unit,
    onOpenVision: () -> Unit,
    onCoreUrlChange: (String) -> Unit,
    onCoreTest: () -> Unit,
    onPairCode: (String) -> Unit,
    onGeminiKeyChange: (String) -> Unit,
    onGeminiKeyClear: () -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddProject: (String, String) -> Unit,
    onMemoryEnabled: (Boolean) -> Unit,
    onClearConversation: () -> Unit,
    onClearError: () -> Unit,
) {
    var selectedName by rememberSaveable { mutableStateOf(FluxTab.HOME.name) }
    val selected = FluxTab.valueOf(selectedName)

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { PulseNavigation(selected, {
            if (state.isListening || state.voiceConnecting) onStop() else onVoice()
        }) { selectedName = it.name } },
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0B141C), Ink, Color(0xFF080B10))))
                .padding(padding),
        ) {
            AnimatedContent(targetState = selected, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "Navegação") { currentTab ->
            when (currentTab) {
                FluxTab.HOME -> PulseHome(
                    state = state,
                    onVoice = { if (state.isListening || state.voiceConnecting) onStop() else onVoice() },
                    onBriefing = onBriefing,
                    onChat = { selectedName = FluxTab.CHAT.name },
                    onPlans = { selectedName = FluxTab.AGENDA.name },
                    onStudio = { selectedName = FluxTab.LAB.name },
                    onDevices = { selectedName = FluxTab.DEVICES.name },
                    onSettings = { selectedName = FluxTab.CONTROL.name },
                    onVision = onOpenVision,
                )
                FluxTab.CHAT -> PulseChat(state, onSend, onVoice, onStop) {
                    selectedName = FluxTab.CONTROL.name
                }
                FluxTab.AGENDA -> AgendaScreen(state, onAddTask, onToggleTask, onDeleteTask, onAddProject)
                FluxTab.DEVICES -> DevicesScreen(
                    state, onScanDevices, onAssistantSetup, onOpenSpotify, onOpenWhatsApp, onOpenMercadoLivre,
                    onOpenInstagram, onOpenEmail, onOpenCalendar, onOpenCanva, onOpenDrive, onOpenYouTube,
                    onOpenSmartThings,
                )
                FluxTab.LAB -> FluxWorkspaceScreen(state, onGenerateImage, onSend)
                FluxTab.CONTROL -> ControlScreen(
                    state, onCoreUrlChange, onCoreTest, onPairCode, onTestVoice,
                    onAssistantSetup, onAppSettings, onNotificationSettings, onMemoryEnabled,
                    onClearConversation, onWakeWordEnabled, onAccentChange, onOpenVision,
                    onGeminiKeyChange, onGeminiKeyClear,
                )
            }
            }
            AnimatedVisibility(
                visible = state.error != null,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                state.error?.let { ErrorBanner(it, onClearError) }
            }
        }
    }

    if (state.assistantCheckComplete && state.assistantAvailable && state.showAssistantOnboarding && !state.assistantRoleHeld) {
        AssistantDialog(onAssistantSetup, onAssistantDismiss)
    }
}

@Composable
private fun PulseNavigation(selected: FluxTab, onVoice: () -> Unit, onSelect: (FluxTab) -> Unit) {
    NavigationBar(containerColor = Color(0xF2091017), tonalElevation = 0.dp) {
        listOf(FluxTab.HOME, FluxTab.CHAT).forEach { tab -> PulseNavItem(selected, tab, onSelect) }
        NavigationBarItem(
            selected = false,
            onClick = onVoice,
            icon = {
                Box(
                    Modifier.size(50.dp).background(
                        Brush.radialGradient(listOf(Color(0xFFB7EFFF), PulseRed, Color(0xFF061A4D))), CircleShape,
                    ).border(1.dp, Color(0xFF54DCFF).copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("F", color = White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold) }
            },
            label = { Text("Voz", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent, unselectedTextColor = Muted),
        )
        listOf(FluxTab.LAB, FluxTab.CONTROL).forEach { tab -> PulseNavItem(selected, tab, onSelect) }
    }
}

@Composable
private fun RowScope.PulseNavItem(selected: FluxTab, tab: FluxTab, onSelect: (FluxTab) -> Unit) {
    NavigationBarItem(
        selected = selected == tab,
        onClick = { onSelect(tab) },
        icon = { Icon(tab.icon, tab.label, Modifier.size(23.dp)) },
        label = { Text(tab.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = White, selectedTextColor = White, indicatorColor = PulseRedDark,
            unselectedIconColor = Muted, unselectedTextColor = Muted,
        ),
    )
}

@Composable
private fun PulseHome(
    state: FluxUiState, onVoice: () -> Unit, onBriefing: () -> Unit, onChat: () -> Unit,
    onPlans: () -> Unit, onStudio: () -> Unit, onDevices: () -> Unit, onSettings: () -> Unit,
    onVision: () -> Unit,
) {
    val now = LocalDateTime.now()
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 22.dp).testTag("flux-home")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("F L U X", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 6.sp)
                Text("EXTREME INTELLIGENCE", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Abrir ajustes", tint = Muted) }
        }
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(now.format(DateTimeFormatter.ofPattern("EEEE, dd MMM", Locale("pt", "BR"))).uppercase(), color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(if (state.coreOnline && state.coreAuthConfigured) "● CORE CONECTADO" else "○ PAREAMENTO", color = if (state.coreOnline && state.coreAuthConfigured) accent else Amber, fontSize = 10.sp)
        }
        Spacer(Modifier.height(26.dp))
        Text("Seu universo.\nUma inteligência.", fontSize = 35.sp, lineHeight = 40.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp)
        Text("Boa ${if (now.hour < 12) "manhã" else if (now.hour < 18) "tarde" else "noite"}, Maurício.", color = Muted, modifier = Modifier.padding(top = 12.dp))
        Box(Modifier.fillMaxWidth().height(285.dp), contentAlignment = Alignment.Center) {
            FluxOrb(state, 270.dp, onVoice)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            StatusPill(presenceLabel(state), accent)
        }
        Text("Voz FLUX 4 · ativa após liberação de créditos", color = Muted, fontSize = 11.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeQuickAction(Icons.Default.ChatBubbleOutline, "Conversar", onChat, Modifier.weight(1f))
            HomeQuickAction(Icons.Default.CenterFocusStrong, "Ver tela", onVision, Modifier.weight(1f))
            HomeQuickAction(Icons.Default.AutoAwesome, "Criar", onStudio, Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        SectionLabel("SEU DIA, COM CLAREZA")
        HomeAction(Icons.Default.WbSunny, "Briefing pessoal", "Agenda, clima e seus próximos passos", onBriefing)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(Modifier.weight(1f).clickable(onClick = onPlans), color = Panel, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(18.dp)) {
                    Text("${state.tasks.count { !it.completed }}", fontSize = 32.sp, fontWeight = FontWeight.Light, color = accent)
                    Text("Tarefas abertas", color = Muted, fontSize = 12.sp)
                }
            }
            Surface(Modifier.weight(1f).clickable(onClick = onDevices), color = Panel, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(18.dp)) { Icon(Icons.Default.Devices, null, tint = accent); Spacer(Modifier.height(10.dp)); Text("Conexões", color = Muted, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(18.dp))
        state.messages.lastOrNull { it.role == Role.FLUX }?.let {
            Text("CONTINUE DE ONDE PAROU", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(it.content.take(160), color = White, fontSize = 14.sp, lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp).clickable(onClick = onChat))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeQuickAction(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(modifier.clickable(onClick = onClick), color = PanelRaised, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
            Text(title, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun HomePanel(label: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            SectionLabel(label)
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun HomeAction(icon: ImageVector, title: String, detail: String, onClick: () -> Unit) {
    Surface(
        color = PanelRaised, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PulseRed)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PulseChat(
    state: FluxUiState,
    onSend: (String) -> Unit,
    onVoice: () -> Unit,
    onStop: () -> Unit,
    onActivate: () -> Unit,
) {
    val app=LocalContext.current.applicationContext as FluxApplication
    var mode by remember { mutableStateOf(app.workspace.intelligenceMode()) }
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focus = LocalFocusManager.current
    LaunchedEffect(state.messages.size, state.isResponding) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        PulseHeader(state)
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("FAST" to "Rápido","STANDARD" to "Padrão","DEEP" to "Profundo").forEach{(id,label)->
                FilterChip(selected=mode==id,onClick={mode=id;app.workspace.setIntelligenceMode(id)},enabled=!state.isResponding,label={Text(label)})
            }
        }
        if (state.activationRequired && state.coreOnline) ActivationStrip(onActivate)
        if (state.messages.isEmpty()) {
            EmptyPulse(state, onVoice, onStop, Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.messages, key = { it.id }) { MessageBubble(it) }
                if (state.isResponding) item { ThinkingBubble() }
            }
        }
        Composer(
            value = draft,
            listening = state.isListening || state.voiceConnecting,
            enabled = !state.isResponding,
            onValueChange = { draft = it },
            onVoice = onVoice,
            onStop = onStop,
            onSend = {
                val message = draft.trim()
                if (message.isNotEmpty()) {
                    onSend(message)
                    draft = ""
                    focus.clearFocus()
                }
            },
        )
    }
}

@Composable
private fun PulseHeader(state: FluxUiState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(PulseRed, PulseRedDark))),
            contentAlignment = Alignment.Center,
        ) { Text("F", color = White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Olá, Maurício", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Text("FLUX • ${BuildConfig.VERSION_NAME}", fontSize = 10.sp, color = Muted, letterSpacing = 1.2.sp)
        }
        StatusPill(
            label = when {
                !state.networkAvailable -> "RECONEXÃO"
                state.isConnecting -> "CONECTANDO"
                state.coreOnline && state.aiReady -> "ONLINE"
                state.coreOnline -> "CORE ONLINE"
                state.geminiKeyConfigured -> "CHAVE ANTIGA"
                else -> "CONFIGURAR"
            },
            color = when {
                state.coreOnline && state.aiReady -> Green
                state.coreOnline || state.geminiKeyConfigured || !state.networkAvailable -> Amber
                else -> PulseRed
            },
        )
    }
}

@Composable
private fun presenceLabel(state: FluxUiState): String = when {
    state.voiceConnecting -> "Conectando voz…"
    state.isListening -> "Ouvindo…"
    state.isResponding -> "Pensando…"
    !state.networkAvailable -> "Sem internet"
    !state.coreOnline -> "Conecte o Core para conversar"
    else -> "Disponível"
}

@Composable
private fun FluxOrb(state: FluxUiState, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val animation = rememberInfiniteTransition(label = "Presença FLUX")
    val breath by animation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (state.isListening) 800 else 2600), RepeatMode.Reverse),
        label = "Respiração",
    )
    val active = state.isListening || state.voiceConnecting || state.isResponding
    val accent = if (!state.networkAvailable) Muted else MaterialTheme.colorScheme.primary
    Box(Modifier.size(size).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            val scale = 0.94f + breath * if (active) 0.1f else 0.03f
            scaleX = scale + state.audioLevel * .06f; scaleY = scale + state.audioLevel * .06f
        }.clip(CircleShape).background(Brush.radialGradient(listOf(
            accent.copy(alpha = .22f), accent.copy(alpha = .09f), Color.Transparent))))
        Canvas(Modifier.fillMaxSize(.88f)) {
            val centerX = this.size.width / 2f
            val centerY = this.size.height / 2f
            val base = this.size.minDimension * .36f
            repeat(9) { line ->
                val path = Path()
                for (step in 0..150) {
                    val angle = step / 150f * 2f * Math.PI.toFloat()
                    val wave = sin(angle * (2.7f + line * .11f) + line * .86f + breath * .9f)
                    val radius = base + wave * (6f + line * 1.2f + state.audioLevel * 15f)
                    val x = centerX + cos(angle) * radius
                    val y = centerY + sin(angle) * radius * (.84f + line * .01f)
                    if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = accent.copy(alpha = .24f + line * .046f),
                    style = Stroke(width = if (line % 3 == 0) 2.4f else 1.2f))
            }
        }
        Icon(if (state.isListening || state.voiceConnecting) Icons.Default.Stop else Icons.Default.Mic,
            if (state.isListening || state.voiceConnecting) "Encerrar voz" else "Conversar por voz",
            tint = Color(0xFFA8E8FF), modifier = Modifier.size(26.dp).align(Alignment.BottomCenter))
    }
}

@Composable
private fun EmptyPulse(state: FluxUiState, onVoice: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FluxOrb(state, 164.dp, if (state.isListening || state.voiceConnecting) onStop else onVoice)
        Spacer(Modifier.height(28.dp))
        Text(
            if (state.isListening) "Estou ouvindo" else "Como posso ajudar agora?",
            fontSize = 28.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.isListening) "Fale normalmente. Toque em parar para encerrar."
            else "${presenceLabel(state)}. Converse por texto ou toque no núcleo para falar.",
            color = Muted,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun ActivationStrip(onActivate: () -> Unit) {
    Surface(
        color = Color(0xFF24150B),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Amber.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PowerSettingsNew, null, tint = Amber)
            Spacer(Modifier.width(10.dp))
            Text("Core online. A IA ainda precisa ser configurada.", Modifier.weight(1f), fontSize = 13.sp)
            TextButton(onClick = onActivate) { Text("VER", color = Amber, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MessageBubble(message: UiMessage) {
    val user = message.role == Role.USER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (user) PulseRedDark else PanelRaised,
            shape = if (user) RoundedCornerShape(20.dp, 5.dp, 20.dp, 20.dp)
            else RoundedCornerShape(5.dp, 20.dp, 20.dp, 20.dp),
            border = if (user) null else BorderStroke(1.dp, Line),
            modifier = Modifier.fillMaxWidth(if (user) 0.86f else 0.94f),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                if (!user) Text("FLUX", color = PulseRed, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                Text(message.content, color = White, lineHeight = 22.sp)
                if (message.mode == "PENDING") {
                    Spacer(Modifier.height(6.dp))
                    Text("SALVA • AGUARDANDO ENVIO", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Surface(color = PanelRaised, shape = RoundedCornerShape(5.dp, 18.dp, 18.dp, 18.dp), border = BorderStroke(1.dp, Line)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("FLUX", color = PulseRed, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Text("Pensando…", color = Muted)
        }
    }
}

@Composable
private fun Composer(
    value: String,
    listening: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onVoice: () -> Unit,
    onStop: () -> Unit,
    onSend: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).testTag("chat-input"),
            enabled = enabled,
            placeholder = { Text("Fale com o FLUX…", color = Muted) },
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PulseRed,
                unfocusedBorderColor = Line,
                focusedContainerColor = Panel,
                unfocusedContainerColor = Panel,
            ),
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = onSend, enabled = enabled) {
                        Icon(Icons.Default.Send, "Enviar", tint = PulseRed)
                    }
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = if (listening) onStop else onVoice,
            modifier = Modifier.size(54.dp).background(if (listening) Color.White else PulseRed, CircleShape),
        ) {
            Icon(
                if (listening) Icons.Default.Stop else Icons.Default.Mic,
                null,
                tint = if (listening) PulseRed else Color.White,
            )
        }
    }
}

@Composable
private fun AgendaScreen(
    state: FluxUiState,
    onAddTask: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddProject: (String, String) -> Unit,
) {
    var task by rememberSaveable { mutableStateOf("") }
    var project by rememberSaveable { mutableStateOf("") }
    var objective by rememberSaveable { mutableStateOf("") }
    ScreenScroll("AGENDA", "Organize hoje e acompanhe seus projetos.") {
        SectionLabel("HOJE")
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseField(task, { task = it }, "Nova tarefa", Modifier.weight(1f), ImeAction.Done) {
                if (task.isNotBlank()) { onAddTask(task); task = "" }
            }
            Spacer(Modifier.width(8.dp))
            RoundAction(Icons.Default.Add, "Adicionar") {
                if (task.isNotBlank()) { onAddTask(task); task = "" }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (state.tasks.isEmpty()) EmptyCard("Nenhuma tarefa por enquanto.")
        state.tasks.forEach { item ->
            Surface(
                color = Panel,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Line),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onToggleTask(item.id) }, Modifier.size(34.dp)) {
                        Icon(
                            if (item.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            null,
                            tint = if (item.completed) Green else Muted,
                        )
                    }
                    Text(item.title, Modifier.weight(1f).padding(horizontal = 8.dp), color = if (item.completed) Muted else White)
                    IconButton(onClick = { onDeleteTask(item.id) }, Modifier.size(34.dp)) {
                        Icon(Icons.Default.DeleteOutline, "Excluir", tint = Muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionLabel("PROJETOS")
        PulseField(project, { project = it }, "Nome do projeto")
        Spacer(Modifier.height(8.dp))
        PulseField(objective, { objective = it }, "Objetivo")
        Spacer(Modifier.height(10.dp))
        PrimaryButton("CRIAR PROJETO", Icons.Default.FolderOpen) {
            if (project.isNotBlank()) {
                onAddProject(project, objective)
                project = ""
                objective = ""
            }
        }
        Spacer(Modifier.height(12.dp))
        state.projects.forEach { item ->
            FeatureCard(Icons.Default.FolderOpen, item.name, item.objective, PulseRed)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DevicesScreen(
    state: FluxUiState,
    onScan: () -> Unit,
    onAssistant: () -> Unit,
    onSpotify: () -> Unit,
    onWhatsApp: () -> Unit,
    onMercadoLivre: () -> Unit,
    onInstagram: () -> Unit,
    onEmail: () -> Unit,
    onCalendar: () -> Unit,
    onCanva: () -> Unit,
    onDrive: () -> Unit,
    onYouTube: () -> Unit,
    onSmartThings: () -> Unit,
) {
    ScreenScroll("DEVICES", "O FLUX acompanha você em cada tela.") {
        SectionLabel("ESTE CELULAR")
        FeatureCard(
            Icons.Default.Android,
            "Assistente do Android",
            if (state.assistantRoleHeld) "Ativo como assistente padrão" else "Toque para definir como assistente padrão",
            if (state.assistantRoleHeld) Green else PulseRed,
            onAssistant,
        )
        Spacer(Modifier.height(10.dp))
        FeatureCard(
            Icons.Default.Headphones,
            "Óculos e fones",
            state.glassesName ?: "Procurar dispositivo Bluetooth de áudio",
            if (state.glassesName != null) Green else PulseRed,
            onScan,
        )
        Spacer(Modifier.height(22.dp))
        SectionLabel("ECOSSISTEMA")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DeviceTile(Icons.Default.Tv, "TV", "Não pareada", Modifier.weight(1f))
            DeviceTile(Icons.Default.Watch, "Relógio", "Não integrado", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DeviceTile(Icons.Default.Computer, "Computador", if (state.coreOnline) "Core acessível" else "Sem conexão", Modifier.weight(1f))
            DeviceTile(Icons.Default.PhoneAndroid, "Celular", "Este aparelho", Modifier.weight(1f))
        }
        Spacer(Modifier.height(22.dp))
        SectionLabel("APLICATIVOS")
        Text("Atalhos para abrir aplicativos. Nenhuma conta é conectada aqui.", color = Muted, fontSize = 12.sp)
        AppRow(Icons.Default.MusicNote, "Spotify", "Abrir aplicativo ou site", onSpotify)
        AppRow(Icons.Default.SmartToy, "WhatsApp", "Abrir rascunho para você editar", onWhatsApp)
        AppRow(Icons.Default.PhotoCamera, "Instagram", "Abrir aplicativo ou site", onInstagram)
        AppRow(Icons.Default.Email, "E-mail", "Preparar novo e-mail", onEmail)
        AppRow(Icons.Default.CalendarMonth, "Agenda", "Criar compromisso para confirmar", onCalendar)
        AppRow(Icons.Default.Palette, "Canva", "Abrir projetos e criações", onCanva)
        AppRow(Icons.Default.Cloud, "Google Drive", "Abrir arquivos", onDrive)
        AppRow(Icons.Default.PlayCircle, "YouTube", "Abrir vídeos e pesquisa", onYouTube)
        AppRow(Icons.Default.Tv, "SmartThings", "Abrir aplicativo ou site; TV ainda não pareada", onSmartThings)
        AppRow(Icons.Default.Storefront, "Mercado Livre", "Abrir aplicativo ou site", onMercadoLivre)
    }
}

@Composable
internal fun LabScreen(state: FluxUiState, onGenerateImage: (String) -> Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var exportBusy by remember { mutableStateOf(false) }
    var exportNotice by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    val saveImage=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val encoded=pendingExport;pendingExport=null
        if(uri!=null&&encoded!=null)scope.launch {
            exportBusy=true
            try { withContext(Dispatchers.IO) {
                val bytes=Base64.decode(encoded,Base64.DEFAULT)
                val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size) ?: error("Imagem inválida")
                try { context.contentResolver.openOutputStream(uri)?.use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))} ?: error("Destino indisponível") } finally {bitmap.recycle()}
            };exportNotice="Imagem salva no local escolhido." }
            catch(e:Exception){if(e is kotlinx.coroutines.CancellationException)throw e;exportNotice="Não foi possível salvar a imagem."}
            finally {exportBusy=false}
        }
    }
    var imagePrompt by rememberSaveable { mutableStateOf("") }
    val generated = remember(state.generatedImageBase64) {
        state.generatedImageBase64?.let { encoded ->
            runCatching {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    ScreenScroll("FLUX STUDIO", "Crie imagens e confira o estado do sistema.") {
        SectionLabel("FLUX STUDIO")
        Text("Crie imagens por descrição usando o gerador seguro do FLUX Core.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        PulseField(imagePrompt, { imagePrompt = it }, "Descreva a imagem que você quer")
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { onGenerateImage(imagePrompt) },
            enabled = imagePrompt.isNotBlank() && !state.imageGenerating,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PulseRed),
        ) {
            if (state.imageGenerating) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(9.dp))
                Text("CRIANDO…", fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(9.dp))
                Text("GERAR IMAGEM", fontWeight = FontWeight.SemiBold)
            }
        }
        generated?.let { image ->
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line), color = Panel) {
                Image(
                    bitmap = image,
                    contentDescription = "Imagem criada pelo FLUX Studio",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        if(generated!=null) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedButton(enabled=!exportBusy,onClick={pendingExport=state.generatedImageBase64;saveImage.launch("FLUX-"+System.currentTimeMillis()+".png")},modifier=Modifier.weight(1f)) { Text("Salvar imagem") }
                OutlinedButton(enabled=!exportBusy,onClick={
                    val encoded=state.generatedImageBase64 ?: return@OutlinedButton
                    scope.launch { exportBusy=true; try {
                        val file=withContext(Dispatchers.IO){
                            val dir=File(context.cacheDir,"creations").apply{mkdirs()}
                            dir.listFiles()?.filter{System.currentTimeMillis()-it.lastModified()>86400000}?.forEach{it.delete()}
                            val bytes=Base64.decode(encoded,Base64.DEFAULT);val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size) ?: error("Imagem inválida")
                            val f=File(dir,"FLUX-"+System.currentTimeMillis()+".png")
                            try{f.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}}finally{bitmap.recycle()};f
                        }
                        val uri=FileProvider.getUriForFile(context,context.packageName+".files",file)
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Compartilhar criação"))
                        exportNotice="Escolha o aplicativo e confirme o compartilhamento."
                    }catch(e:Exception){if(e is kotlinx.coroutines.CancellationException)throw e;exportNotice="Não foi possível preparar o compartilhamento."}finally{exportBusy=false} }
                },modifier=Modifier.weight(1f)) { Text("Compartilhar") }
            }
        }
        exportNotice?.let{Text(it,color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=10.dp))}
        Spacer(Modifier.height(24.dp))
        SectionLabel("DIAGNÓSTICO")
        SystemStatus("FLUX Core", state.coreOnline, if (state.coreOnline) "Diagnóstico respondeu" else "Conecte em Ajustes")
        SystemStatus("Voz Live", state.voiceVerified,
            if (state.voiceVerified) "Sessão aberta neste uso do aplicativo"
            else if (state.voiceConfigured) "Configurada; teste de áudio pendente" else "Ainda não configurada")
        Text("Instalação de atualizações e recuperação não estão disponíveis no aplicativo.",
            color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ControlScreen(
    state: FluxUiState,
    onCoreUrlChange: (String) -> Unit,
    onReconnect: () -> Unit,
    onPairCode: (String) -> Unit,
    onTestVoice: () -> Unit,
    onAssistant: () -> Unit,
    onAppSettings: () -> Unit,
    onNotifications: () -> Unit,
    onMemory: (Boolean) -> Unit,
    onClear: () -> Unit,
    onWakeWord: (Boolean) -> Unit,
    onAccent: (String) -> Unit,
    onVision: () -> Unit,
    onGeminiKeyChange: (String) -> Unit,
    onGeminiKeyClear: () -> Unit,
) {
    var coreUrl by rememberSaveable(state.coreUrl) { mutableStateOf(state.coreUrl) }
    var pairCode by remember { mutableStateOf("") }
    LaunchedEffect(state.coreAuthConfigured, state.isConnecting, state.error) {
        if (state.coreAuthConfigured && !state.isConnecting && state.error == null) pairCode = ""
    }

    ScreenScroll("SISTEMA", "Conexão, inteligência, voz e privacidade.") {
        SectionLabel("STATUS AO VIVO")
        SystemStatus(
            "FLUX Link",
            state.networkAvailable,
            if (state.networkAvailable) "Internet disponível" else "Aguardando a rede voltar",
        )
        SystemStatus(
            "FLUX Core",
            state.coreOnline,
            when {
                state.coreOnline -> "Diagnóstico do Core respondeu"
                !state.coreAuthConfigured -> "Aguardando código de pareamento"
                state.isConnecting -> "Tentando reconectar"
                else -> "Servidor ainda não alcançado"
            },
        )
        SystemStatus("Inteligência", state.aiVerified, when {
            state.aiVerified -> "Resposta recebida neste uso"
            state.aiReady -> "Provedor configurado; teste o chat"
            state.geminiKeyConfigured -> "Chave antiga salva; o Core é a rota principal"
            else -> "Precisa de ativação"
        })
        SystemStatus(
            "Voz FLUX",
            state.voiceVerified,
            if (state.voiceVerified) "Reconhecimento iniciado neste uso; confira o áudio"
            else if (state.voiceConfigured) "Voz FLUX configurada no Core; teste o áudio" else "Precisa de ativação",
        )
        Spacer(Modifier.height(12.dp))
        if (state.coreAuthConfigured) {
            OutlinedAction("TESTAR CONEXÃO", Icons.Default.Refresh, onReconnect)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedAction("INICIAR TESTE DE VOZ", Icons.Default.VolumeUp, onTestVoice)

        Spacer(Modifier.height(22.dp))
        SectionLabel("FLUX LINK")
        Text("Servidor do FLUX Core. Para conectar este aparelho, use um código temporário de pareamento.",
            color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(10.dp))
        PulseField(
            value = coreUrl,
            onValueChange = { coreUrl = it },
            placeholder = "https://seu-core.workers.dev",
        )
        Spacer(Modifier.height(10.dp))
        SystemStatus("Pareamento seguro", state.coreAuthConfigured,
            if (state.coreAuthConfigured) "Credencial protegida pelo Android" else "Aguardando código temporário")
        Spacer(Modifier.height(10.dp))
        PrimaryButton("SALVAR ENDEREÇO", Icons.Default.Link) { onCoreUrlChange(coreUrl) }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pairCode,
            onValueChange = { pairCode = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (state.coreAuthConfigured) "Novo código para reparar" else "Cole o código temporário", color = Muted) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(10.dp))
        PrimaryButton(if (state.coreAuthConfigured) "PAREAR NOVAMENTE" else "PAREAR APARELHO", Icons.Default.Link) {
            if (!state.isConnecting) onPairCode(pairCode)
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("INTELIGÊNCIA E VOZ")
        Text(
            "Esta avaliação usa Cloudflare Workers AI para texto e Vision e mantém a voz FLUX 4 na Inworld, que exige créditos. Esta versão de avaliação usa áudio WebRTC simultâneo. Interrupção por fala e cancelamento de eco precisam ser validados neste aparelho.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Spacer(Modifier.height(10.dp))
        SystemStatus("Agenda do Android", state.calendarHeadline != "Agenda não autorizada", state.calendarHeadline)
        SystemStatus("Previsão do tempo", state.weatherHeadline != "Clima não consultado", state.weatherHeadline)
        Text("Ao pedir o panorama, o Android solicita acesso à agenda e à localização aproximada. Os próximos eventos são sincronizados com o site pareado.",
            color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        if (state.geminiKeyConfigured) {
            Spacer(Modifier.height(8.dp))
            OutlinedAction("REMOVER CHAVE ANTIGA DO GEMINI", Icons.Default.DeleteOutline, onGeminiKeyClear, danger = true)
        }
        Spacer(Modifier.height(22.dp))
        SectionLabel("ASSISTENTE")
        FeatureCard(
            Icons.Default.Android,
            "Assistente padrão",
            if (state.assistantRoleHeld) "Ativado no Android" else "Configurar agora",
            if (state.assistantRoleHeld) Green else PulseRed,
            onAssistant,
        )
        Spacer(Modifier.height(8.dp))
        ToggleCard(
            Icons.Default.RecordVoiceOver,
            "Ativação mãos-livres por “Flux”",
            "Usa o reconhecimento local do Android quando disponível",
            state.wakeWordEnabled,
            onWakeWord,
        )
        Spacer(Modifier.height(8.dp))
        FeatureCard(
            Icons.Default.CenterFocusStrong,
            "FLUX Vision",
            "Testar painel sobre a tela atual",
            PulseRed,
            onVision,
        )
        Spacer(Modifier.height(8.dp))
        FeatureCard(Icons.Default.Notifications, "Notificações", "Abrir permissões do sistema", PulseRed, onNotifications)
        Spacer(Modifier.height(8.dp))
        FeatureCard(Icons.Default.Security, "Permissões do aplicativo", "Microfone, Bluetooth e energia", PulseRed, onAppSettings)
        Spacer(Modifier.height(22.dp))
        SectionLabel("COMPORTAMENTO")
        ToggleCard(Icons.Default.Memory, "Histórico nas respostas",
            "Usar mensagens anteriores no chat; desligar inicia um contexto novo", state.memoryEnabled, onMemory)
        Spacer(Modifier.height(22.dp))
        SectionLabel("APARÊNCIA")
        AccentSelector(state.accentKey, onAccent)
        Spacer(Modifier.height(22.dp))
        SectionLabel("DADOS LOCAIS")
        OutlinedAction("LIMPAR CONVERSA", Icons.Default.ClearAll, onClear, danger = true)
        if (state.pendingMessageCount > 0) {
            Spacer(Modifier.height(10.dp))
            Text("Mensagens salvas aguardando a IA: " + state.pendingMessageCount, color = Amber, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ScreenScroll(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        Text(subtitle, color = Muted, lineHeight = 20.sp)
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SystemStatus(name: String, online: Boolean, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(if (online) Green else PulseRed, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold)
            Text(detail, color = Muted, fontSize = 12.sp)
        }
        Text(if (online) "OK" else "PENDENTE", color = if (online) Green else PulseRed, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, subtitle: String, accent: Color, onClick: (() -> Unit)? = null) {
    Surface(
        color = Panel,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DeviceTile(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line), modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = PulseRed, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(20.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AppRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = PulseRed)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
        Icon(Icons.Default.OpenInNew, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ToggleCard(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PulseRed)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PulseRed),
            )
        }
    }
}

@Composable
private fun AccentSelector(selected: String, onSelect: (String) -> Unit) {
    val accents = listOf(
        "blue" to Color(0xFF17B8FF),
        "cyan" to Color(0xFF00D9F5),
        "purple" to Color(0xFF8D73FF),
        "gold" to Color(0xFFFFB840),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        accents.forEach { (key, color) ->
            Box(
                Modifier.size(52.dp).testTag("accent-" + key)
                    .clip(CircleShape)
                    .background(color)
                    .border(if (selected == key) 4.dp else 1.dp, if (selected == key) White else Line, CircleShape)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected == key) Icon(Icons.Default.Check, "Cor selecionada", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PulseField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, color = Muted) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PulseRed,
            unfocusedBorderColor = Line,
            focusedContainerColor = Panel,
            unfocusedContainerColor = Panel,
        ),
    )
}

@Composable
private fun RoundAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(54.dp).background(PulseRed, RoundedCornerShape(16.dp))) {
        Icon(icon, description, tint = Color.White)
    }
}

@Composable
private fun PrimaryButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PulseRed),
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(9.dp))
        Text(label, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
    }
}

@Composable
private fun OutlinedAction(label: String, icon: ImageVector, onClick: () -> Unit, danger: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (danger) PulseRed.copy(alpha = 0.65f) else Line),
    ) {
        Icon(icon, null, tint = if (danger) PulseRed else White)
        Spacer(Modifier.width(9.dp))
        Text(label, color = if (danger) PulseRed else White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = PulseRed, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun EmptyCard(message: String) {
    Surface(color = Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line), modifier = Modifier.fillMaxWidth()) {
        Text(message, Modifier.padding(16.dp), color = Muted)
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        color = Color(0xFF301016),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PulseRed.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = White, fontSize = 12.sp, lineHeight = 17.sp)
            IconButton(onClick = onDismiss, Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Fechar", tint = Muted)
            }
        }
    }
}

@Composable
private fun AssistantDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelRaised,
        icon = { Icon(Icons.Default.Android, null, tint = PulseRed) },
        title = { Text("Ativar como assistente?") },
        text = {
            Text(
                "Assim você abre o FLUX pelo atalho do assistente do Android e conversa por voz mais rápido.",
                color = Muted,
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = PulseRed)) {
                Text("ATIVAR")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("AGORA NÃO", color = Muted) } },
    )
}
