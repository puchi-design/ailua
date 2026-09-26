package com.example.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * Reads real device status into [OsChromeState].
 *
 * Deliberately lightweight: one synchronous read per minute, no background
 * service, no callbacks registration, no permissions that require user consent.
 */
object OsChromeRuntimeSource {

    fun read(context: Context): OsChromeState = OsChromeState(
        timeLabel = OsChromeFormat.timeLabel(System.currentTimeMillis()),
        batteryPercent = readBatteryPercent(context)
            ?: OsChromeState.PreviewBaseline.batteryPercent,
        networkLabel = OsChromeFormat.networkLabel(readNetworkKind(context))
    )

    /** Device battery percentage, or null when the platform cannot report it. */
    fun readBatteryPercent(context: Context): Int? {
        val batteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 0..100) level else null
    }

    fun readNetworkKind(context: Context): OsNetworkKind {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return OsNetworkKind.NONE
        return try {
            val activeNetwork = connectivityManager.activeNetwork
                ?: return OsNetworkKind.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                ?: return OsNetworkKind.NONE
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                    OsNetworkKind.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                    OsNetworkKind.CELLULAR
                else -> OsNetworkKind.NONE
            }
        } catch (e: SecurityException) {
            OsNetworkKind.NONE
        }
    }
}

/**
 * [OsChromeState] fed by the device clock, refreshed on every minute boundary
 * so the chrome stays alive for as long as the composition does.
 */
@Composable
fun rememberOsChromeState(): OsChromeState {
    val context = LocalContext.current
    var state by remember(context) { mutableStateOf(OsChromeRuntimeSource.read(context)) }

    LaunchedEffect(context) {
        while (true) {
            delay(OsChromeFormat.millisUntilNextMinute(System.currentTimeMillis()))
            state = OsChromeRuntimeSource.read(context)
        }
    }

    return state
}
