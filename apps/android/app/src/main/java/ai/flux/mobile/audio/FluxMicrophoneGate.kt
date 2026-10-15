package ai.flux.mobile.audio

/** Process-local capture owners. Privacy mute closes main and assistant-overlay sessions. */
object FluxMicrophoneGate {
    private val owners = mutableSetOf<() -> Unit>()
    @Synchronized fun register(close: () -> Unit) { owners.add(close) }
    @Synchronized fun unregister(close: () -> Unit) { owners.remove(close) }
    fun closeAll() {
        val closing = synchronized(this) { owners.toList().also { owners.clear() } }
        closing.forEach { runCatching { it() } }
    }
}
