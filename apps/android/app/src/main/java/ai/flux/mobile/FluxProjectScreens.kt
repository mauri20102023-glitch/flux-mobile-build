package ai.flux.mobile

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.flux.mobile.model.FluxUiState
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.sin

@Composable
fun FluxVisionPage(state: FluxUiState, onCapture:()->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
        Text("FLUX Vision",style=MaterialTheme.typography.headlineMedium)
        Text("Analise conteúdo real, com sua autorização.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick=onCapture){Text("Capturar tela com autorização")}
        Text("Uma captura por vez. O Android solicita consentimento; você pode encerrar pela notificação de captura. Não é vídeo contínuo.",fontSize=12.sp)
        FluxAnalysisScreen()
    }
}

@Composable
fun FluxControlPage(state:FluxUiState,onSettings:()->Unit,onWorkspace:()->Unit,onDevices:()->Unit,onPlans:()->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("FLUX Control",style=MaterialTheme.typography.headlineMedium)
        Text("Seu ecossistema, sob controle.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp)) {
            Text(if(state.coreOnline&&state.coreAuthConfigured) "Core conectado" else "Conexão precisa de verificação")
            Text(if(state.networkAvailable) "Este celular com acesso à rede" else "Este celular sem rede",fontSize=12.sp)
            Text("TV, relógio e óculos não são presumidos conectados.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        OutlinedButton(onClick=onDevices,modifier=Modifier.fillMaxWidth()){Text("Dispositivos e suporte")}
        OutlinedButton(onClick=onWorkspace,modifier=Modifier.fillMaxWidth()){Text("Workspace · conexões, memória e cofre")}
        OutlinedButton(onClick=onPlans,modifier=Modifier.fillMaxWidth()){Text("Tarefas e projetos")}
        Button(onClick=onSettings,modifier=Modifier.fillMaxWidth()){Text("Ajustes e privacidade")}
        Text("Você decide quando conectar contas, compartilhar conteúdo e executar ações. Funções remotas do Core continuam independentes do computador.",fontSize=12.sp)
    }
}

@Composable
fun FluxStudyScreen(state:FluxUiState) {
    val app=LocalContext.current.applicationContext as FluxApplication
    val scope=rememberCoroutineScope()
    var topic by remember {mutableStateOf("")}
    var action by remember {mutableStateOf("Explicar")}
    var answer by remember {mutableStateOf("")}
    var notice by remember {mutableStateOf("A lousa funciona localmente. IA e voz exigem pareamento.")}
    var busy by remember {mutableStateOf(false)}
    var voiceBusy by remember {mutableStateOf(false)}
    var independent by remember {mutableStateOf(true)}
    var job by remember {mutableStateOf<Job?>(null)}
    var voiceJob by remember {mutableStateOf<Job?>(null)}
    var player by remember {mutableStateOf<MediaPlayer?>(null)}
    var audioFile by remember {mutableStateOf<File?>(null)}
    fun stopAudio(){voiceJob?.cancel();voiceJob=null;player?.release();player=null;audioFile?.delete();audioFile=null;voiceBusy=false}
    DisposableEffect(Unit){onDispose{job?.cancel();voiceJob?.cancel();player?.release();audioFile?.delete()}}
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp).testTag("flux-study"),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("FLUX Study",style=MaterialTheme.typography.headlineMedium)
        Text("Entenda. Explore. Aprenda.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Lousa local e explicações do seu Core. Avatar 3D em desenvolvimento.",fontSize=12.sp)
        NativeStudyBoard()
        OutlinedTextField(topic,{topic=it},label={Text("Conteúdo ou exercício")},modifier=Modifier.fillMaxWidth(),minLines=3)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            listOf("Explicar","Uma pista","Exercícios").forEach{label->FilterChip(action==label,{action=label},label={Text(label,fontSize=10.sp)})}
        }
        Row{Checkbox(independent,{independent=it});Text("Incentivar meu raciocínio antes da solução",modifier=Modifier.padding(top=12.dp),fontSize=12.sp)}
        Button(enabled=topic.isNotBlank()&&!busy,onClick={stopAudio();job=scope.launch{busy=true;notice="Consultando o Core…";try {
            if(!state.coreAuthConfigured)error("Pareie o aparelho em Ajustes para consultar o FLUX.")
            val directive=when(action){"Uma pista"->"Dê apenas uma pista e aguarde meu raciocínio.";"Exercícios"->"Crie três exercícios progressivos e gabarito separado.";else->"Explique com exemplos e faça uma pergunta para conferir compreensão."}
            val prefs=app.getSharedPreferences("flux_study",0)
            val id=prefs.getString("conversation",null)?:UUID.randomUUID().toString().also{prefs.edit().putString("conversation",it).apply()}
            val result=app.api.workspace("/v1/chat",JSONObject().put("requestId",UUID.randomUUID().toString()).put("conversationId",id).put("message","FLUX Study. $directive ${if(independent) "Modo Independence: ensine o método e incentive minhas habilidades." else ""}\nConteúdo que forneci: $topic").put("modality","TEXT").put("mode","STANDARD"))
            answer=result.getString("content");notice="Resposta recebida do Core. Confira o raciocínio."
        }catch(e:Exception){if(e is CancellationException)throw e;notice=e.message?:"Consulta não concluída."}finally{busy=false}}}){Text(if(busy)"Preparando estudo…" else "Pedir ao FLUX")}
        Text(notice,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp)
        if(answer.isNotBlank()) {
            Text(answer,lineHeight=23.sp)
            Button(enabled=!voiceBusy,onClick={voiceJob=scope.launch{voiceBusy=true;try {
                val bytes=app.api.synthesize(answer)
                val file=withContext(Dispatchers.IO){File.createTempFile("flux-study-",".mp3",app.cacheDir).apply{writeBytes(bytes)}};audioFile=file
                val next=MediaPlayer();player=next;next.setDataSource(file.absolutePath);next.setOnCompletionListener{stopAudio()};next.setOnErrorListener{_,_,_->notice="Áudio não pôde ser reproduzido.";stopAudio();true};next.setOnPreparedListener{if(player===it){it.start();notice="Ouvindo os primeiros 1.000 caracteres com a voz FLUX 4."}};next.prepareAsync()
            }catch(e:Exception){if(e is CancellationException)throw e;notice=e.message?:"Voz indisponível.";stopAudio()}}}){Text("Ouvir com FLUX 4")}
        }
        OutlinedButton(onClick={job?.cancel();stopAudio();busy=false;notice="Consulta e voz interrompidas."}){Text("Interromper")}
        Text("A explicação permanece nesta tela; o Core mantém o contexto da conversa. Este incremento não mede domínio da matéria nem oferece avatar ou barge-in automático durante este áudio de estudo.",fontSize=11.sp)
    }
}

@Composable
private fun NativeStudyBoard(){
    var mode by remember{mutableStateOf("Função quadrática")}
    var coefficient by remember{mutableFloatStateOf(1f)}
    var catalyst by remember{mutableStateOf(false)}
    val accent=MaterialTheme.colorScheme.primary
    Text("LOUSA INTERATIVA",fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {listOf("Função quadrática","Energia de ativação").forEach{label->FilterChip(mode==label,{mode=label},label={Text(label,fontSize=10.sp)})}}
    Canvas(Modifier.fillMaxWidth().height(170.dp).background(MaterialTheme.colorScheme.surface)) {
        val values=(0..80).map{i->val t=i/80f;val y=if(mode=="Função quadrática")coefficient*(-4f+8*t)*(-4f+8*t) else {val peak=if(catalyst)5.5f else 10f;if(t<=.5f)peak*sin(Math.PI*t).toFloat().let{it*it}else peak+(-4f-peak)*sin(Math.PI*(t-.5)).toFloat().let{it*it}};t to y}
        val min=minOf(0f,values.minOf{it.second})-2;val max=maxOf(0f,values.maxOf{it.second})+2
        fun y(v:Float)=size.height-20f-(v-min)/(max-min)*(size.height-40f)
        drawLine(Color(0xFF4A505E),androidx.compose.ui.geometry.Offset(15f,y(0f)),androidx.compose.ui.geometry.Offset(size.width-15f,y(0f)),1f)
        val path=Path();values.forEachIndexed{i,(t,v)->val x=15f+t*(size.width-30f);if(i==0)path.moveTo(x,y(v))else path.lineTo(x,y(v))};drawPath(path,accent,style=Stroke(3f))
    }
    if(mode=="Função quadrática"){Text("y = ${"%.2f".format(coefficient)}x² · domínio de −4 a 4",fontSize=12.sp);Slider(coefficient,{coefficient=it},valueRange=-3f..3f,modifier=Modifier.testTag("study-coefficient"))}
    else{Row{Switch(catalyst,{catalyst=it});Text("Catalisador",Modifier.padding(start=12.dp,top=12.dp))};Text("Perfil qualitativo, unidades ilustrativas. O catalisador reduz a barreira; ΔH continua −4.",fontSize=12.sp)}
}
