package ai.flux.mobile

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private val PulseRed: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
private val PulseRedDark: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
private val Ink = Color(0xFF07080A)
private val Panel = Color(0xFF111318)
private val PanelRaised = Color(0xFF191C22)
private val Line = Color(0xFF30343D)
private val White = Color(0xFFF4F6FA)
private val Muted = Color(0xFF9BA2AD)
private val Green = Color(0xFF52D27F)
private val Amber = Color(0xFFFFBC52)

private enum class FluxTab(val label: String, val icon: ImageVector) {
    CHAT("FLUX Chat", Icons.Default.AutoAwesome),
    AGENDA("Projetos", Icons.Default.CalendarMonth),
    DEVICES("Dispositivos", Icons.Default.Devices),
    LAB("Laboratório", Icons.Default.Build),
    CONTROL("Sistema", Icons.Default.Settings),
}

@Composable
fun FluxTheme(accentKey: String = "red", content: @Composable () -> Unit) {
    val (accent, accentDark) = when (accentKey) {
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
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
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
    onProactivityEnabled: (Boolean) -> Unit,
    onClearConversation: () -> Unit,
    onClearError: () -> Unit,
) {
    var selectedName by rememberSaveable { mutableStateOf(FluxTab.CHAT.name) }
    val selected = FluxTab.valueOf(selectedName)

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { PulseNavigation(selected) { selectedName = it.name } },
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF14161B), Ink, Color(0xFF050608))))
                .padding(padding),
        ) {
            when (selected) {
                FluxTab.CHAT -> PulseChat(state, onSend, onVoice, onStop) {
                    selectedName = FluxTab.CONTROL.name
                }
                FluxTab.AGENDA -> AgendaScreen(state, onAddTask, onToggleTask, onDeleteTask, onAddProject)
                FluxTab.DEVICES -> DevicesScreen(
                    state, onScanDevices, onAssistantSetup, onOpenSpotify, onOpenWhatsApp, onOpenMercadoLivre,
                    onOpenInstagram, onOpenEmail, onOpenCalendar, onOpenCanva, onOpenDrive, onOpenYouTube,
                    onOpenSmartThings,
                )
                FluxTab.LAB -> LabScreen(state, onGenerateImage)
                FluxTab.CONTROL -> ControlScreen(
                    state, onCoreUrlChange, onCoreTest, onPairCode, onTestVoice,
                    onAssistantSetup, onAppSettings, onNotificationSettings, onMemoryEnabled,
                    onProactivityEnabled, onClearConversation, onWakeWordEnabled, onAccentChange, onOpenVision,
                    onGeminiKeyChange, onGeminiKeyClear,
                )
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
private fun PulseNavigation(selected: FluxTab, onSelect: (FluxTab) -> Unit) {
    NavigationBar(containerColor = Color(0xF20A0A0D), tonalElevation = 0.dp) {
        FluxTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, tab.label, Modifier.size(22.dp)) },
                label = { Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = PulseRedDark,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                ),
            )
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
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focus = LocalFocusManager.current
    LaunchedEffect(state.messages.size, state.isResponding) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        PulseHeader(state)
        if (state.activationRequired && state.coreOnline) ActivationStrip(onActivate)
        if (state.messages.isEmpty()) {
            EmptyPulse(state.isListening, onVoice, Modifier.weight(1f))
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
            listening = state.isListening,
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
        ) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(23.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Olá, Maurício", fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text("FLUX CHAT • MOBILE 1.7.3", fontSize = 10.sp, color = Muted, letterSpacing = 1.2.sp)
        }
        StatusPill(
            label = when {
                !state.networkAvailable -> "RECONEXÃO"
                state.isConnecting -> "CONECTANDO"
                state.coreOnline && state.aiReady -> "ONLINE"
                state.coreOnline -> "CORE ONLINE"
                state.geminiKeyConfigured -> "GEMINI PESSOAL"
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
private fun EmptyPulse(listening: Boolean, onVoice: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(if (listening) 188.dp else 164.dp).clip(CircleShape)
                    .background(PulseRed.copy(alpha = if (listening) 0.18f else 0.08f)),
            )
            Box(
                Modifier.size(126.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(PulseRed, PulseRedDark, Color(0xFF31070D))))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    .clickable(onClick = onVoice),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Mic, "Falar com o FLUX", tint = Color.White, modifier = Modifier.size(44.dp)) }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            if (listening) "Estou ouvindo" else "Como posso ajudar agora?",
            fontSize = 28.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (listening) "Fale normalmente e termine a frase."
            else "Converse por texto ou voz. O contexto continua entre celular, TV, computador e EDITH.",
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
            Text("FLUX", color = PulseRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
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
            modifier = Modifier.weight(1f),
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
            DeviceTile(Icons.Default.Tv, "TV", "SmartThings", Modifier.weight(1f))
            DeviceTile(Icons.Default.Watch, "Relógio", "Wear OS", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DeviceTile(Icons.Default.Computer, "Computador", "FLUX Core", Modifier.weight(1f))
            DeviceTile(Icons.Default.PhoneAndroid, "Celular", "Android", Modifier.weight(1f))
        }
        Spacer(Modifier.height(22.dp))
        SectionLabel("APLICATIVOS")
        AppRow(Icons.Default.MusicNote, "Spotify", "Música e controles", onSpotify)
        AppRow(Icons.Default.SmartToy, "WhatsApp", "Preparar mensagem para você confirmar", onWhatsApp)
        AppRow(Icons.Default.PhotoCamera, "Instagram", "Abrir publicação e mensagens", onInstagram)
        AppRow(Icons.Default.Email, "E-mail", "Preparar novo e-mail", onEmail)
        AppRow(Icons.Default.CalendarMonth, "Agenda", "Criar compromisso para confirmar", onCalendar)
        AppRow(Icons.Default.Palette, "Canva", "Abrir projetos e criações", onCanva)
        AppRow(Icons.Default.Cloud, "Google Drive", "Abrir arquivos", onDrive)
        AppRow(Icons.Default.PlayCircle, "YouTube", "Abrir vídeos e pesquisa", onYouTube)
        AppRow(Icons.Default.Tv, "Samsung Crystal", "Parear e controlar pelo SmartThings", onSmartThings)
        AppRow(Icons.Default.Storefront, "Mercado Livre", "Pesquisa e compras", onMercadoLivre)
    }
}

@Composable
private fun LabScreen(state: FluxUiState, onGenerateImage: (String) -> Unit) {
    var imagePrompt by rememberSaveable { mutableStateOf("") }
    val generated = remember(state.generatedImageBase64) {
        state.generatedImageBase64?.let { encoded ->
            runCatching {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    ScreenScroll("FLUX LAB", "Criação controlada, testes e recuperação antes de qualquer atualização.") {
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
                Text("CRIANDO…", fontWeight = FontWeight.Black)
            } else {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(9.dp))
                Text("GERAR IMAGEM", fontWeight = FontWeight.Black)
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
        Spacer(Modifier.height(24.dp))
        SectionLabel("DESENVOLVIMENTO SEGURO")
        FeatureCard(Icons.Default.Build, "FLUX Builder", "Prepara novas funções em um projeto separado. Nunca instala sozinho.", PulseRed)
        Spacer(Modifier.height(9.dp))
        FeatureCard(Icons.Default.Science, "FLUX Test Lab", "Executa testes isolados antes de liberar uma versão.", Color(0xFF00D9F5))
        Spacer(Modifier.height(9.dp))
        FeatureCard(Icons.Default.Security, "FLUX Security", "Revisa permissões, segredos e ações de risco.", Green)
        Spacer(Modifier.height(9.dp))
        FeatureCard(Icons.Default.SystemUpdate, "FLUX Update", "Só instala pacotes assinados após sua confirmação.", Amber)
        Spacer(Modifier.height(9.dp))
        FeatureCard(Icons.Default.Restore, "FLUX Recovery", "Mantém uma versão anterior pronta para desfazer falhas.", Color(0xFFFF7D88))
        Spacer(Modifier.height(9.dp))
        FeatureCard(Icons.Default.Code, "FLUX Developer Mode", "Exibe código, testes e registros na TV ou no computador.", Color(0xFF8D73FF))
        Spacer(Modifier.height(18.dp))
        Surface(
            color = Color(0xFF102218),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Green.copy(alpha = 0.5f)),
        ) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, null, tint = Green)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Regra central: criar, testar, revisar, pedir sua aprovação e só então atualizar.",
                    color = White,
                    lineHeight = 20.sp,
                )
            }
        }
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
    onProactivity: (Boolean) -> Unit,
    onClear: () -> Unit,
    onWakeWord: (Boolean) -> Unit,
    onAccent: (String) -> Unit,
    onVision: () -> Unit,
    onGeminiKeyChange: (String) -> Unit,
    onGeminiKeyClear: () -> Unit,
) {
    var coreUrl by rememberSaveable(state.coreUrl) { mutableStateOf(state.coreUrl) }
    var pairCode by rememberSaveable { mutableStateOf("") }
    var geminiKey by rememberSaveable { mutableStateOf("") }
    var showGeminiKey by rememberSaveable { mutableStateOf(false) }

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
                state.coreOnline -> "Conexão persistente ativa"
                !state.corePairingAvailable && !state.coreAuthConfigured -> "Aguardando código de pareamento"
                state.isConnecting -> "Tentando reconectar"
                else -> "Servidor ainda não alcançado"
            },
        )
        SystemStatus("Inteligência", state.aiReady || state.geminiKeyConfigured, when {
            state.aiReady -> "IA pronta para responder"
            state.geminiKeyConfigured -> "Gemini pessoal configurado • teste o chat"
            else -> "Precisa de ativação"
        })
        SystemStatus(
            "FLUX LIVE",
            state.voiceConfigured && state.voiceOfficial,
            if (state.voiceConfigured) state.voiceProvider else "Precisa de ativação",
        )
        Spacer(Modifier.height(12.dp))
        if (state.corePairingAvailable || state.coreAuthConfigured) {
            OutlinedAction("TESTAR CONEXÃO", Icons.Default.Refresh, onReconnect)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedAction("TESTAR GEMINI LIVE", Icons.Default.VolumeUp, onTestVoice)

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
        if (!state.coreAuthConfigured) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = pairCode,
                onValueChange = { pairCode = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cole o código temporário", color = Muted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton("PAREAR APARELHO", Icons.Default.Link) { onPairCode(pairCode) }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("GEMINI LIVE PESSOAL")
        Text(
            "Cole a chave uma única vez. Ela fica criptografada pelo Android Keystore e não entra no APK, no GitHub ou no histórico do FLUX.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Spacer(Modifier.height(10.dp))
        SystemStatus(
            "Chave do Gemini",
            state.geminiKeyConfigured,
            if (state.geminiKeyConfigured) "Protegida neste aparelho" else "Ainda não configurada",
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = geminiKey,
            onValueChange = { geminiKey = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    if (state.geminiKeyConfigured) "Digite uma nova chave para substituir" else "Cole sua chave do Gemini",
                    color = Muted,
                )
            },
            singleLine = true,
            visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                    Icon(if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar ou ocultar chave")
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PulseRed,
                unfocusedBorderColor = Line,
                focusedContainerColor = Panel,
                unfocusedContainerColor = Panel,
            ),
        )
        Spacer(Modifier.height(10.dp))
        PrimaryButton(
            if (state.geminiKeyConfigured) "SUBSTITUIR CHAVE PROTEGIDA" else "PROTEGER E ATIVAR GEMINI",
            Icons.Default.Key,
        ) {
            onGeminiKeyChange(geminiKey)
            geminiKey = ""
            showGeminiKey = false
        }
        if (state.geminiKeyConfigured) {
            Spacer(Modifier.height(8.dp))
            OutlinedAction("REMOVER CHAVE DESTE APARELHO", Icons.Default.DeleteOutline, onGeminiKeyClear, danger = true)
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
            "Modo principal: diga “Flux” ou “Flux, veja minha tela” sem tocar",
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
        ToggleCard(Icons.Default.Memory, "Memória", "Manter o contexto das conversas", state.memoryEnabled, onMemory)
        Spacer(Modifier.height(8.dp))
        ToggleCard(Icons.Default.AutoAwesome, "Proatividade", "Sugerir próximos passos sem exagerar", state.moderateProactivity, onProactivity)
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
        Text(if (online) "OK" else "AÇÃO", color = if (online) Green else PulseRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
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
        "red" to Color(0xFFFF304A),
        "cyan" to Color(0xFF00D9F5),
        "purple" to Color(0xFF8D73FF),
        "gold" to Color(0xFFFFB840),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        accents.forEach { (key, color) ->
            Box(
                Modifier.size(52.dp)
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
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
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
