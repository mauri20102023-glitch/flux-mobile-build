package ai.flux.mobile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.flux.mobile.model.FluxUiState
import ai.flux.mobile.security.FluxVaultActivity
import kotlinx.coroutines.*
import org.json.JSONObject

@Composable
fun FluxWorkspaceScreen(state: FluxUiState, onGenerate: (String)->Unit, onSend: (String)->Unit) {
    val app = LocalContext.current.applicationContext as FluxApplication
    val context = LocalContext.current
    var section by remember { mutableStateOf("Criar") }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var title by remember(section) { mutableStateOf("") }
    var content by remember(section) { mutableStateOf("") }
    var query by remember(section) { mutableStateOf("") }
    var consent by remember(section) { mutableStateOf(false) }
    var editing by remember(section) { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf("study") }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        val requestedSection = section
        val loaded = when (requestedSection) {
            "Memória" -> app.api.workspace("/v1/memories")
            "Missões", "Estudar" -> app.api.workspace("/v1/missions")
            "Conexões" -> app.api.workspace("/v1/connect")
            "Sistema" -> app.api.workspace("/v1/status")
            "Consumo" -> app.api.workspace("/v1/usage")
            else -> null
        }
        if(section == requestedSection) result=loaded
    }
    fun execute(action: suspend ()->Unit) {
        if(busy) return
        scope.launch { busy=true; error=null; try { action() } catch (e:Exception) {
            if(e is CancellationException) throw e
            error=e.message ?: "Operação não concluída."
        } finally { busy=false } }
    }
    LaunchedEffect(section, state.coreAuthConfigured) {
        result=null;error=null
        if(section !in listOf("Criar","Segurança","Builder")) {
            if(!state.coreAuthConfigured) error="Pareie o aparelho em Ajustes para acessar este módulo."
            else try { busy=true; load() } catch(e:Exception) { if(e is CancellationException)throw e;error=e.message } finally {busy=false}
        }
    }
    LaunchedEffect(section, state.coreAuthConfigured) {
        if (state.coreAuthConfigured && section in listOf("Missões","Estudar")) while(true) {
            delay(8000)
            if(!busy) try { load() } catch(e:Exception) { if(e is CancellationException)throw e;error=e.message }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Workspace", style=MaterialTheme.typography.headlineMedium, modifier=Modifier.padding(start=22.dp,top=22.dp))
        Text("Suas ferramentas, um só lugar.", color=MaterialTheme.colorScheme.onSurfaceVariant, modifier=Modifier.padding(start=22.dp,top=4.dp,bottom=16.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=18.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("Criar","Estudar","Memória","Missões","Conexões","Segurança","Consumo","Sistema","Builder").forEach {
                FilterChip(selected=section==it,onClick={section=it},enabled=!busy,label={Text(it)})
            }
        }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal=22.dp))
        error?.let { Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(22.dp)) }
        if(section=="Criar") { LabScreen(state,onGenerate); return@Column }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
            when(section) {
                "Memória" -> {
                    Text("Memórias autorizadas",style=MaterialTheme.typography.titleLarge)
                    Text("Armazenamento do proprietário pareado. Perfis de outras pessoas ainda não são suportados. Não guarde senhas aqui.",fontSize=12.sp)
                    OutlinedTextField(title,{title=it},label={Text("Título")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(content,{content=it},label={Text("Informação para lembrar")},modifier=Modifier.fillMaxWidth())
                    Row { Checkbox(consent,{consent=it}); Text("Autorizo guardar esta informação",modifier=Modifier.padding(top=12.dp)) }
                    Button(enabled=!busy && consent && title.isNotBlank() && content.isNotBlank(),onClick={ execute {
                        app.api.workspace("/v1/memories"+(editing?.let{"/$it"} ?: ""),JSONObject().put("title",title).put("content",content).put("category","preference").put("consent",true),if(editing==null) "POST" else "PATCH")
                        title="";content="";editing=null;consent=false;load()
                    } }) {Text(if(editing==null) "Guardar memória" else "Salvar correção")}
                    val rows=result?.optJSONArray("memories")
                    for(i in 0 until (rows?.length() ?: 0)) {
                        val m=rows!!.getJSONObject(i)
                        WorkspaceCard(m.optString("title"),m.optString("content")) {
                            Row { TextButton(onClick={editing=m.getString("id");title=m.getString("title");content=m.getString("content");consent=false}) {Text("Corrigir")}
                                TextButton(enabled=!busy,onClick={execute {app.api.workspace("/v1/memories/"+m.getString("id"),method="DELETE");load()}}) {Text("Esquecer")} }
                        }
                    }
                }
                "Missões", "Estudar" -> {
                    Text(if(section=="Estudar") "Seu tutor pessoal" else "Mission Control",style=MaterialTheme.typography.titleLarge)
                    Text("Resultados de texto gerados pelo Core, com fila persistente. Sem envio de mensagens ou alterações de produção.",fontSize=12.sp)
                    if(section=="Missões") Row(Modifier.horizontalScroll(rememberScrollState())) {
                        listOf("study" to "Estudo","code" to "Código","business" to "Empresa","draft" to "Texto").forEach{(id,label)->FilterChip(kind==id,{kind=id},label={Text(label)},modifier=Modifier.padding(end=6.dp))}
                    }
                    OutlinedTextField(content,{content=it},label={Text("Conteúdo, matéria ou objetivo")},modifier=Modifier.fillMaxWidth())
                    if(section=="Estudar") {
                        listOf("Explicar" to "Identifique conhecimento prévio, explique passo a passo com exemplos e faça perguntas para verificar compreensão.",
                            "Simulado" to "Crie um simulado com 5 questões e gabarito comentado separado.",
                            "Flashcards" to "Crie 10 flashcards de pergunta e resposta.",
                            "Mapa mental" to "Organize um mapa mental textual com conceitos relacionados, hierarquia e exemplos.").forEach{(label,prompt)->
                            OutlinedButton(enabled=content.isNotBlank()&&!busy,onClick={execute{app.api.workspace("/v1/missions",JSONObject().put("kind","study").put("objective",prompt+"\nMaterial autorizado: "+content));load()}}) {Text(label)}
                        }
                        Text("Para Geekie One/ClassApp, compartilhe conteúdo permitido ou use Vision. Acesso automático à conta ainda não implementado.",fontSize=12.sp)
                    } else Button(enabled=content.isNotBlank()&&!busy,onClick={execute{app.api.workspace("/v1/missions",JSONObject().put("kind",kind).put("objective",content));content="";load()}}){Text("Executar missão")}
                    val rows=result?.optJSONArray("missions")
                    for(i in 0 until (rows?.length()?:0)) {
                        val m=rows!!.getJSONObject(i)
                        WorkspaceCard(m.optString("objective"),m.optString("status")+" · "+m.optString("createdAt")) {
                            if(m.has("output")) Text(m.getString("output"),lineHeight=22.sp)
                            if(m.optString("status") in listOf("queued","running")) TextButton(enabled=!busy,onClick={execute{app.api.workspace("/v1/missions/"+m.getString("id")+"/cancel",JSONObject());load()}}){Text("Cancelar")}
                            if(m.has("error"))Text(m.optString("error"),color=MaterialTheme.colorScheme.error)
                        }
                    }
                }
                "Conexões" -> {
                    Text("FLUX Connect",style=MaterialTheme.typography.titleLarge)
                    Text("As contas conectadas ao Codex não são automaticamente contas do FLUX. Cada autorização abaixo é específica do aplicativo.",fontSize=12.sp)
                    OutlinedTextField(query,{query=it},label={Text("Pesquisa para leituras autorizadas")},modifier=Modifier.fillMaxWidth())
                    val rows=result?.optJSONArray("providers")
                    for(i in 0 until (rows?.length()?:0)) {
                        val p=rows!!.getJSONObject(i);val id=p.getString("id")
                        WorkspaceCard(p.getString("name"),p.optString("state")) {
                            val missing=p.optJSONArray("missing")
                            if((missing?.length()?:0)>0)Text("Cadastro OAuth pendente: "+missing.toString(),fontSize=12.sp)
                            Text("Permissões: "+p.optString("scope"),fontSize=11.sp)
                            Text("Callback: "+p.optString("redirectUri"),fontSize=10.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                TextButton(enabled=!busy,onClick={execute{val r=app.api.workspace("/v1/connect/$id/start",JSONObject());context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(r.getString("url"))))}}){Text("Autorizar")}
                                TextButton(enabled=!busy&&p.optBoolean("connected"),onClick={execute{app.api.workspace("/v1/connect/$id/test",JSONObject());load()}}){Text("Testar")}
                                TextButton(enabled=!busy&&p.optBoolean("connected"),onClick={execute{val r=app.api.workspace("/v1/connect/$id/read",JSONObject().put("query",query));error=null;content=r.toString(2)}}){Text("Consultar")}
                                TextButton(enabled=!busy&&p.optBoolean("connected"),onClick={execute{app.api.workspace("/v1/connect/$id/revoke",JSONObject());load()}}){Text("Revogar")}
                            }
                        }
                    }
                    if(content.isNotBlank()) Text(content.take(12000),fontSize=12.sp)
                    val alternatives=result?.optJSONArray("alternatives")
                    for(i in 0 until (alternatives?.length()?:0)){val a=alternatives!!.getJSONObject(i);WorkspaceCard(a.optString("id"),a.optString("state")+" · "+a.optString("method")) {}}
                }
                "Segurança" -> {
                    WorkspaceCard("FLUX Vault","Cofre local criptografado, protegido pela biometria forte ou credencial segura do aparelho.") {
                        Button(onClick={context.startActivity(Intent(context,FluxVaultActivity::class.java))}) {Text("Abrir cofre seguro")}
                    }
                    Text("Cofre não envia senhas à IA. Backup, sincronização e preenchimento automático ainda não implementados. Voz não autentica acesso ao cofre.",fontSize=12.sp)
                    WorkspaceCard("Privacidade","Capture somente o que você autorizar. Revogue contas em Conexões e apague memórias na aba Memória.") {}
                }
                "Consumo" -> {
                    Text("Orçamento de R$ 200/mês",style=MaterialTheme.typography.titleLarge)
                    Text("Estimativas de operação, não uma fatura. Realtime pode consumir diretamente no provedor; consulte Billing para o total real.",fontSize=12.sp)
                    result?.let { usage ->
                        val settings=usage.optJSONObject("settings") ?: JSONObject()
                        val planning=usage.optJSONObject("planning") ?: JSONObject()
                        WorkspaceCard("Estimativa mensal",usage.optString("alert")) {
                            Text("R$ %.2f".format(java.util.Locale("pt","BR"),usage.optDouble("estimateBrl")),fontSize=34.sp,fontWeight=FontWeight.SemiBold)
                            Text("Referência: 60 minutos de conversa/dia. Metade do tempo com o FLUX falando.",fontSize=12.sp)
                        }
                        WorkspaceCard("Reserva por serviço","Preços em dólares; créditos não são contratados pelo aplicativo.") {
                            Text("Inworld Creator: US$ "+planning.optInt("inworldCreatorSubscriptionUsd")+"/mês · US$ "+planning.optInt("includedCreditUsd")+" em créditos")
                            Text("Cloudflare: reserva de US$ "+planning.optInt("cloudflareReserveUsd")+"/mês")
                            Text("Uso Inworld estimado: US$ %.2f".format(planning.optDouble("inworldEstimatedUseUsd")))
                        }
                        var fx by remember(settings.toString()) { mutableStateOf(settings.optString("usdBrl")) }
                        var fees by remember(settings.toString()) { mutableStateOf(settings.optString("feesPercent")) }
                        OutlinedTextField(fx,{fx=it},label={Text("Dólar em reais")},modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(fees,{fees=it},label={Text("Reserva de taxas (%)")},modifier=Modifier.fillMaxWidth())
                        Button(enabled=!busy,onClick={execute{
                            val rate=fx.replace(',','.').toDoubleOrNull() ?: error("Informe um câmbio válido.")
                            val fee=fees.replace(',','.').toDoubleOrNull() ?: error("Informe uma porcentagem válida.")
                            app.api.workspace("/v1/usage",JSONObject().put("usdBrl",rate).put("feesPercent",fee));load()
                        }}) { Text("Atualizar estimativa") }
                        val counts=usage.optJSONObject("observedGatewayAttempts") ?: JSONObject()
                        val limits=usage.optJSONObject("routeLimits") ?: JSONObject()
                        WorkspaceCard("Uso registrado no Core",usage.optString("month")) {
                            listOf("textRequests" to "Respostas de texto","imageRequests" to "Imagens","visionRequests" to "Análises de tela","ttsCharacters" to "Caracteres de voz","voiceOffers" to "Sessões de voz").forEach{(key,label)->
                                Text(label+": "+counts.optLong(key)+" / "+limits.optLong(key),fontSize=13.sp)
                                LinearProgressIndicator(progress={ (counts.optDouble(key,0.0)/limits.optDouble(key,1.0)).toFloat().coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth())
                            }
                        }
                        Text(usage.optString("warning"),fontSize=12.sp)
                    }
                    Text("Nenhuma compra ou recarga automática. Câmbio, impostos e taxas dependem da sua forma de pagamento.",fontSize=12.sp)
                }
                "Sistema" -> result?.let { Text(it.toString(2),fontSize=12.sp,lineHeight=20.sp) }
                "Builder" -> {
                    Text("Builder Lab",style=MaterialTheme.typography.titleLarge)
                    Text("Crie uma proposta de código como missão. Execução de testes em repositórios e implantação pelo FLUX ainda não implementadas. Produção não pode ser alterada por este módulo.")
                    OutlinedTextField(content,{content=it},label={Text("Problema técnico ou melhoria")},modifier=Modifier.fillMaxWidth())
                    Button(enabled=content.isNotBlank()&&!busy,onClick={execute{app.api.workspace("/v1/missions",JSONObject().put("kind","code").put("objective","Prepare uma proposta de código, critérios de teste e rollback. Não afirme execução.\n"+content));section="Missões";load()}}){Text("Preparar proposta")}
                }
            }
            if(section in listOf("Memória","Missões","Estudar","Conexões","Sistema","Consumo"))OutlinedButton(enabled=!busy,onClick={execute{load()}}){Text("Atualizar dados")}
            Spacer(Modifier.height(30.dp))
        }
    }
}
@Composable
private fun WorkspaceCard(title:String,detail:String,content:@Composable ColumnScope.()->Unit) {
    Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(title,fontWeight=FontWeight.SemiBold);Text(detail,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp);content()}
    }
}
