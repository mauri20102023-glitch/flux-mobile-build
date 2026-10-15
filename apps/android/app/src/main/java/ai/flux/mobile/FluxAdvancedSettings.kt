package ai.flux.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.flux.mobile.model.FluxUiState

@Composable
fun FluxAdvancedSettings(state: FluxUiState, onAdvanced: (Boolean)->Unit, onMute: (Boolean)->Unit) {
    var preview by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("FLUX Advanced",style=MaterialTheme.typography.titleLarge)
        Text("A mesma inteligência em um espaço expandido. Ativar mantém a conversa e as permissões atuais. Nenhum aparelho é conectado automaticamente.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick={onAdvanced(!state.advancedEnabled)}) { Text(if(state.advancedEnabled) "Sair do Advanced" else "Iniciar Advanced") }
        OutlinedButton(onClick={onMute(!state.microphoneMuted)}) { Text(if(state.microphoneMuted) "Reativar microfone do FLUX" else "Silenciar microfone do FLUX") }
        Text(if(state.microphoneMuted) "Captação do FLUX desativada. Use este botão para reativar." else "Reativar o microfone não inicia captura nem ativa a palavra Flux.",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick={preview=true}) { Text("Prévia do painel inferior") }
    }
    if(preview) AlertDialog(onDismissRequest={preview=false},title={Text("Painel inferior · prévia local")},text={Text("O painel fixo reúne conversa, dispositivos, estudos e cartões sob demanda. Esta é uma prévia neste celular; não há monitor externo conectado nem transferência remota comprovada.")},confirmButton={TextButton(onClick={preview=false}){Text("Voltar ao painel")}})
}
