package com.fphoenixcorneae.android_wifi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.MacAddress
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat


@SuppressLint("MissingPermission")
object WifiManagerImpl {

    private var mConnectivityManager: ConnectivityManager? = null
    private var mWifiManager: WifiManager? = null
    private val mWifiBroadcastReceiver by lazy {
        WifiBroadcastReceiver(
            onEnabledWifi = {
                mOnWifiEnabled?.invoke(it)
                if (it) {
                    mWifiManager?.startScan()
                } else {
                    mOnScanResults?.invoke(null)
                }
            },
            onScanResultsAvailable = {
                val results = scanResults()
                mOnScanResults?.invoke(results)
            },
        ) {
            mWifiManager?.startScan()
        }
    }

    private val mNetworkCallbackImpl by lazy {
        NetworkCallbackImpl(
            onWifiConnected = {
                mOnWifiConnected?.invoke(it ?: getConnectionWifiInfo())
            }
        )
    }
    private var mWifiSettingsPanelLauncher: ActivityResultLauncher<Intent>? = null
    private var mOnWifiEnabled: ((Boolean) -> Unit)? = null
    private var mOnScanResults: ((scanResults: List<ScanResult>?) -> Unit)? = null
    private var mOnWifiConnected: ((WifiInfo?) -> Unit)? = null

    /**
     * 注册监听wifi扫描结果、wifi开关变化的状态、wifi是否连接成功
     */
    fun registerWifiBroadcastReceiver(context: Context) = apply {
        if (mWifiManager == null) {
            mWifiManager = ContextCompat.getSystemService(context, WifiManager::class.java)
        }
        val intentFilter = IntentFilter().apply {
            // 监听wifi扫描结果
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            // 监听wifi开关变化的状态
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // wifi连接结果
                addAction(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)
            }
        }
        context.registerReceiver(mWifiBroadcastReceiver, intentFilter)
        if (mConnectivityManager == null) {
            mConnectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        }
        // 监听wifi是否连接成功
        mConnectivityManager?.requestNetwork(NetworkRequest.Builder().build(), mNetworkCallbackImpl)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (context is ComponentActivity) {
                mWifiSettingsPanelLauncher =
                    context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
            }
        }
    }

    /**
     * 反注册
     */
    fun unregisterWifiBroadcastReceiver(context: Context) {
        context.unregisterReceiver(mWifiBroadcastReceiver)
        mConnectivityManager?.unregisterNetworkCallback(mNetworkCallbackImpl)
    }

    /**
     * 打开/关闭wifi
     */
    fun enabledWifi(context: Context, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 以 Android 10 或更高版本为目标平台的应用无法启用或停用 Wi-Fi。WifiManager.setWifiEnabled() 方法始终返回 false。
            // 使用设置面板
            mWifiSettingsPanelLauncher?.launch(Intent(Settings.Panel.ACTION_WIFI))
        } else {
            if (mWifiManager == null) {
                mWifiManager = ContextCompat.getSystemService(context, WifiManager::class.java)
            }
            if (mWifiManager?.isWifiEnabled != enabled) {
                mWifiManager?.isWifiEnabled = enabled
            }
        }
    }

    /**
     * 获取wifi扫描结果
     */
    private fun scanResults(): MutableList<ScanResult> {
        // 过滤ssid重复且信号水平低的wifi
        val results = mutableListOf<ScanResult>()
        val signalStrength = hashMapOf<String, Int>()
        mWifiManager?.scanResults?.filter {
            val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.wifiSsid?.toString()
            } else {
                it.SSID
            }
            !ssid.isNullOrEmpty()
        }?.forEachIndexed { index, scanResult ->
            Log.d("TAG", "calculateSignalLevel: ${WifiManager.calculateSignalLevel(scanResult.level, 5)}")
            val key = buildString {
                append(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        scanResult.wifiSsid?.toString()
                    } else {
                        scanResult.SSID
                    }
                )
                append(" ")
                append(scanResult.capabilities)
            }
            if (!signalStrength.containsKey(key)) {
                signalStrength[key] = results.size
                results.add(scanResult)
            } else {
                signalStrength[key]?.let { position ->
                    val result = results[position]
                    mWifiManager?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (it.calculateSignalLevel(scanResult.level) > it.calculateSignalLevel(result.level)) {
                                results[position] = scanResult
                            }
                        } else {
                            if (scanResult.level > result.level) {
                                results[position] = scanResult
                            }
                        }
                    }
                }
            }
        }
        // 排序
        results.sortWith(Comparator { o1, o2 ->
            return@Comparator if (WifiManager.calculateSignalLevel(o1.level, 5)
                == WifiManager.calculateSignalLevel(o2.level, 5)
            ) {
                val o1Ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    o1.wifiSsid?.toString() ?: ""
                } else {
                    o1.SSID ?: ""
                }
                val o2Ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    o2.wifiSsid?.toString() ?: ""
                } else {
                    o2.SSID ?: ""
                }
                o1Ssid.compareTo(o2Ssid, ignoreCase = true)
            } else {
                o2.level.compareTo(o1.level)
            }
        })
        return results
    }

    fun getConnectionWifiInfo() = mWifiManager?.connectionInfo

    /**
     * 获取网络配置列表
     */
    fun getWifiConfigurations() = mWifiManager?.configuredNetworks

    @RequiresApi(Build.VERSION_CODES.R)
    fun getNetworkSuggestions() = mWifiManager?.networkSuggestions

    fun createWifiConfiguration(ssid: String?, password: String?, type: WifiCipherType?): WifiConfiguration {
        val config = WifiConfiguration()
        config.allowedAuthAlgorithms.clear()
        config.allowedGroupCiphers.clear()
        config.allowedKeyManagement.clear()
        config.allowedPairwiseCiphers.clear()
        config.allowedProtocols.clear()
        config.SSID = "\"" + ssid + "\""
        when {
            type === WifiCipherType.NO_PASSWORD -> {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }

            type === WifiCipherType.WEP -> {
                config.preSharedKey = "\"" + password + "\""
                config.hiddenSSID = true
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                config.wepTxKeyIndex = 0
            }

            type === WifiCipherType.WPA -> {
                config.preSharedKey = "\"" + password + "\""
                config.hiddenSSID = true
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                config.status = WifiConfiguration.Status.ENABLED
            }
        }
        return config
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun addNetworkSuggestion(
        scanResult: ScanResult?,
        password: String?,
        networkSuggestion: WifiNetworkSuggestion? = null,
    ) {
        scanResult ?: return
        val wifiNetworkSuggestion = WifiNetworkSuggestion.Builder()
            .setSsid(scanResult.getSsid())
            .setBssid(MacAddress.fromString(scanResult.BSSID))
            .setIsAppInteractionRequired(true)
            .apply {
                if (scanResult.getWifiCipher() == WifiCipherType.WEP || scanResult.getWifiCipher() == WifiCipherType.WPA) {
                    password?.let {
                        if (scanResult.capabilities.contains("wpa3", true)) {
                            setWpa3Passphrase(it)
                        } else {
                            setWpa2Passphrase(it)
                        }
                    }
                }
            }
            .build()
        // 先添加建议
        when (mWifiManager?.addNetworkSuggestions(listOf(networkSuggestion ?: wifiNetworkSuggestion))) {
            WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS, WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> {
                val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                    .setSsid(scanResult.getSsid())
                    .setBssid(MacAddress.fromString(scanResult.BSSID))
                    .apply {
                        if (scanResult.getWifiCipher() == WifiCipherType.WEP || scanResult.getWifiCipher() == WifiCipherType.WPA) {
                            password?.let {
                                if (scanResult.capabilities.contains("wpa3", true)) {
                                    setWpa3Passphrase(it)
                                } else {
                                    setWpa2Passphrase(it)
                                }
                            }
                        }
                    }
                    .build()
                val networkRequest = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(wifiNetworkSpecifier)
                    .build()
                // 连接wifi
                mConnectivityManager?.requestNetwork(networkRequest, mNetworkCallbackImpl)
            }

            else -> {}
        }
    }

    /**
     * 忘记Wifi
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun removeNetworkSuggestions(networkSuggestions: List<WifiNetworkSuggestion>) {
        mWifiManager?.removeNetworkSuggestions(networkSuggestions)
    }

    /**
     * 忘记Wifi
     * See [WifiManager.addNetworkSuggestions], [WifiManager.removeNetworkSuggestions]
     */
    @Deprecated("")
    fun removeNetwork(netId: Int) {
        mWifiManager?.removeNetwork(netId)
    }

    /**
     * 根据已有配置信息接入某个wifi
     */
    fun addNetwork(config: WifiConfiguration): Boolean {
        val wifiInfo = getConnectionWifiInfo()
        if (wifiInfo != null) {
            mWifiManager?.disableNetwork(wifiInfo.networkId)
        }
        var result = false
        if (config.networkId > 0) {
            result = mWifiManager?.enableNetwork(config.networkId, true) ?: false
            mWifiManager?.updateNetwork(config)
        } else {
            val networkId = mWifiManager?.addNetwork(config) ?: -1
            if (networkId > 0) {
                mWifiManager?.saveConfiguration()
            }
            result = mWifiManager?.enableNetwork(networkId, true) ?: false
            mWifiManager?.reconnect()
        }
        return result
    }

    fun setOnWifiEnabled(onWifiEnabled: (Boolean) -> Unit) = apply {
        mOnWifiEnabled = onWifiEnabled
    }

    fun setOnScanResults(onScanResults: (scanResults: List<ScanResult>?) -> Unit) = apply {
        mOnScanResults = onScanResults
    }

    fun setOnWifiConnected(onWifiConnected: (WifiInfo?) -> Unit) = apply {
        mOnWifiConnected = onWifiConnected
    }
}

/**
 * wifi热点的加密类型
 */
enum class WifiCipherType {
    WEP,
    WPA,
    NO_PASSWORD,
    INVALID,
}

/**
 * 获取wifi的ssid
 */
fun ScanResult?.getSsid() = this?.run {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        wifiSsid.toString()
    } else {
        SSID
    }
} ?: ""

/**
 * 判断wifi热点支持的加密方式
 */
fun ScanResult?.getWifiCipher() = this?.run {
    when {
        capabilities.isEmpty() -> WifiCipherType.INVALID
        capabilities.contains("WEP") -> WifiCipherType.WEP
        capabilities.contains("WPA") || capabilities.contains("WPA2") || capabilities.contains("WPS") -> WifiCipherType.WPA
        else -> WifiCipherType.NO_PASSWORD
    }
} ?: WifiCipherType.NO_PASSWORD