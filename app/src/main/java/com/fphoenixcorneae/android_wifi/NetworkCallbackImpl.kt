package com.fphoenixcorneae.android_wifi

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.os.Build
import android.util.Log

class NetworkCallbackImpl(
    private val onWifiConnected: (WifiInfo?) -> Unit = {},
    private val onCellularConnected: () -> Unit = {},
) : ConnectivityManager.NetworkCallback() {

    private val TAG = "NetworkCallbackImpl"

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        Log.i(TAG, "网络已链接")
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        Log.i(TAG, "网络已断开")
    }


    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            when {
                // WIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.i(TAG, "wifi已经连接")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        onWifiConnected(networkCapabilities.transportInfo as WifiInfo?)
                    } else {
                        onWifiConnected(null)
                    }
                }
                // 数据流量
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Log.i(TAG, "数据流量已经连接")
                    onCellularConnected()
                }

                else -> {
                    Log.i(TAG, "其他网络")
                }
            }
        }
    }

    override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {

    }
}
