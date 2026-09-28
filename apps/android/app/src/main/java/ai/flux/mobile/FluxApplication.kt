package ai.flux.mobile

import android.app.Application
import ai.flux.mobile.data.FluxApiClient
import ai.flux.mobile.data.FluxConnectionSettings
import ai.flux.mobile.data.FluxLocalWorkspace
import ai.flux.mobile.data.FluxNetworkMonitor
import ai.flux.mobile.data.LocalConversationCache
import okhttp3.OkHttpClient
import okhttp3.ConnectionPool
import java.util.concurrent.TimeUnit

class FluxApplication : Application() {
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(210, TimeUnit.SECONDS)
            .build()
    }
    val connectionSettings: FluxConnectionSettings by lazy { FluxConnectionSettings(this) }
    val api: FluxApiClient by lazy {
        FluxApiClient(
            httpClient,
            connectionSettings::coreUrl,
            connectionSettings::authToken,
            connectionSettings::deviceId,
        )
    }
    val cache: LocalConversationCache by lazy { LocalConversationCache(this) }
    val workspace: FluxLocalWorkspace by lazy { FluxLocalWorkspace(this) }
    val networkMonitor: FluxNetworkMonitor by lazy { FluxNetworkMonitor(this) }
}
