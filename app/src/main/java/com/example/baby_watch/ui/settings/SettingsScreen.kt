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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baby_watch.R
import com.example.baby_watch.data.server.MobileServerRepository
import com.example.baby_watch.data.server.ServerConnectionStatus
import com.example.baby_watch.notification.EmergencyContact
import com.example.baby_watch.ui.theme.Background
import com.example.baby_watch.ui.theme.Primary
import com.example.baby_watch.ui.theme.PrimaryContainer
import com.example.baby_watch.ui.theme.SafeGreen
import com.example.baby_watch.ui.theme.SecondaryContainer
import com.example.baby_watch.ui.theme.TextPrimary
import com.example.baby_watch.ui.theme.TextSecondary
import com.example.baby_watch.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repositoryState by MobileServerRepository.state.collectAsState()
    val configuration = repositoryState.configuration
    var baseUrl by remember(configuration.baseUrl) { mutableStateOf(configuration.baseUrl) }
    var username by remember(configuration.username) { mutableStateOf(configuration.username) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var contact by remember { mutableStateOf(EmergencyContact.get(context)) }

    LaunchedEffect(Unit) {
        MobileServerRepository.ensureInitialized(context)
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "系统设置",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            "设备连接与告警联系",
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BabyProfileCard(
                deviceId = configuration.deviceId,
                online = repositoryState.snapshot.device.online,
            )
            ContactSection(
                contact = contact,
                onContactChange = {
                    contact = it
                    EmergencyContact.save(context, it)
                },
            )
            ServerConfigSection(
                baseUrl = baseUrl,
                deviceId = configuration.deviceId,
                username = username,
                password = password,
                passwordVisible = passwordVisible,
                hasSavedPassword = configuration.hasPassword,
                status = repositoryState.connectionStatus,
                statusDetail = repositoryState.errorMessage,
                onBaseUrlChange = { baseUrl = it },
                onUsernameChange = { username = it },
                onPasswordChange = { password = it },
                onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                onSave = {
                    MobileServerRepository.saveConfiguration(
                        context = context,
                        baseUrl = baseUrl,
                        username = username,
                        password = password,
                    )
                    password = ""
                    passwordVisible = false
                },
            )
        }
    }
}

@Composable
private fun BabyProfileCard(deviceId: String, online: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.baby_avatar),
                            contentDescription = "宝宝头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "小宝贝",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("服务端智能守护", fontSize = 14.sp, color = TextSecondary)
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.86f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "固定监护设备",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                            Text(deviceId, fontSize = 12.sp, color = TextSecondary)
                        }
                        Text(
                            if (online) "在线" else "离线",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (online) SafeGreen else TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerConfigSection(
    baseUrl: String,
    deviceId: String,
    username: String,
    password: String,
    passwordVisible: Boolean,
    hasSavedPassword: Boolean,
    status: ServerConnectionStatus,
    statusDetail: String?,
    onBaseUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityChange: () -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "监护服务端",
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                ConnectionBadge(status)
            }

            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text("HTTPS 服务端地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = deviceId,
                onValueChange = {},
                readOnly = true,
                label = { Text("固定设备 ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("服务端用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = {
                    Text(if (hasSavedPassword) "密码（留空则保持已保存密码）" else "服务端密码")
                },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityChange) {
                        Icon(
                            if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            statusDetail?.takeIf { status == ServerConnectionStatus.Error }?.let {
                Text(it, fontSize = 12.sp, color = Color(0xFFE05252))
            }
            Text(
                "密码使用 Android Keystore 加密保存；ESP32 设备密钥不会存入 App。",
                fontSize = 12.sp,
                color = TextTertiary,
            )
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(7.dp))
                Text("保存并连接", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ConnectionBadge(status: ServerConnectionStatus) {
    val text = when (status) {
        ServerConnectionStatus.NotConfigured -> "未配置"
        ServerConnectionStatus.Connecting -> "连接中"
        ServerConnectionStatus.Connected -> "已连接"
        ServerConnectionStatus.Error -> "连接异常"
    }
    val color = when (status) {
        ServerConnectionStatus.Connected -> SafeGreen
        ServerConnectionStatus.Connecting -> Primary
        ServerConnectionStatus.NotConfigured -> TextTertiary
        ServerConnectionStatus.Error -> Color(0xFFE05252)
    }
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun ContactSection(contact: String, onContactChange: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(contact) { mutableStateOf(contact) }

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onContactChange(draft.trim())
                        editing = false
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text("取消") }
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "告警联系人",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (contact.isBlank()) "尚未设置紧急联系人" else "紧急联系人",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        contact.ifBlank { "未设置" },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        if (contact.isBlank()) {
                            "设置后，三级至四级发送短信，五级自动拨号。"
                        } else {
                            "三级至四级发送短信，五级自动拨号。"
                        },
                        fontSize = 12.sp,
                        color = TextTertiary,
                    )
                }
                IconButton(
                    onClick = {
                        draft = contact
                        editing = true
                    }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Primary)
                }
            }
        }
    }
}
