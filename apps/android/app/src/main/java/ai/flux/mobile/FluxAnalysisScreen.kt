package ai.flux.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

/** Authorized single-photo/image and first-three-page PDF analysis. No continuous video. */
@Composable
fun FluxAnalysisScreen() {
    val context=LocalContext.current
    val app=context.applicationContext as FluxApplication
    val scope=rememberCoroutineScope()
    var selected by remember { mutableStateOf<Uri?>(null) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var prompt by remember { mutableStateOf("Explique o conteúdo e indique o que está ilegível.") }
    var consent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){photo?.recycle();photo=null;selected=uri;consent=false;output="";error=null}}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()){bitmap->if(bitmap!=null){photo?.recycle();photo=bitmap;selected=null;consent=false;output="";error=null}}
    DisposableEffect(Unit){onDispose{photo?.recycle()}}
    Text("Vision e documentos",style=MaterialTheme.typography.titleLarge)
    Text("Envie uma imagem, tire uma foto ou analise até as 3 primeiras páginas de um PDF. Câmera é foto única; esta tela não transmite vídeo.",style=MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled=!busy,onClick={picker.launch(arrayOf("image/*","application/pdf"))}){Text("Escolher arquivo")}
        OutlinedButton(enabled=!busy,onClick={try{camera.launch(null)}catch(e:Exception){error="Nenhum aplicativo de câmera disponível."}}){Text("Câmera")}
    }
    Text(if(photo!=null)"Foto capturada pela câmera" else if(selected!=null)"Arquivo selecionado" else "Nenhum conteúdo selecionado",style=MaterialTheme.typography.bodySmall)
    OutlinedTextField(prompt,{prompt=it},label={Text("O que deseja entender?")},modifier=Modifier.fillMaxWidth())
    Row {Checkbox(consent,{consent=it},enabled=!busy);Text("Autorizo enviar este conteúdo ao modelo visual na Cloudflare.",modifier=Modifier.padding(top=10.dp),style=MaterialTheme.typography.bodySmall)}
    Button(enabled=!busy&&consent&&prompt.isNotBlank()&&(photo!=null||selected!=null),onClick={
        val uri=selected;val captured=photo;val instruction=prompt
        scope.launch {busy=true;error=null;output=""
            try {
                val captures=withContext(Dispatchers.IO){
                    if(captured!=null) listOf("Foto" to encodeAnalysisFrame(captured))
                    else loadAnalysisFrames(context,uri!!)
                }
                val results=mutableListOf<String>()
                for((label,image) in captures) {
                    val r=app.api.workspace("/v1/vision",JSONObject().put("prompt",instruction.take(2500)+"\nOrigem: "+label+". Não presuma páginas ou quadros não recebidos.").put("image",image))
                    results+=label+"\n"+r.getString("content")
                    output=results.joinToString("\n\n")
                }
            }catch(e:Exception){if(e is CancellationException)throw e;error=e.message ?: "A análise não foi concluída."}
            finally{busy=false}
        }
    }){Text(if(busy)"Analisando conteúdo…" else "Analisar conteúdo autorizado")}
    if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    if(output.isNotBlank())Text(output)
}
private fun encodeAnalysisFrame(bitmap:Bitmap):String {
    val resized=if(bitmap.width>1280||bitmap.height>1280){val scale=1280.0/maxOf(bitmap.width,bitmap.height);Bitmap.createScaledBitmap(bitmap,(bitmap.width*scale).toInt().coerceAtLeast(1),(bitmap.height*scale).toInt().coerceAtLeast(1),true)}else bitmap
    try{val buffer=ByteArrayOutputStream();check(resized.compress(Bitmap.CompressFormat.JPEG,82,buffer));return "data:image/jpeg;base64,"+Base64.encodeToString(buffer.toByteArray(),Base64.NO_WRAP)}finally{if(resized!==bitmap)resized.recycle()}
}
private fun loadAnalysisFrames(context:android.content.Context,uri:Uri):List<Pair<String,String>> {
    val mime=context.contentResolver.getType(uri).orEmpty()
    if(mime=="application/pdf") {
        val temp=File.createTempFile("flux-document-",".pdf",context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use{input->temp.outputStream().use{output->val buffer=ByteArray(8192);var total=0;while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=10000000){"PDF acima de 10 MB. Compartilhe um trecho menor."};output.write(buffer,0,n)}}} ?: error("Arquivo indisponível")
            val fd=android.os.ParcelFileDescriptor.open(temp,android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(fd).use{pdf->require(pdf.pageCount>0){"PDF sem páginas."};return (0 until minOf(3,pdf.pageCount)).map{index->pdf.openPage(index).use{page->
                val scale=1280.0/maxOf(page.width,page.height)
                val bitmap=Bitmap.createBitmap((page.width*scale).toInt().coerceAtLeast(1),(page.height*scale).toInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888)
                try{bitmap.eraseColor(android.graphics.Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);("Página ${index+1} de ${pdf.pageCount}; somente primeiras 3 páginas enviadas" to encodeAnalysisFrame(bitmap))}finally{bitmap.recycle()}
            }}}
        }finally{temp.delete()}
    }
    require(mime.startsWith("image/")){"Formato não suportado. Escolha PDF ou imagem."}
    val bytes=context.contentResolver.openInputStream(uri)?.use{input->val output=ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0;while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=10000000){"Imagem acima de 10 MB."};output.write(buffer,0,n)};output.toByteArray()} ?: error("Imagem indisponível")
    val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
    require(bounds.outWidth>0&&bounds.outHeight>0){"Imagem inválida."}
    var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>2560)sample*=2
    val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample}) ?: error("Não foi possível ler a imagem.")
    try{return listOf("Imagem compartilhada" to encodeAnalysisFrame(bitmap))}finally{bitmap.recycle()}
}
