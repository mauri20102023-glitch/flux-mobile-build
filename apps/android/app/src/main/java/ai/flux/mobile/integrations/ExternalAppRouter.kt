package ai.flux.mobile.integrations

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioManager
import android.provider.CalendarContract
import android.view.KeyEvent

class ExternalAppRouter(
    private val context: Context,
    private val onError: (String) -> Unit,
) {
    fun openSpotify() = openAppOrWeb(
        packageName = "com.spotify.music",
        uri = "https://open.spotify.com/",
        failureMessage = "Não encontrei o Spotify neste aparelho.",
    )

    fun openMercadoLivre() = openAppOrWeb(
        packageName = "com.mercadolibre",
        uri = "https://www.mercadolivre.com.br/vendas/lista",
        failureMessage = "Não consegui abrir o Mercado Livre neste aparelho.",
    )

    fun openInstagram() = openAppOrWeb(
        packageName = "com.instagram.android",
        uri = "https://www.instagram.com/",
        failureMessage = "Não consegui abrir o Instagram.",
    )

    fun composeEmail() {
        launch(
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:?subject=Mensagem%20do%20FLUX")),
            "Não encontrei um aplicativo de e-mail.",
        )
    }

    fun openCalendar() {
        val insert = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Compromisso criado com o FLUX")
        }
        launch(insert, "Não encontrei um aplicativo de agenda.")
    }

    fun openCanva() = openAppOrWeb(
        packageName = "com.canva.editor",
        uri = "https://www.canva.com/",
        failureMessage = "Não consegui abrir o Canva.",
    )

    fun openDrive() = openAppOrWeb(
        packageName = "com.google.android.apps.docs",
        uri = "https://drive.google.com/",
        failureMessage = "Não consegui abrir o Google Drive.",
    )

    fun openYouTube() = openAppOrWeb(
        packageName = "com.google.android.youtube",
        uri = "https://www.youtube.com/",
        failureMessage = "Não consegui abrir o YouTube.",
    )

    fun openSmartThings() = openAppOrWeb(
        packageName = "com.samsung.android.oneconnect",
        uri = "https://account.smartthings.com/",
        failureMessage = "Não encontrei o SmartThings neste aparelho.",
    )

    fun playPauseMedia() = dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

    fun nextMedia() = dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)

    fun prepareWhatsApp() {
        val message = "Olá! Mensagem preparada com o Flux AI."
        val direct = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }
        val fallback = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            },
            "Confirmar aplicativo de mensagem",
        )
        launch(if (direct.resolveActivity(context.packageManager) != null) direct else fallback,
            "Não encontrei um aplicativo compatível para preparar a mensagem.")
    }

    private fun openAppOrWeb(packageName: String, uri: String, failureMessage: String) {
        val direct = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(packageName)
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        launch(if (direct.resolveActivity(context.packageManager) != null) direct else fallback, failureMessage)
    }

    private fun launch(intent: Intent, failureMessage: String) {
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure { onError(failureMessage) }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        runCatching {
            val audio = context.getSystemService(AudioManager::class.java)
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }.onFailure { onError("Não consegui controlar a reprodução neste aparelho.") }
    }
}
