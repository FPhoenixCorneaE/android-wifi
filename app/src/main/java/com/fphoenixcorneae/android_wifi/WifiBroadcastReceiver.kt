package com.fphoenixcorneae.android_wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log

/**
 * WiFi扫描结果、WiFi状态变化的广播接收
 */
class WifiBroadcastReceiver(
    private val onEnabledWifi: (Boolean) -> Unit = {},
    private val onScanResultsAvailable: () -> Unit = {},
    private val onWifiConnect: () -> Unit = {},
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)) {
            WifiManager.WIFI_STATE_ENABLED -> {
                Log.d("WifiBroadcastReceiver", "onReceive: Wifi开启成功")
                onEnabledWifi(true)
            }

            WifiManager.WIFI_STATE_DISABLED -> {
                Log.d("WifiBroadcastReceiver", "onReceive: Wifi开关未打开")
                onEnabledWifi(false)
            }

            else -> {}
        }

        when (intent?.action) {
            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                onScanResultsAvailable()
            }

            WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION -> {
                Log.d("WifiBroadcastReceiver", "onReceive: onWifiConnect")
                onWifiConnect()
            }
        }
    }
}