package ai.flux.mobile.security

import android.app.KeyguardManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ai.flux.mobile.FluxTheme
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local-only vault. Each decryption/encryption requires an authenticated Keystore operation. */
class FluxVaultActivity : ComponentActivity() {
    private var entries by mutableStateOf<JSONArray?>(null)
    private var notice by mutableStateOf("Bloqueado. Autentique-se pelo Android.")
    private var service by mutableStateOf("")
    private var username by mutableStateOf("")
    private var password by mutableStateOf("")
    private val prefs by lazy { getSharedPreferences("flux_vault_encrypted",MODE_PRIVATE) }
    private val aad="flux-vault-v1".toByteArray()
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { FluxTheme("cyan") {
            Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                Column(Modifier.statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text("FLUX Vault",style=MaterialTheme.typography.headlineMedium)
                    Text("Cofre local · AES-256-GCM · Android Keystore")
                    Text(notice,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(entries==null) Button(onClick=::unlock) {Text("Autenticar e abrir")}
                    else {
                        Button(onClick=::lock) {Text("Bloquear agora")}
                        OutlinedTextField(service,{service=it},label={Text("Serviço")},modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(username,{username=it},label={Text("Usuário")},modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(password,{password=it},label={Text("Senha")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
                        OutlinedButton(onClick={val chars="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%+-_";val rng=SecureRandom();password=buildString{repeat(24){append(chars[rng.nextInt(chars.length)])}}}) {Text("Gerar senha forte")}
                        Button(enabled=service.isNotBlank()&&password.isNotBlank(),onClick={
                            val copy=JSONArray(entries.toString()).put(JSONObject().put("id",UUID.randomUUID().toString()).put("service",service.take(120)).put("username",username.take(200)).put("password",password.take(500)))
                            save(copy)
                        }){Text("Autenticar e guardar")}
                        val rows=entries
                        for(i in 0 until(rows?.length()?:0)) {
                            val item=rows!!.getJSONObject(i)
                            HorizontalDivider()
                            Text(item.optString("service"),style=MaterialTheme.typography.titleMedium)
                            Text(item.optString("username"))
                            var reveal by remember(item.getString("id")){mutableStateOf(false)}
                            Text(if(reveal)item.optString("password")else"••••••••••••")
                            Row {
                                TextButton(onClick={reveal=!reveal}){Text(if(reveal)"Ocultar" else "Mostrar")}
                                TextButton(onClick={val next=JSONArray();for(j in 0 until rows.length())if(j!=i)next.put(rows.getJSONObject(j));save(next)}){Text("Excluir com autenticação")}
                            }
                        }
                    }
                    Text("Não sincroniza senhas, não envia dados à IA e não possui backup ou preenchimento automático nesta versão. Perder a chave do aparelho pode tornar o cofre irrecuperável.",style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick={finish()}){Text("Voltar ao FLUX")}
                }
            }
        } }
    }
    private fun secret():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (store.getKey("flux-vault-v1",null) as? SecretKey)?.let{return it}
        val builder=KeyGenParameterSpec.Builder("flux-vault-v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
        if(Build.VERSION.SDK_INT>=30) builder.setUserAuthenticationParameters(0,KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        else @Suppress("DEPRECATION") builder.setUserAuthenticationValidityDurationSeconds(-1)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{init(builder.build())}.generateKey()
    }
    private fun authenticate(cipher:Cipher,action:(Cipher)->Unit) {
        if(!getSystemService(KeyguardManager::class.java).isDeviceSecure){notice="Configure bloqueio de tela seguro e biometria nas configurações Android.";return}
        val builder=BiometricPrompt.Builder(this).setTitle("FLUX Vault").setSubtitle("Autorize esta operação protegida no aparelho")
        if(Build.VERSION.SDK_INT>=30) builder.setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        else builder.setNegativeButton("Cancelar",mainExecutor){_,_->lock()}
        builder.build().authenticate(BiometricPrompt.CryptoObject(cipher),CancellationSignal(),mainExecutor,object:BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){try{action(result.cryptoObject!!.cipher!!)}catch(e:Exception){lock();notice="A operação criptográfica falhou. Não remova o cofre se precisar recuperar seus dados."}}
            override fun onAuthenticationError(code:Int,message:CharSequence){notice="Operação não autorizada: $message"}
        })
    }
    private fun unlock(){try{
        val stored=prefs.getString("data",null)
        if(stored==null){save(JSONArray());return}
        val iv=Base64.decode(prefs.getString("iv",null),Base64.NO_WRAP)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply{init(Cipher.DECRYPT_MODE,secret(),GCMParameterSpec(128,iv));updateAAD(aad)}
        authenticate(cipher){authorized->val plaintext=authorized.doFinal(Base64.decode(stored,Base64.NO_WRAP));try{entries=JSONArray(String(plaintext,Charsets.UTF_8));notice="Cofre aberto. Bloqueia ao sair da tela."}finally{plaintext.fill(0)}}
    }catch(e:Exception){notice="Cofre indisponível. Confira a autenticação do aparelho; a chave pode ter sido invalidada. Não apagamos dados automaticamente."}}
    private fun save(next:JSONArray){try{
        require(next.length()<=200){"Limite de 200 credenciais."}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply{init(Cipher.ENCRYPT_MODE,secret());updateAAD(aad)}
        authenticate(cipher){authorized->val plaintext=next.toString().toByteArray(Charsets.UTF_8);try{
            val encrypted=authorized.doFinal(plaintext)
            check(prefs.edit().putString("data",Base64.encodeToString(encrypted,Base64.NO_WRAP)).putString("iv",Base64.encodeToString(authorized.iv,Base64.NO_WRAP)).commit())
            entries=next;service="";username="";password="";notice="Credenciais guardadas de forma criptografada neste aparelho."
        }finally{plaintext.fill(0)}}
    }catch(e:Exception){notice="Não foi possível preparar a gravação criptografada."}}
    private fun lock(){entries=null;service="";username="";password="";notice="Cofre bloqueado."}
    override fun onStop(){lock();super.onStop()}
}
