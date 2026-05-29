package com.example.baby_watch.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baby_watch.R
import com.example.baby_watch.data.ip.ip_ok
import com.example.baby_watch.data.repository.Esp32CamConfig
import com.example.baby_watch.data.repository.Esp32CamManager
import com.example.baby_watch.data.repository.receive_json
import com.example.baby_watch.device.mic.Esp32AudioManager
import com.example.baby_watch.notification.EmergencyContact
import com.example.baby_watch.ui.theme.Background
import com.example.baby_watch.ui.theme.Baby_watchTheme
import com.example.baby_watch.ui.theme.Primary
import com.example.baby_watch.ui.theme.PrimaryContainer
import com.example.baby_watch.ui.theme.SafeGreen
import com.example.baby_watch.ui.theme.SecondaryContainer
import com.example.baby_watch.ui.theme.TextPrimary
import com.example.baby_watch.ui.theme.TextSecondary
import com.example.baby_watch.ui.theme.TextTertiary

@Composable
private fun BabyProfileCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFE4F5FF), PrimaryContainer, SecondaryContainer)
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.baby_avatar),
                            contentDescription = "小宝贝头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("小宝贝", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("10个月20天", fontSize = 14.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(
                            "健康成长中",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.72f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.86f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("设备状态", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("设备ID：BABY-RA8", fontSize = 12.sp, color = TextSecondary)
                        }
                        Text("在线", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    ip_ok.load(context)
    var camHost by remember { mutableStateOf(Esp32CamConfig.getSavedHost(context)) }
    var routerIp by remember { mutableStateOf(ip_ok.routerIp) }
    var routerPort by remember { mutableStateOf(ip_ok.routerPort.toString()) }
    var contact by remember { mutableStateOf(EmergencyContact.get(context)) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("系统设置", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("网络配置与紧急联系人", fontSize = 13.sp, color = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BabyProfileCard()
            NetworkConfigSection(
                camHost = camHost,
                targetIp = routerIp,
                targetPort = routerPort,
                onCamHostChange = { camHost = it },
                onTargetIpChange = { routerIp = it },
                onTargetPortChange = { routerPort = it.filter(Char::isDigit) },
                onSave = {
                    Esp32CamConfig.saveHost(context, camHost)
                    Esp32CamManager.connect(camHost)
                    Esp32AudioManager.connect(camHost)
                    val port = routerPort.toIntOrNull() ?: ip_ok.routerPort
                    ip_ok.save(context, routerIp, port)
                    routerPort = port.toString()
                    receive_json.restart()
                }
            )
            ContactSection(contact = contact, onContactChange = {
                contact = it
                EmergencyContact.save(context, it)
            })
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
}

@Composable
private fun NetworkConfigSection(
    camHost: String,
    targetIp: String,
    targetPort: String,
    onCamHostChange: (String) -> Unit,
    onTargetIpChange: (String) -> Unit,
    onTargetPortChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Router, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("网络配置", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NetworkValueField(
                    modifier = Modifier.weight(1f),
                    label = "ESP32-CAM",
                    value = camHost,
                    onValueChange = onCamHostChange
                )
                NetworkValueField(
                    modifier = Modifier.weight(1f),
                    label = "目标 IP",
                    value = targetIp,
                    onValueChange = onTargetIpChange
                )
                NetworkValueField(
                    modifier = Modifier.weight(1f),
                    label = "目标端口",
                    value = targetPort,
                    onValueChange = onTargetPortChange,
                    keyboardType = KeyboardType.Number
                )
            }
            SaveButton(onClick = onSave, text = "保存并连接")
        }
    }
}

@Composable
private fun NetworkValueField(
    modifier: Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFBFAFF))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(22.dp)
            )
        }
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit, text: String) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primary)
    ) {
        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(7.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ContactSection(contact: String, onContactChange: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(contact) }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("编辑紧急联系人") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("手机号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onContactChange(draft)
                    editing = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text("取消") }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("紧急联系人")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("三级告警优先联系", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(contact.ifBlank { "未设置" }, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("请确保号码可接收短信或电话提醒。", fontSize = 12.sp, color = TextTertiary)
                }
                IconButton(onClick = {
                    draft = contact
                    editing = true
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Primary)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    Baby_watchTheme { SettingsScreen() }
}
