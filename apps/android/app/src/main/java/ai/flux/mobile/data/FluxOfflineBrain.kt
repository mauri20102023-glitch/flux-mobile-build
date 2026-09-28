package ai.flux.mobile.data

import ai.flux.mobile.model.LocalProject
import ai.flux.mobile.model.LocalTask
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class OfflineBrainResult(
    val content: String,
    val handledLocally: Boolean = false,
    val taskToAdd: String? = null,
    val action: OfflineAction? = null,
)

enum class OfflineAction {
    OPEN_SPOTIFY,
    SPOTIFY_PLAY_PAUSE,
    SPOTIFY_NEXT,
    OPEN_WHATSAPP,
    OPEN_MERCADO_LIVRE,
    OPEN_SMARTTHINGS,
}

/**
 * Always-available mobile layer. It handles private local data and common
 * assistant commands without a server. Open-ended reasoning is queued for the
 * real local model on the user's computer instead of being faked.
 */
class FluxOfflineBrain {
    fun respond(
        input: String,
        tasks: List<LocalTask>,
        projects: List<LocalProject>,
    ): OfflineBrainResult {
        val message = input.trim()
        val normalized = message.lowercase(Locale.forLanguageTag("pt-BR"))
        val pendingTasks = tasks.filterNot(LocalTask::completed)

        if (Regex("\\b(abra|abrir|inicie|iniciar)\\b.*\\bspotify\\b").containsMatchIn(normalized)) {
            return OfflineBrainResult("Certo, Maurício. Abrindo o Spotify.", true, action = OfflineAction.OPEN_SPOTIFY)
        }
        if (Regex("\\b(próxima|proxima|pule|avançar|avancar)\\b.*\\b(música|musica|faixa)?\\b").containsMatchIn(normalized)) {
            return OfflineBrainResult("Certo, Maurício. Avançando a reprodução.", true, action = OfflineAction.SPOTIFY_NEXT)
        }
        if (Regex("\\b(pausar|pause|continuar|reproduzir|play)\\b.*\\b(música|musica|spotify)?\\b").containsMatchIn(normalized)) {
            return OfflineBrainResult("Certo, Maurício. Ajustando a reprodução.", true, action = OfflineAction.SPOTIFY_PLAY_PAUSE)
        }
        if (Regex("\\b(abra|abrir|mensagem|whatsapp)\\b.*\\bwhatsapp\\b").containsMatchIn(normalized)) {
            return OfflineBrainResult("Maurício, preparei o WhatsApp. O envio continua dependendo da sua confirmação.", true, action = OfflineAction.OPEN_WHATSAPP)
        }
        if (Regex("\\b(abra|abrir|vendas?|anúncios?|anuncios?)\\b.*\\bmercado livre\\b").containsMatchIn(normalized)) {
            return OfflineBrainResult("Certo, Maurício. Abrindo sua área do Mercado Livre.", true, action = OfflineAction.OPEN_MERCADO_LIVRE)
        }
        if (Regex("\\b(abra|abrir|tv|televisão|televisao|smartthings)\\b.*\\b(tv|televisão|televisao|smartthings)\\b").containsMatchIn(normalized)) {
            return OfflineBrainResult("Certo, Maurício. Abrindo o controle da TV pelo SmartThings.", true, action = OfflineAction.OPEN_SMARTTHINGS)
        }
        if (Regex("^(?:flux[, ]*)?(?:liste|listar|mostre|mostrar|quais são|quais sao)? ?(?:minhas )?(?:tarefas|pendências|pendencias|agenda)(?: de hoje)?[?.! ]*$|^o que tenho hoje[?.! ]*$").containsMatchIn(normalized)) {
            val summary = if (pendingTasks.isEmpty()) {
                "Você não tem tarefas locais pendentes."
            } else {
                pendingTasks.take(8).joinToString(prefix = "Suas tarefas pendentes são: ", separator = "; ") { it.title } + "."
            }
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            return OfflineBrainResult("Maurício, hoje é $today. $summary", handledLocally = true)
        }
        if (Regex("^(?:flux[, ]*)?(?:liste|listar|mostre|mostrar|quais são|quais sao) (?:meus )?projetos[?.! ]*$").containsMatchIn(normalized)) {
            val summary = if (projects.isEmpty()) "Nenhum projeto local foi cadastrado." else projects
                .take(8)
                .joinToString(prefix = "Projetos locais: ", separator = "; ") { "${it.name} — ${it.objective}" } + "."
            return OfflineBrainResult("Maurício, $summary", handledLocally = true)
        }
        val task = Regex("^(?:flux[, ]*)?(?:adicione|adicionar|crie|criar|anote|anotar)(?: uma)? tarefa(?: para)?[: ]+(.+)$")
            .find(normalized)?.groupValues?.getOrNull(1)?.trim()
        if (!task.isNullOrBlank()) {
            return OfflineBrainResult(
                content = "Certo, Maurício. Adicionei a tarefa local: $task.",
                handledLocally = true,
                taskToAdd = task.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pt-BR")) else it.toString() },
            )
        }
        return OfflineBrainResult(
            content = "Reconectando à inteligência do Flux… seu pedido ficou salvo e será enviado automaticamente.",
        )
    }
}
