package com.fphoenixcorneae.android_wifi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fphoenixcorneae.compose.clickableNoRipple
import com.fphoenixcorneae.widget.CustomEditText

/**
 * 连接Wifi弹窗
 */
@Preview
@Composable
fun ConnectWifiDialog(
    ssid: String = "",
    onDismissRequest: () -> Unit = {},
    onConnectWifi: (String) -> Unit = { },
) {
    Dialog(
        onDismissRequest = {
            onDismissRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xff7f7f7f).copy(0.4f))
                .clickableNoRipple {
                    onDismissRequest()
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(16.dp)
                    .clickableNoRipple {
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_close),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.End)
                        .clickableNoRipple {
                            onDismissRequest()
                        },
                )
                Text(
                    text = ssid,
                    color = Color(0xff666666),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.padding(top = 4.dp)
                )
                var wifiPws by remember {
                    mutableStateOf("")
                }
                var wifiPwsShow by remember { mutableStateOf(true) }
                CustomEditText(
                    text = wifiPws,
                    onValueChange = { wifiPws = it },
                    paddingStart = 16.dp,
                    paddingEnd = 8.dp,
                    endIcon = if (wifiPwsShow) R.mipmap.ic_password_show else R.mipmap.ic_password_hide,
                    endIconSize = 20.dp,
                    onEndIconClick = {
                        wifiPwsShow = !wifiPwsShow
                    },
                    visualTransformation = if (wifiPwsShow) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .padding(start = 30.dp, top = 20.dp, end = 30.dp)
                        .fillMaxWidth()
                        .height(36.dp)
                        .border(1.dp, Color(0xfff5f5f5), RoundedCornerShape(36.dp))
                        .padding(end = 16.dp)
                )
                Button(
                    onClick = {
                        onConnectWifi(wifiPws)
                        onDismissRequest()
                    },
                    shape = RoundedCornerShape(36.dp),
                    border = BorderStroke(0.5.dp, Color(0xffccad89)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xffff9527)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 30.dp, top = 20.dp, end = 30.dp)
                        .fillMaxWidth()
                        .height(36.dp)

                ) {
                    Text(
                        text = "连接到网络",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500,
                    )
                }
            }
        }
    }
}