package ai.flux.mobile.audio

import android.graphics.Bitmap

/**
 * Contrato sem dependências externas usado pela tela principal.
 *
 * A implementação Gemini Live é descoberta apenas quando o usuário toca no
 * microfone. Isso mantém o caminho de inicialização leve e recuperável.
 */
interface FluxVoiceBridge {
    fun setAudioLevelListener(listener: (Float)->Unit) {}
    fun startSession()
    fun sendUserMessage(message: String)
    fun sendContextualUpdate(context: String)
    fun sendScreenFrame(frame: Bitmap, prompt: String)
    fun endSession()
    fun destroy()
}
