package ai.flux.mobile.assistant

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ai.flux.mobile.audio.FluxVoiceBridge
import ai.flux.mobile.audio.FluxVoiceController

/** Native bottom-sheet assistant, similar to Android's contextual assistant surface. */
class FluxVoiceInteractionSession(
    context: Context,
    private val launchArgs: Bundle?,
) : VoiceInteractionSession(context) {
    private lateinit var orb: FluxEnergyOrbView
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var prompt: EditText
    private lateinit var screenshotView: ImageView
    private var controller: FluxVoiceBridge? = null
    private var visibleText = ""
    private var screenshotReceived = false
    private var visionSummaryRequested = false
    private var shown = false
    private val visionRequested: Boolean
        get() = launchArgs?.getBoolean(FluxVoiceInteractionService.ARG_VISION, false) == true

    override fun onCreate() {
        super.onCreate()
        setContentView(buildView())
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        shown = true
        FluxWakeWordService.pauseForConversation()
        status.text = if (visionRequested) "Lendo a tela…" else "Conectando ao FLUX Live…"
        voice().startSession()
    }

    override fun onHandleAssist(data: Bundle?, structure: AssistStructure?, content: AssistContent?) {
        super.onHandleAssist(data, structure, content)
        visibleText = extractVisibleText(structure).take(6_000)
        val uri = content?.webUri?.toString().orEmpty()
        val contextText = buildString {
            append("Contexto da tela atual do Android. ")
            if (uri.isNotBlank()) append("Endereço: $uri. ")
            if (visibleText.isNotBlank()) append("Texto visível: $visibleText")
        }
        if (contextText.length > 40) voice().sendContextualUpdate(contextText)
        updateVisionState()
        // O Android pode entregar o texto antes da captura. Aguarde um instante
        // para priorizar a imagem e use o texto quando ela não estiver disponível.
        if (visionRequested && visibleText.isNotBlank()) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (shown && !screenshotReceived && !visionSummaryRequested) {
                    visionSummaryRequested = true
                    voice().sendUserMessage("Descreva brevemente o texto visível desta tela e espere minha pergunta.")
                }
            }, 1_200L)
        }
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        screenshotReceived = screenshot != null
        if (screenshot != null) {
            screenshotView.setImageBitmap(screenshot)
            screenshotView.visibility = View.VISIBLE
            if (visionRequested) {
                visionSummaryRequested = true
                voice().sendScreenFrame(
                    screenshot,
                    "Observe a captura da tela que enviei. Descreva em uma frase o que aparece e espere minha pergunta.",
                )
            }
        }
        updateVisionState()
    }

    override fun onHide() {
        shown = false
        controller?.endSession()
        FluxWakeWordService.resumeAfterConversation()
        super.onHide()
    }

    override fun onDestroy() {
        controller?.destroy()
        controller = null
        FluxWakeWordService.resumeAfterConversation()
        super.onDestroy()
    }

    private fun voice(): FluxVoiceBridge = controller ?: FluxVoiceController(
        context = context,
        onSessionChanged = { connected ->
            context.mainExecutor.execute {
                orb.setActive(connected)
                if (connected) FluxWakeWordService.pauseForConversation()
                else FluxWakeWordService.resumeAfterConversation()
                status.text = if (connected) "FLUX ouvindo" else "Diga ‘Flux’ para tentar de novo"
            }
        },
        onUserTranscript = { text -> context.mainExecutor.execute { transcript.text = "Você: $text" } },
        onAgentResponse = { text ->
            context.mainExecutor.execute {
                transcript.text = text
                status.text = "FLUX LIVE"
            }
        },
        onError = { message ->
            context.mainExecutor.execute {
                status.text = "Falha de conexão"
                transcript.text = message
            }
        },
    ).also { controller = it }

    private fun updateVisionState() {
        if (!visionRequested) return
        status.text = when {
            screenshotReceived -> "Imagem capturada • contexto visual disponível"
            visibleText.isNotBlank() -> "Texto da tela recebido • pode perguntar"
            else -> "Aguardando conteúdo da tela…"
        }
    }

    private fun buildView(): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(35, 0, 0, 0))
            setOnClickListener { hide() }
        }
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(18))
            background = rounded(Color.rgb(10, 11, 15), 30f, Color.argb(150, 255, 255, 255))
            isClickable = true
        }
        root.addView(panel, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        ).apply { setMargins(dp(10), dp(10), dp(10), dp(18)) })

        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        orb = FluxEnergyOrbView(context)
        top.addView(orb, LinearLayout.LayoutParams(dp(64), dp(64)))
        val titleBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        titleBox.addView(label("FLUX", 22f, Color.WHITE, Typeface.BOLD))
        status = label("Preparando…", 12f, Color.rgb(164, 170, 181), Typeface.NORMAL)
        titleBox.addView(status)
        top.addView(titleBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val close = label("×", 30f, Color.WHITE, Typeface.NORMAL).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(30, 32, 39), 25f)
            setOnClickListener { hide() }
        }
        top.addView(close, LinearLayout.LayoutParams(dp(48), dp(48)))
        panel.addView(top)

        screenshotView = ImageView(context).apply {
            visibility = View.GONE
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(22, 24, 30), 18f)
        }
        panel.addView(screenshotView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(118),
        ).apply { topMargin = dp(8) })

        transcript = label(
            if (visionRequested) "Preparando o contexto da tela…" else "Pode falar, Maurício.",
            15f,
            Color.rgb(234, 237, 243),
            Typeface.NORMAL,
        ).apply { setPadding(dp(4), dp(12), dp(4), dp(12)); maxLines = 4 }
        panel.addView(transcript)

        val inputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        prompt = EditText(context).apply {
            hint = "Perguntar ao FLUX"
            setHintTextColor(Color.rgb(150, 155, 166))
            setTextColor(Color.WHITE)
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(16), 0, dp(10), 0)
            background = rounded(Color.rgb(30, 32, 39), 24f)
            setOnEditorActionListener { _, _, _ -> sendPrompt(); true }
        }
        inputRow.addView(prompt, LinearLayout.LayoutParams(0, dp(52), 1f))
        val send = label("➤", 22f, Color.WHITE, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            background = rounded(accentColor(), 27f)
            setOnClickListener { sendPrompt() }
        }
        inputRow.addView(send, LinearLayout.LayoutParams(dp(54), dp(54)).apply { marginStart = dp(8) })
        panel.addView(inputRow)

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.addView(action("Perguntar") {
            prompt.requestFocus()
            context.getSystemService(InputMethodManager::class.java)
                .showSoftInput(prompt, InputMethodManager.SHOW_IMPLICIT)
        }, weighted())
        actions.addView(action("Resumir") {
            if (!screenshotReceived && visibleText.isBlank()) transcript.text = "Esta tela não forneceu conteúdo ao Android."
            else voice().sendUserMessage("Resuma a tela atual em até três frases.")
        }, weighted())
        actions.addView(action("Traduzir") {
            if (!screenshotReceived && visibleText.isBlank()) transcript.text = "Esta tela não forneceu texto legível ao Android."
            else voice().sendUserMessage("Traduza para português brasileiro o texto visível nesta tela.")
        }, weighted())
        panel.addView(actions, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(10) })
        return root
    }

    private fun sendPrompt() {
        val message = prompt.text.toString().trim()
        if (message.isEmpty()) return
        transcript.text = "Você: $message"
        prompt.text.clear()
        voice().sendUserMessage(message)
    }

    private fun extractVisibleText(structure: AssistStructure?): String {
        if (structure == null) return ""
        val result = LinkedHashSet<String>()
        fun walk(node: AssistStructure.ViewNode) {
            node.text?.toString()?.trim()?.takeIf(String::isNotBlank)?.let(result::add)
            node.contentDescription?.toString()?.trim()?.takeIf(String::isNotBlank)?.let(result::add)
            for (index in 0 until node.childCount) walk(node.getChildAt(index))
        }
        for (window in 0 until structure.windowNodeCount) walk(structure.getWindowNodeAt(window).rootViewNode)
        return result.joinToString(" • ")
    }

    private fun label(text: String, size: Float, color: Int, style: Int) = TextView(context).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        setTypeface(Typeface.DEFAULT, style)
    }

    private fun action(text: String, click: () -> Unit) = TextView(context).apply {
        this.text = text
        textSize = 13f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setOnClickListener { click() }
        background = rounded(Color.rgb(25, 27, 33), 20f)
    }

    private fun weighted() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
        setMargins(dp(3), 0, dp(3), 0)
    }

    private fun rounded(color: Int, radius: Float, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius.toInt()).toFloat()
        stroke?.let { setStroke(1, it) }
    }

    private fun accentColor(): Int = when (
        context.getSharedPreferences("flux_workspace", Context.MODE_PRIVATE).getString("accent_key", "red")
    ) {
        "cyan" -> Color.rgb(0, 229, 255)
        "purple" -> Color.rgb(123, 97, 255)
        "gold" -> Color.rgb(255, 184, 64)
        else -> Color.rgb(255, 48, 74)
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
