package ai.flux.mobile.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class FluxNetworkMonitor(context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    val available: Flow<Boolean> = callbackFlow {
        fun isUsable(network: Network?): Boolean {
            val capabilities = network?.let(manager::getNetworkCapabilities) ?: return false
            // VALIDATED oscila durante a troca entre Wi-Fi e rede móvel. O aplicativo
            // deve tentar o Core enquanto o Android informa capacidade de internet;
            // a chamada HTTP continua sendo a fonte real de verdade.
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(isUsable(network)) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
            override fun onLost(network: Network) { trySend(isUsable(manager.activeNetwork)) }
            override fun onUnavailable() { trySend(false) }
        }
        trySend(isUsable(manager.activeNetwork))
        val registered = runCatching { manager.registerDefaultNetworkCallback(callback) }.isSuccess
        awaitClose {
            if (registered) runCatching { manager.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()
}
