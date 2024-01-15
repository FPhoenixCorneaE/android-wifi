package com.fphoenixcorneae.android_wifi

import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.fphoenixcorneae.android_wifi.ui.theme.AndroidwifiTheme
import com.fphoenixcorneae.compose.clickableNoRipple
import com.fphoenixcorneae.widget.Switch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val isWifiEnabled = MutableStateFlow(false)
    private val wifiScanResults = MutableStateFlow<List<ScanResult>?>(null)
    private val wifiConnected = MutableStateFlow<WifiInfo?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidwifiTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFDDDDDD)) {
                    val wifiEnabled by isWifiEnabled.collectAsState()
                    val scanResults by wifiScanResults.collectAsState()
                    val connectedWifi by wifiConnected.collectAsState()
                    var selectedWifi by remember { mutableStateOf<ScanResult?>(null) }
                    var showConnectWifiDialog by remember {
                        mutableStateOf(false)
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 16.dp)
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "WLAN",
                                fontSize = 16.sp,
                                color = Color.Black,
                            )
                            Box(contentAlignment = Alignment.CenterEnd) {
                                Switch(
                                    checked = wifiEnabled,
                                )
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .fillMaxHeight()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    WifiManagerImpl.enabledWifi(this@MainActivity, !wifiEnabled)
                                                }
                                            )
                                        },
                                ) {}
                            }
                        }
                        scanResults?.firstOrNull {
                            // 筛选已连接的wifi
                            it.getSsid() == connectedWifi?.ssid?.replace("\"", "")
                        }?.let {
                            Text(
                                text = "已连接WLAN",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 20.dp)
                            )
                            WifiItem(scanResult = it, isConnected = true) {

                            }
                        }
                        Text(
                            text = "可用WLAN",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 20.dp)
                        )
                        scanResults?.filter {
                            // 剔除已连接的wifi
                            it.getSsid() != connectedWifi?.ssid?.replace("\"", "")
                        }?.let { results ->
                            LazyColumn(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth()
                                    .height(0.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White),
                            ) {
                                items(results.size) {
                                    Column {
                                        WifiItem(results.getOrNull(it)) {
                                            selectedWifi = results.getOrNull(it)
                                            if (selectedWifi?.getWifiCipher() == WifiCipherType.NO_PASSWORD) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    WifiManagerImpl.addNetworkSuggestion(selectedWifi, null)
                                                } else {
                                                    WifiManagerImpl.addNetwork(
                                                        WifiManagerImpl.createWifiConfiguration(
                                                            selectedWifi?.getSsid(),
                                                            null,
                                                            selectedWifi?.getWifiCipher()
                                                        )
                                                    )
                                                }
                                            } else {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                    WifiManagerImpl.getNetworkSuggestions()
                                                        ?.filter { it.ssid == selectedWifi.getSsid() }
                                                        ?.let {
                                                            WifiManagerImpl.addNetworkSuggestion(
                                                                selectedWifi,
                                                                null,
                                                                it.firstOrNull()
                                                            )
                                                        }
                                                        ?: run {
                                                            showConnectWifiDialog = true
                                                        }
                                                } else {
                                                    showConnectWifiDialog = true
                                                }
                                            }
                                        }
                                        if (it < results.size - 1) {
                                            Divider(
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                thickness = 0.5.dp,
                                                color = Color.Gray.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (showConnectWifiDialog) {
                        ConnectWifiDialog(
                            ssid = selectedWifi.getSsid(),
                            onDismissRequest = {
                                showConnectWifiDialog = false
                            }
                        ) { pws ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                WifiManagerImpl.addNetworkSuggestion(selectedWifi, pws)
                            }
                        }
                    }
                }
            }
        }
        WifiManagerImpl.registerWifiBroadcastReceiver(this)
            .setOnWifiEnabled {
                lifecycleScope.launch {
                    isWifiEnabled.emit(it)
                }
            }
            .setOnScanResults {
                lifecycleScope.launch {
                    wifiScanResults.emit(it)
                }
                Log.d("WifiManagerImpl", "onCreate: $it")
            }
            .setOnWifiConnected {
                lifecycleScope.launch {
                    wifiConnected.emit(it)
                }
            }
    }

    @Composable
    private fun WifiItem(scanResult: ScanResult?, isConnected: Boolean = false, onClick: () -> Unit = {}) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (isConnected) 16.dp else 0.dp))
                .background(if (isConnected) Color.White else Color.Transparent)
                .padding(if (isConnected) 10.dp else 0.dp)
                .clickableNoRipple {
                    onClick()
                },
        ) {
            Column {
                // wifi名称
                Text(
                    text = scanResult.getSsid(),
                    fontSize = 16.sp,
                    color = if (isConnected) Color.Blue else Color.Black,
                    fontWeight = FontWeight.W500,
                )
                val cipherType = when (scanResult?.getWifiCipher()) {
                    WifiCipherType.WEP, WifiCipherType.WPA -> "加密"
                    WifiCipherType.NO_PASSWORD -> "开放"
                    else -> ""
                }
                // 是否加密
                Text(
                    text = if (isConnected) "已连接" else cipherType,
                    fontSize = 15.sp,
                    color = Color.Gray,
                )
            }
            val wifiResId =
                when (WifiManager.calculateSignalLevel(
                    scanResult?.level ?: Int.MIN_VALUE, 5
                )) {
                    4 -> R.mipmap.ic_wifi_signal_4
                    3 -> R.mipmap.ic_wifi_signal_3
                    2 -> R.mipmap.ic_wifi_signal_2
                    1 -> R.mipmap.ic_wifi_signal_1
                    else -> R.mipmap.ic_wifi_signal_0
                }
            Box {
                // 信号强度
                Image(
                    painter = painterResource(id = wifiResId),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                // 是否加密
                if (scanResult?.getWifiCipher() == WifiCipherType.WEP
                    || scanResult?.getWifiCipher() == WifiCipherType.WPA
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_password),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.5.dp, bottom = 5.dp)
                            .size(10.dp)
                            .align(Alignment.BottomEnd),
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WifiManagerImpl.unregisterWifiBroadcastReceiver(this)
    }
}