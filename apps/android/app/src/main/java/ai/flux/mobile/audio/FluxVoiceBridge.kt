package ai.flux.mobile.audio

/**
 * Contrato sem dependências externas usado pela tela principal.
 *
 * A implementação Gemini Live é descoberta apenas quando o usuário toca no
 * microfone. Isso mantém o caminho de inicialização leve e recuperável.
 */
interface FluxVoiceBridge {
    fun startSession()
    fun sendUserMessage(message: String)
    fun sendContextualUpdate(context: String)
    fun endSession()
    fun destroy()
}
