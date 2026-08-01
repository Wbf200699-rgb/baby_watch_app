package com.example.baby_watch.ui.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baby_watch.data.server.MatrixFrame
import com.example.baby_watch.data.server.Bme688Summary
import com.example.baby_watch.data.server.MobileServerRepository
import com.example.baby_watch.data.server.PressureSummary
import com.example.baby_watch.data.server.Scd41Summary
import com.example.baby_watch.data.server.ServerConnectionStatus
import com.example.baby_watch.ui.theme.Background
import com.example.baby_watch.ui.theme.Level3Red
import com.example.baby_watch.ui.theme.Level1Yellow
import com.example.baby_watch.ui.theme.Primary
import com.example.baby_watch.ui.theme.PrimaryContainer
import com.example.baby_watch.ui.theme.SafeGreen
import com.example.baby_watch.ui.theme.Secondary
import com.example.baby_watch.ui.theme.TextPrimary
import com.example.baby_watch.ui.theme.TextSecondary
import com.example.baby_watch.ui.theme.TextTertiary
import kotlin.math.abs

private enum class MatrixPalette {
    Thermal,
    Pressure,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(modifier: Modifier = Modifier) {
    val repositoryState by MobileServerRepository.state.collectAsState()
    val thermalMatrix by MobileServerRepository.thermalMatrix.collectAsState()
    val pressureMatrix by MobileServerRepository.pressureMatrix.collectAsState()
    val snapshot = repositoryState.snapshot
    val deviceOnline = repositoryState.connectionStatus ==
        ServerConnectionStatus.Connected && snapshot.device.online

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Sensors,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                "传感器",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                        Text(
                            when {
                                repositoryState.connectionStatus != ServerConnectionStatus.Connected ->
                                    repositoryState.errorMessage ?: "正在连接服务端…"
                                snapshot.device.online -> "服务端实时同步 · 设备在线"
                                else -> "服务端已连接 · 设备离线"
                            },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FivePointTemperatureCard(
                values = snapshot.fivePointTemperaturesC,
                deviceOnline = deviceOnline,
            )
            EnvironmentSensorCard(
                scd41 = snapshot.scd41,
                bme688 = snapshot.bme688,
                bme688Supported = repositoryState.capabilities.bme688Supported,
                deviceOnline = deviceOnline,
            )
            MatrixImageCard(
                title = "热成像",
                description = "服务端热成像矩阵",
                icon = Icons.Default.DeviceThermostat,
                matrix = thermalMatrix,
                palette = MatrixPalette.Thermal,
                thermalCenterC = snapshot.thermalCenterC,
                deviceOnline = deviceOnline,
            )
            MatrixImageCard(
                title = "压力分布",
                description = "服务端压力矩阵",
                icon = Icons.Default.GridOn,
                matrix = pressureMatrix,
                palette = MatrixPalette.Pressure,
                pressure = snapshot.pressure,
                deviceOnline = deviceOnline,
            )
        }
    }
}

@Composable
private fun EnvironmentSensorCard(
    scd41: Scd41Summary,
    bme688: Bme688Summary,
    bme688Supported: Boolean,
    deviceOnline: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F7F1)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Sensors, null, tint = SafeGreen, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("环境监测", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        if (deviceOnline) "SCD41 CO₂ · BME688 环境参数" else "显示服务端最近一次环境数据",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }
            }

            Text("SCD41", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnvironmentValue(
                    Modifier.weight(1f), "CO₂",
                    scd41.co2Ppm?.let { "$it ppm" } ?: "--",
                    scd41.valid,
                )
                EnvironmentValue(
                    Modifier.weight(1f), "温度",
                    scd41.temperatureC?.let { "%.1f°C".format(it) } ?: "--",
                    scd41.valid,
                )
                EnvironmentValue(
                    Modifier.weight(1f), "湿度",
                    scd41.humidityPercent?.let { "%.1f%%".format(it) } ?: "--",
                    scd41.valid,
                )
            }

            Text("BME688", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            if (!bme688Supported && !bme688.supported) {
                Text("当前服务端未声明 BME688 能力", fontSize = 12.sp, color = TextTertiary)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EnvironmentValue(
                        Modifier.weight(1f), "温度",
                        bme688.temperatureC?.let { "%.1f°C".format(it) } ?: "--",
                        bme688.climateValid,
                    )
                    EnvironmentValue(
                        Modifier.weight(1f), "湿度",
                        bme688.humidityPercent?.let { "%.1f%%".format(it) } ?: "--",
                        bme688.climateValid,
                    )
                    EnvironmentValue(
                        Modifier.weight(1f), "气压",
                        bme688.pressureHpa?.let { "%.1f hPa".format(it) } ?: "--",
                        bme688.climateValid,
                    )
                }
                EnvironmentValue(
                    Modifier.fillMaxWidth(),
                    "原始气阻（非 VOC/IAQ）",
                    bme688.rawGasKohm?.let { "%.1f kΩ".format(it) } ?: "--",
                    bme688.gasValid,
                )
            }
        }
    }
}

@Composable
private fun EnvironmentValue(
    modifier: Modifier,
    label: String,
    value: String,
    valid: Boolean,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (valid) Color(0xFFF0F9F5) else Color(0xFFF5F6F8))
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(label, fontSize = 10.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (valid) TextPrimary else TextTertiary)
    }
}

@Composable
private fun FivePointTemperatureCard(values: List<Float?>, deviceOnline: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.DeviceThermostat,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "五点温度",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        if (deviceOnline) {
                            "四角与中心位置 · 实时"
                        } else {
                            "四角与中心位置 · 最近一次数据"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.65f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFF8FBFF), Color(0xFFFFF8EE))
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFC9D6E5),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                TemperaturePoint(
                    modifier = Modifier.align(Alignment.TopStart),
                    label = "T1 左上",
                    value = values.getOrNull(0),
                )
                TemperaturePoint(
                    modifier = Modifier.align(Alignment.TopEnd),
                    label = "T2 右上",
                    value = values.getOrNull(1),
                )
                TemperaturePoint(
                    modifier = Modifier.align(Alignment.Center),
                    label = "T3 中心",
                    value = values.getOrNull(2),
                )
                TemperaturePoint(
                    modifier = Modifier.align(Alignment.BottomStart),
                    label = "T4 左下",
                    value = values.getOrNull(3),
                )
                TemperaturePoint(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    label = "T5 右下",
                    value = values.getOrNull(4),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (deviceOnline) {
                    "点位顺序对应服务端 t_mc[0..4]；温度阈值需按实际安装位置标定。"
                } else {
                    "设备当前离线，以上为服务端保留的最近一次数据。"
                },
                fontSize = 11.sp,
                color = TextTertiary,
            )
        }
    }
}

@Composable
private fun BoxScope.TemperaturePoint(
    modifier: Modifier,
    label: String,
    value: Float?,
) {
    Column(
        modifier = modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(temperatureColor(value))
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value?.let { "%.1f°C".format(it) } ?: "--",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (value == null) TextSecondary else Color.White,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
        )
    }
}

@Composable
private fun MatrixImageCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    matrix: MatrixFrame?,
    palette: MatrixPalette,
    thermalCenterC: Float? = null,
    pressure: PressureSummary? = null,
    deviceOnline: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (palette == MatrixPalette.Thermal) {
                                Color(0xFFFFEEE7)
                            } else {
                                Color(0xFFEAF2FF)
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (palette == MatrixPalette.Thermal) Secondary else Primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        matrixSummary(description, matrix, palette, deviceOnline),
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                matrix == null || !deviceOnline -> TextTertiary
                                matrix.delayed -> Level1Yellow
                                else -> SafeGreen
                            }
                        )
                )
            }

            Spacer(Modifier.height(14.dp))
            if (matrix == null || matrix.rows <= 0 || matrix.cols <= 0 || matrix.values.isEmpty()) {
                EmptyMatrixPlaceholder(palette)
            } else {
                MatrixCanvas(matrix = matrix, palette = palette)
                Spacer(Modifier.height(10.dp))
                MatrixLegend(matrix = matrix, palette = palette)
                Text(
                    "${if (deviceOnline) "实时序列" else "最近序列"} ${matrix.sequence} · ${
                        matrix.receivedAt.ifBlank { "等待时间戳" }
                    }",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 11.sp,
                    color = TextTertiary,
                )
                Text(
                    buildString {
                        matrix.fps?.let { append("%.2f FPS".format(it)) }
                        matrix.frameAgeMs?.let {
                            if (isNotEmpty()) append(" · ")
                            append("延迟 ${it} ms")
                        }
                        if (matrix.drops > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("丢帧 ${matrix.drops}")
                        }
                        if (matrix.delayed) {
                            if (isNotEmpty()) append(" · ")
                            append("数据延迟")
                        }
                    }.ifBlank { "等待帧质量指标" },
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = if (matrix.delayed) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (matrix.delayed) Level1Yellow else TextTertiary,
                )
                if (palette == MatrixPalette.Thermal) {
                    Spacer(Modifier.height(12.dp))
                    ThermalTemperatureSummary(
                        matrix = matrix,
                        serverCenterC = thermalCenterC,
                    )
                } else if (pressure != null) {
                    Spacer(Modifier.height(12.dp))
                    PressureSignalSummary(pressure)
                }
            }
        }
    }
}

@Composable
private fun PressureSignalSummary(pressure: PressureSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EnvironmentValue(
            Modifier.weight(1f),
            "总压力",
            pressure.total?.toString() ?: "--",
            pressure.total != null,
        )
        EnvironmentValue(
            Modifier.weight(1f),
            "边缘压力",
            pressure.edge?.toString() ?: "--",
            pressure.edge != null,
        )
        EnvironmentValue(
            Modifier.weight(1f),
            "有效点",
            pressure.activePoints?.toString() ?: "--",
            pressure.activePoints != null,
        )
    }
}

@Composable
private fun ThermalTemperatureSummary(
    matrix: MatrixFrame,
    serverCenterC: Float?,
) {
    val values = matrix.values.take(matrix.rows * matrix.cols)
    val minimumC = values.minOrNull()?.let { thermalValueC(it, matrix.unit) }
    val maximumC = values.maxOrNull()?.let { thermalValueC(it, matrix.unit) }
    val centerIndex = if (matrix.rows > 0 && matrix.cols > 0) {
        (matrix.rows / 2) * matrix.cols + (matrix.cols / 2)
    } else {
        -1
    }
    val matrixCenterC = values.getOrNull(centerIndex)?.let { thermalValueC(it, matrix.unit) }
    val centerC = serverCenterC ?: matrixCenterC

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThermalTemperatureItem(
            modifier = Modifier.weight(1f),
            label = "最高温度",
            value = maximumC,
            color = Level3Red,
            background = Color(0xFFFFECEA),
        )
        ThermalTemperatureItem(
            modifier = Modifier.weight(1f),
            label = "中心温度",
            value = centerC,
            color = Secondary,
            background = Color(0xFFFFF3E8),
        )
        ThermalTemperatureItem(
            modifier = Modifier.weight(1f),
            label = "最低温度",
            value = minimumC,
            color = Primary,
            background = Color(0xFFEAF2FF),
        )
    }
}

@Composable
private fun ThermalTemperatureItem(
    modifier: Modifier,
    label: String,
    value: Float?,
    color: Color,
    background: Color,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value?.let { "%.1f°C".format(it) } ?: "--",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
        )
    }
}

@Composable
private fun EmptyMatrixPlaceholder(palette: MatrixPalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (palette == MatrixPalette.Thermal) 1.25f else 1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF4F6F9))
            .border(1.dp, Color(0xFFD8DEE8), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (palette == MatrixPalette.Thermal) {
                    Icons.Default.DeviceThermostat
                } else {
                    Icons.Default.GridOn
                },
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(38.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text("等待服务端矩阵数据", fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun MatrixCanvas(
    matrix: MatrixFrame,
    palette: MatrixPalette,
) {
    val expected = matrix.rows * matrix.cols
    val values = matrix.values.take(expected)
    val minimum = values.minOrNull() ?: 0
    val maximum = values.maxOrNull() ?: minimum
    val range = (maximum - minimum).toFloat()
    val aspect = (matrix.cols.toFloat() / matrix.rows.toFloat()).coerceIn(0.8f, 2f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEDF1F6))
            .border(1.dp, Color(0xFFD8DEE8), RoundedCornerShape(14.dp)),
    ) {
        val cellWidth = size.width / matrix.cols
        val cellHeight = size.height / matrix.rows
        values.forEachIndexed { index, value ->
            val row = index / matrix.cols
            val column = index % matrix.cols
            if (row >= matrix.rows) return@forEachIndexed
            val normalized = if (range == 0f) {
                if (value == 0) 0f else 0.5f
            } else {
                ((value - minimum) / range).coerceIn(0f, 1f)
            }
            drawRect(
                color = matrixColor(normalized, palette),
                topLeft = Offset(column * cellWidth, row * cellHeight),
                size = Size(cellWidth + 0.5f, cellHeight + 0.5f),
            )
        }
        if (palette == MatrixPalette.Pressure && matrix.rows <= 32 && matrix.cols <= 32) {
            val gridColor = Color.White.copy(alpha = 0.72f)
            for (column in 1 until matrix.cols) {
                val x = column * cellWidth
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                )
            }
            for (row in 1 until matrix.rows) {
                val y = row * cellHeight
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
            }
        }
    }
}

@Composable
private fun MatrixLegend(
    matrix: MatrixFrame,
    palette: MatrixPalette,
) {
    val minimum = matrix.minimum ?: matrix.values.minOrNull() ?: 0
    val maximum = matrix.maximum ?: matrix.values.maxOrNull() ?: minimum
    val startLabel: String
    val endLabel: String
    if (palette == MatrixPalette.Thermal) {
        startLabel = formatThermalValue(minimum, matrix.unit)
        endLabel = formatThermalValue(maximum, matrix.unit)
    } else {
        startLabel = "低压力 $minimum"
        endLabel = "高压力 $maximum"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(startLabel, fontSize = 11.sp, color = TextSecondary)
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        if (palette == MatrixPalette.Thermal) {
                            listOf(
                                Color(0xFF3157D5),
                                Color(0xFF38C7C1),
                                Color(0xFFFFD166),
                                Color(0xFFE53935),
                            )
                        } else {
                            listOf(
                                Color(0xFFF0F4FA),
                                Color(0xFF6CA6FF),
                                Color(0xFF6750A4),
                                Color(0xFFD81B60),
                            )
                        }
                    )
                )
        )
        Text(endLabel, fontSize = 11.sp, color = TextSecondary)
    }
}

private fun matrixSummary(
    description: String,
    matrix: MatrixFrame?,
    palette: MatrixPalette,
    deviceOnline: Boolean,
): String {
    if (matrix == null) return "$description · 等待数据"
    val status = when {
        !deviceOnline -> "最近一次"
        matrix.delayed -> "数据延迟"
        else -> "实时"
    }
    return if (palette == MatrixPalette.Pressure) {
        "$status · ${matrix.rows}×${matrix.cols} · ${matrix.activePoints} 个受压点"
    } else {
        "$status · ${matrix.rows}×${matrix.cols} · ${matrix.unit.ifBlank { "温度" }}"
    }
}

private fun matrixColor(normalized: Float, palette: MatrixPalette): Color {
    return when (palette) {
        MatrixPalette.Thermal -> when {
            normalized < 0.34f -> lerp(
                Color(0xFF3157D5),
                Color(0xFF38C7C1),
                normalized / 0.34f,
            )

            normalized < 0.68f -> lerp(
                Color(0xFF38C7C1),
                Color(0xFFFFD166),
                (normalized - 0.34f) / 0.34f,
            )

            else -> lerp(
                Color(0xFFFFD166),
                Color(0xFFE53935),
                (normalized - 0.68f) / 0.32f,
            )
        }

        MatrixPalette.Pressure -> when {
            normalized < 0.4f -> lerp(
                Color(0xFFDDE7F4),
                Color(0xFF6CA6FF),
                normalized / 0.4f,
            )

            normalized < 0.75f -> lerp(
                Color(0xFF6CA6FF),
                Color(0xFF6750A4),
                (normalized - 0.4f) / 0.35f,
            )

            else -> lerp(
                Color(0xFF6750A4),
                Color(0xFFD81B60),
                (normalized - 0.75f) / 0.25f,
            )
        }
    }
}

private fun temperatureColor(value: Float?): Color = when {
    value == null -> Color(0xFFE5E9F0)
    value < 20f -> Color(0xFF4F80E1)
    value < 30f -> Color(0xFF24A68A)
    value < 38f -> Color(0xFFFF9F43)
    else -> Level3Red
}

private fun formatThermalValue(value: Int, unit: String): String {
    return "%.1f°C".format(thermalValueC(value, unit))
}

private fun thermalValueC(value: Int, unit: String): Float {
    val normalizedUnit = unit.trim().lowercase()
    return when {
        normalizedUnit in setOf("mc", "millidegree_c", "millicelsius") -> value / 1000f
        normalizedUnit in setOf("cd", "cdeg", "centidegree_c", "centicelsius") -> value / 100f
        normalizedUnit in setOf("dc", "ddeg", "deci_c", "decidegree_c", "decicelsius") -> value / 10f
        normalizedUnit in setOf("c", "°c", "celsius") -> value.toFloat()
        abs(value) >= 1000 -> value / 100f
        abs(value) >= 100 -> value / 10f
        else -> value.toFloat()
    }
}
