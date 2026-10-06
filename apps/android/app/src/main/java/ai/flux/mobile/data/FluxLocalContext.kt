package ai.flux.mobile.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CalendarEntry(val title: String, val start: String, val end: String)
data class LocalFacts(val context: String, val calendarHeadline: String, val weatherHeadline: String)

/** Reads only the next seven days from calendars already synced on this phone. */
object FluxLocalContext {
    private val timeFormat = DateTimeFormatter.ofPattern("EEE HH:mm").withZone(ZoneId.systemDefault())

    suspend fun collect(context: Context, api: FluxApiClient, message: String): LocalFacts {
        val normalized = message.lowercase()
        val overview = listOf("bom dia", "me atualize", "resumo do dia", "meu dia").any(normalized::contains)
        val calendarRequested = overview || listOf("agenda", "compromisso", "evento", "reunião", "reuniao").any(normalized::contains)
        val weatherRequested = overview || listOf("clima", "tempo", "previsão", "previsao", "chuva", "temperatura").any(normalized::contains)
        val facts = mutableListOf<String>()
        var calendarHeadline = "Agenda não autorizada"
        var weatherHeadline = "Clima não consultado"

        if (calendarRequested) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                val entries = withContext(Dispatchers.IO) { readCalendar(context) }
                calendarHeadline = if (entries.isEmpty()) "Sem eventos nos próximos 7 dias"
                    else "${entries.size} evento(s) nos próximos 7 dias"
                facts += if (entries.isEmpty()) "Agenda do Android: sem eventos nos próximos sete dias."
                    else "Agenda do Android (próximos sete dias): " + entries.joinToString("; ") {
                        "${it.title} em ${timeFormat.format(Instant.parse(it.start))}"
                    }
                // A sincronização permite que o site pareado mostre o mesmo panorama.
                runCatching { api.updateCalendar(entries) }
            } else facts += "Agenda do Android: acesso ainda não permitido pelo usuário."
        }

        if (weatherRequested) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val location = withContext(Dispatchers.IO) {
                    val manager = context.getSystemService(LocationManager::class.java)
                    manager.getProviders(true).asSequence().mapNotNull { provider ->
                        runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
                    }.maxByOrNull { it.time }
                }
                if (location != null) {
                    runCatching { api.weather(location.latitude, location.longitude) }.onSuccess { report ->
                        weatherHeadline = "${report.temperature.toInt()}° · ${report.condition}"
                        facts += "Clima atual (Open-Meteo, consultado agora): ${report.temperature}°C, ${report.condition}; " +
                            "máxima ${report.high}°C, mínima ${report.low}°C, chance de chuva ${report.rainChance}%."
                    }.onFailure { facts += "Clima: serviço temporariamente indisponível." }
                } else facts += "Clima: Android ainda não obteve uma localização aproximada."
            } else facts += "Clima: acesso à localização aproximada ainda não permitido."
        }
        return LocalFacts(facts.joinToString("\n"), calendarHeadline, weatherHeadline)
    }

    private fun readCalendar(context: Context): List<CalendarEntry> {
        val start = System.currentTimeMillis()
        val end = start + 7L * 24 * 60 * 60 * 1_000
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString()).appendPath(end.toString()).build()
        val columns = arrayOf(CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN, CalendarContract.Instances.END)
        val result = mutableListOf<CalendarEntry>()
        context.contentResolver.query(uri, columns, null, null,
            "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            while (cursor.moveToNext() && result.size < 20) {
                val title = cursor.getString(0)?.trim()?.take(180).orEmpty()
                if (title.isBlank()) continue
                result += CalendarEntry(title, Instant.ofEpochMilli(cursor.getLong(1)).toString(),
                    Instant.ofEpochMilli(cursor.getLong(2)).toString())
            }
        }
        return result
    }
}
