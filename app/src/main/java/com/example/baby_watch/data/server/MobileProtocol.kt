package com.example.baby_watch.data.server

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

const val DEFAULT_SERVER_URL = "https://renesas.zyxserver.online"
const val FIXED_DEVICE_ID = "esp32cam-01"
const val MOBILE_PROTOCOL_VERSION = 1

enum class ServerConnectionStatus {
    NotConfigured,
    Connecting,
    Connected,
    Error,
}

data class ServerConfigurationView(
    val baseUrl: String = DEFAULT_SERVER_URL,
    val deviceId: String = FIXED_DEVICE_ID,
    val username: String = "",
    val hasPassword: Boolean = false,
)

internal data class ServerCredentials(
    val baseUrl: String,
    val deviceId: String,
    val username: String,
    val password: String,
) {
    val isComplete: Boolean
        get() = baseUrl.startsWith("https://") &&
            deviceId.isNotBlank() &&
            username.isNotBlank() &&
            password.isNotBlank()
}

data class DeviceSummary(
    val id: String = FIXED_DEVICE_ID,
    val online: Boolean = false,
    val firmware: String = "",
    val hardware: String = "",
    val wifiRssi: Int? = null,
    val lastSeen: String = "",
    val matrixConnected: Boolean = false,
)

data class MobileCapabilities(
    val aiEnabled: Boolean = false,
    val bme688Supported: Boolean = false,
    val matrixKinds: Set<String> = emptySet(),
    val commands: Set<String> = emptySet(),
)

data class Scd41Summary(
    val supported: Boolean = false,
    val valid: Boolean = false,
    val status: Int = 0,
    val co2Ppm: Int? = null,
    val temperatureC: Float? = null,
    val humidityPercent: Float? = null,
    val sampleAgeMs: Long? = null,
    val crcErrors: Int = 0,
    val i2cErrors: Int = 0,
)

data class Bme688Summary(
    val supported: Boolean = false,
    val climateValid: Boolean = false,
    val gasValid: Boolean = false,
    val status: Int = 0,
    val address: Int? = null,
    val temperatureC: Float? = null,
    val humidityPercent: Float? = null,
    val pressureHpa: Float? = null,
    val rawGasKohm: Float? = null,
    val sampleAgeMs: Long? = null,
    val errorCount: Int = 0,
)

data class PressureSummary(
    val occupied: Boolean? = null,
    val total: Long? = null,
    val peak: Int? = null,
    val edge: Long? = null,
    val activePoints: Int? = null,
    val centerX100: Int? = null,
    val centerY100: Int? = null,
)

data class InferenceSummary(
    val state: Int = 0,
    val candidate: Int = 0,
    val confidence: Float = 0f,
    val severity: Int = 0,
    val faults: Int = 0,
) {
    val stateLabel: String
        get() = safetyStateLabel(state)
}

data class AudioPlayerSummary(
    val status: Int = 0,
    val track: Int = 0,
    val trackCount: Int = 0,
    val volume: Int = 0,
    val file: String = "",
) {
    val isPlaying: Boolean
        get() = status == 5

    val isPaused: Boolean
        get() = status == 7
}

data class VisionOverlayBox(
    val classId: Int,
    val label: String,
    val scoreX1000: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

data class VisionOverlaySummary(
    val enabled: Boolean = false,
    val stale: Boolean = true,
    val expired: Boolean = true,
    val calibrationStatus: String = "identity_unverified",
    val sourceSequence: Long = 0,
    val sourceAgeMs: Long = 0,
    val boxes: List<VisionOverlayBox> = emptyList(),
)

data class MonitorSnapshot(
    val serverTime: String = "",
    val device: DeviceSummary = DeviceSummary(),
    val fivePointTemperaturesC: List<Float?> = List(5) { null },
    val thermalCenterC: Float? = null,
    val pressureActivePoints: Int? = null,
    val pressure: PressureSummary = PressureSummary(),
    val scd41: Scd41Summary = Scd41Summary(),
    val bme688: Bme688Summary = Bme688Summary(),
    val inference: InferenceSummary = InferenceSummary(),
    val diagnosticsOverall: String = "offline",
    val videoAvailable: Boolean = false,
    val audioAvailable: Boolean = false,
    val audioPlayer: AudioPlayerSummary = AudioPlayerSummary(),
    val visionOverlay: VisionOverlaySummary = VisionOverlaySummary(),
)

data class MatrixFrame(
    val kind: String,
    val sequence: Long,
    val rows: Int,
    val cols: Int,
    val unit: String,
    val receivedAt: String,
    val minimum: Int? = null,
    val maximum: Int? = null,
    val fps: Float? = null,
    val frameAgeMs: Long? = null,
    val drops: Long = 0,
    val delayed: Boolean = false,
    val values: List<Int>,
) {
    val activePoints: Int
        get() = values.count { it > 0 }
}

data class AiStory(
    val storyId: String,
    val status: String,
    val title: String,
    val storyText: String,
    val audioSizeBytes: Long,
    val audioDurationMs: Long,
    val commandId: Long?,
    val error: String,
    val createdAt: String,
    val updatedAt: String,
) {
    val canReplay: Boolean
        get() = status in setOf("ready", "queued_for_device", "queued_for_playback")
}

data class AiAdvisory(
    val advisoryId: String,
    val level: String,
    val title: String,
    val summary: String,
    val evidence: List<String>,
    val actions: List<String>,
    val confidence: Float,
    val requiresUserAttention: Boolean,
    val sourceSequence: Long,
    val acknowledged: Boolean,
    val createdAt: String,
)

data class SafetyEvent(
    val id: Long,
    val sourceEventId: Long?,
    val eventType: String,
    val state: String,
    val severity: Int,
    val confidence: Float,
    val receivedAt: String,
    val acknowledged: Boolean,
    val payload: JSONObject? = null,
) {
    val title: String
        get() {
            val numericState = state.toIntOrNull()
            return if (numericState != null) safetyStateLabel(numericState)
            else eventType.replace('_', ' ').ifBlank { "安全事件" }
        }

    val detail: String
        get() = "严重度 $severity · 置信度 ${"%.1f".format(confidence)}%"
}

data class MobileCommandStatus(
    val id: Long,
    val action: String,
    val status: String,
    val result: JSONObject?,
)

data class MonitorRepositoryState(
    val configuration: ServerConfigurationView = ServerConfigurationView(),
    val capabilities: MobileCapabilities = MobileCapabilities(),
    val connectionStatus: ServerConnectionStatus = ServerConnectionStatus.NotConfigured,
    val snapshot: MonitorSnapshot = MonitorSnapshot(),
    val errorMessage: String? = null,
)

data class VideoStreamState(
    val wanted: Boolean = false,
    val connected: Boolean = false,
    val sequence: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val quality: Int = 0,
    val frameCount: Long = 0,
    val gapCount: Long = 0,
    val frameBytes: ByteArray? = null,
    val errorMessage: String? = null,
)

data class AudioStreamState(
    val wanted: Boolean = false,
    val connected: Boolean = false,
    val receiving: Boolean = false,
    val muted: Boolean = false,
    val sequence: Long = 0,
    val errorMessage: String? = null,
)

data class S3MediaFrame(
    val mediaType: Int,
    val flags: Int,
    val sequence: Long,
    val timestampUs: Long,
    val meta0: Int,
    val meta1: Int,
    val meta2: Int,
    val meta3: Int,
    val payload: ByteArray,
) {
    val discontinuity: Boolean
        get() = flags and 1 != 0
}

object S3MediaProtocol {
    const val HEADER_BYTES = 32
    const val TYPE_JPEG = 1
    const val TYPE_PCM16 = 2
    private const val MAX_JPEG_BYTES = 512 * 1024

    fun decode(message: ByteArray): S3MediaFrame {
        require(message.size >= HEADER_BYTES) { "S3MD frame is shorter than 32 bytes" }
        require(
            message[0] == 'S'.code.toByte() &&
                message[1] == '3'.code.toByte() &&
                message[2] == 'M'.code.toByte() &&
                message[3] == 'D'.code.toByte()
        ) { "Invalid S3MD magic" }

        val buffer = ByteBuffer.wrap(message).order(ByteOrder.BIG_ENDIAN)
        buffer.position(4)
        val version = buffer.get().toInt() and 0xFF
        require(version == 1) { "Unsupported S3MD version $version" }
        val mediaType = buffer.get().toInt() and 0xFF
        require(mediaType == TYPE_JPEG || mediaType == TYPE_PCM16) {
            "Unsupported S3MD media type $mediaType"
        }
        val flags = buffer.short.toInt() and 0xFFFF
        val sequence = buffer.int.toLong() and 0xFFFF_FFFFL
        val timestampUs = buffer.long
        val payloadLength = buffer.int
        val meta0 = buffer.short.toInt() and 0xFFFF
        val meta1 = buffer.short.toInt() and 0xFFFF
        val meta2 = buffer.short.toInt() and 0xFFFF
        val meta3 = buffer.short.toInt() and 0xFFFF
        require(payloadLength >= 0 && message.size == HEADER_BYTES + payloadLength) {
            "S3MD payload length mismatch"
        }
        return S3MediaFrame(
            mediaType = mediaType,
            flags = flags,
            sequence = sequence,
            timestampUs = timestampUs,
            meta0 = meta0,
            meta1 = meta1,
            meta2 = meta2,
            meta3 = meta3,
            payload = message.copyOfRange(HEADER_BYTES, message.size),
        )
    }

    fun decodeVideo(message: ByteArray): S3MediaFrame {
        val frame = decode(message)
        require(frame.mediaType == TYPE_JPEG) { "S3MD frame is not JPEG video" }
        require(frame.flags and 0xFFFE == 0) { "Unsupported S3MD video flags" }
        require(frame.meta0 in 1..4096 && frame.meta1 in 1..4096) {
            "Invalid S3MD video dimensions"
        }
        require(frame.meta2 in 0..63 && frame.meta3 == 0) {
            "Invalid S3MD JPEG metadata"
        }
        require(frame.payload.size in 4..MAX_JPEG_BYTES) {
            "Invalid S3MD JPEG payload length"
        }
        require(
            frame.payload[0] == 0xFF.toByte() &&
                frame.payload[1] == 0xD8.toByte() &&
                frame.payload[frame.payload.lastIndex - 1] == 0xFF.toByte() &&
                frame.payload[frame.payload.lastIndex] == 0xD9.toByte()
        ) { "S3MD JPEG is missing SOI/EOI markers" }
        return frame
    }
}

object MobileProtocolParser {
    fun parseCapabilities(json: String): MobileCapabilities {
        val root = JSONObject(json)
        val version = root.optInt("protocol_version", 0)
        require(version == MOBILE_PROTOCOL_VERSION) {
            "服务端协议版本为 $version，客户端仅支持 v$MOBILE_PROTOCOL_VERSION"
        }
        val matrixKinds = root.optJSONArray("matrix_kinds").stringSet()
        val commands = buildSet {
            val raw = root.optJSONArray("commands")
            if (raw != null) {
                for (index in 0 until raw.length()) {
                    raw.optJSONObject(index)?.optString("action")
                        ?.takeIf(String::isNotBlank)
                        ?.let(::add)
                }
            }
        }
        val bme = root.objectOrNull("environment_sensors")?.objectOrNull("bme688")
        return MobileCapabilities(
            aiEnabled = root.objectOrNull("ai")?.booleanValue("enabled") == true,
            bme688Supported = bme != null &&
                bme.optString("gas_semantics") == "raw_resistance_only" &&
                !bme.booleanValue("bsec"),
            matrixKinds = matrixKinds,
            commands = commands,
        )
    }

    fun containsDevice(json: String, deviceId: String): Boolean {
        val trimmed = json.trimStart()
        val devices = if (trimmed.startsWith("[")) {
            JSONArray(json)
        } else {
            JSONObject(json).optJSONArray("devices") ?: return false
        }
        for (index in 0 until devices.length()) {
            val device = devices.optJSONObject(index) ?: continue
            val candidate = device.optString("device_id", device.optString("id"))
            if (candidate == deviceId) return true
        }
        return false
    }

    fun parseSnapshot(json: String): MonitorSnapshot {
        val root = JSONObject(json)
        val device = root.objectOrNull("device")
            ?: findDevice(
                root.optJSONArray("devices"),
                root.optString("selected_device", FIXED_DEVICE_ID),
            )
            ?: JSONObject()
        val telemetry = root.objectOrNull("telemetry") ?: JSONObject()
        val sensors = telemetry.objectOrNull("sensors") ?: JSONObject()
        val bedTemperature = sensors.objectOrNull("bed_temperature")
        val thermal = sensors.objectOrNull("thermal")
        val inference = telemetry.objectOrNull("inference") ?: JSONObject()
        val environment = sensors.objectOrNull("environment")
        val bme688 = environment?.objectOrNull("bme688")
            ?: sensors.objectOrNull("bme688")
        val pressureSensor = sensors.objectOrNull("pressure")
        val media = root.objectOrNull("media") ?: JSONObject()
        val audioPlayer = sensors.objectOrNull("audio_player") ?: JSONObject()
        val visionOverlay = root.objectOrNull("vision_overlay")
        val capabilities = device.optJSONArray("capabilities")
        val pressureMatrix = root.objectOrNull("matrices")?.objectOrNull("pressure")

        val temperatures = mutableListOf<Float?>()
        val rawTemperatures = bedTemperature?.optJSONArray("t_mc")
        for (index in 0 until 5) {
            val raw = rawTemperatures.numberOrNull(index)
            temperatures += raw?.toFloat()?.div(1000f)
        }

        val centerRaw = thermal.numberOrNull("ctr_cd")?.toFloat()
        val thermalCenter = centerRaw?.let { if (kotlin.math.abs(it) >= 1000f) it / 100f else it / 10f }
        val online = device.booleanValue("online")
        val faults = inference.optInt("faults", 0)
        val video = media.objectOrNull("video")
        val audio = media.objectOrNull("audio")
        val runtimeTick = telemetry.numberOrNull("ra8_tick")?.toLong()
            ?: telemetry.numberOrNull("tick")?.toLong()
        val scd41Summary = parseScd41(environment, runtimeTick)
        val bme688Summary = parseBme688(bme688, runtimeTick)
        val pressureSummary = PressureSummary(
            occupied = inference.opt("occupied")?.let(::booleanFromAny),
            total = inference.numberOrNull("p_total")?.toLong()
                ?: pressureSensor.numberOrNull("total")?.toLong(),
            peak = inference.numberOrNull("p_peak")?.toInt()
                ?: pressureSensor.numberOrNull("peak")?.toInt(),
            edge = inference.numberOrNull("p_edge")?.toLong()
                ?: pressureSensor.numberOrNull("edge")?.toLong(),
            activePoints = inference.numberOrNull("p_points")?.toInt(),
            centerX100 = inference.numberOrNull("px100")?.toInt(),
            centerY100 = inference.numberOrNull("py100")?.toInt(),
        )

        return MonitorSnapshot(
            serverTime = root.optString("server_time"),
            device = DeviceSummary(
                id = device.optString("device_id", device.optString("id", FIXED_DEVICE_ID)),
                online = online,
                firmware = device.optString("firmware"),
                hardware = device.optString("hardware"),
                wifiRssi = device.numberOrNull("wifi_rssi")?.toInt(),
                lastSeen = device.optString("last_seen"),
                matrixConnected = device.booleanValue("matrix_connected"),
            ),
            fivePointTemperaturesC = temperatures,
            thermalCenterC = thermalCenter,
            pressureActivePoints = pressureSummary.activePoints ?: pressureMatrix
                ?.optJSONArray("values")
                ?.countPositiveValues(),
            pressure = pressureSummary,
            scd41 = scd41Summary,
            bme688 = bme688Summary,
            inference = InferenceSummary(
                state = inference.optInt("state", 0),
                candidate = inference.optInt("candidate", inference.optInt("state", 0)),
                confidence = inference.numberOrNull("confidence")?.toFloat()
                    ?: inference.numberOrNull("conf")?.toFloat()
                    ?: 0f,
                severity = inference.optInt("severity", 0).coerceIn(0, 5),
                faults = faults,
            ),
            diagnosticsOverall = root.objectOrNull("diagnostics")?.optString("overall", "offline")
                ?: when {
                    !online -> "offline"
                    faults != 0 -> "fault"
                    else -> "ok"
                },
            videoAvailable = mediaAvailable(video) ||
                capabilities.containsAny("camera", "video", "s3_camera_jpeg_wss"),
            audioAvailable = mediaAvailable(audio) ||
                capabilities.containsAny("audio", "microphone", "s3_external_microphone"),
            audioPlayer = AudioPlayerSummary(
                status = audioPlayer.optInt("s", 0),
                track = audioPlayer.optInt("track", 0),
                trackCount = audioPlayer.optInt("count", 0),
                volume = audioPlayer.optInt("vol", 0),
                file = audioPlayer.optString("file"),
            ),
            visionOverlay = parseVisionOverlay(visionOverlay),
        )
    }

    fun parseMatrix(json: String): MatrixFrame {
        val root = JSONObject(json)
        val data = root.objectOrNull("data")
        val matrix = root.objectOrNull("matrix")
            ?: data?.objectOrNull("matrix")
            ?: data
            ?: root
        val valuesJson = matrix.optJSONArray("values") ?: JSONArray()
        val values = ArrayList<Int>(valuesJson.length())
        for (index in 0 until valuesJson.length()) {
            values += valuesJson.optInt(index)
        }
        return MatrixFrame(
            kind = matrix.optString("kind"),
            sequence = matrix.optLong("sequence"),
            rows = matrix.optInt("rows"),
            cols = matrix.optInt("cols"),
            unit = matrix.optString("unit"),
            receivedAt = matrix.optString("received_at"),
            minimum = matrix.numberOrNull("minimum")?.toInt()
                ?: matrix.numberOrNull("min")?.toInt(),
            maximum = matrix.numberOrNull("maximum")?.toInt()
                ?: matrix.numberOrNull("max")?.toInt(),
            fps = matrix.numberOrNull("fps")?.toFloat(),
            frameAgeMs = matrix.numberOrNull("frame_age_ms")?.toLong(),
            drops = matrix.numberOrNull("drops")?.toLong() ?: 0L,
            delayed = matrix.booleanValue("delayed") ||
                ((matrix.numberOrNull("frame_age_ms")?.toLong() ?: 0L) > 2_000L),
            values = values,
        )
    }

    fun parseStories(json: String): List<AiStory> {
        val root = JSONObject(json)
        val raw = root.optJSONArray("stories") ?: return emptyList()
        return buildList {
            for (index in 0 until raw.length()) {
                raw.optJSONObject(index)?.let { add(parseStoryObject(it)) }
            }
        }
    }

    fun parseStory(json: String): AiStory {
        val root = JSONObject(json)
        return parseStoryObject(
            root.objectOrNull("story") ?: root.objectOrNull("data") ?: root
        )
    }

    fun parseAdvisories(json: String): List<AiAdvisory> {
        val root = JSONObject(json)
        val raw = root.optJSONArray("advisories") ?: return emptyList()
        return buildList {
            for (index in 0 until raw.length()) {
                raw.optJSONObject(index)?.let { add(parseAdvisoryObject(it)) }
            }
        }
    }

    fun parseAdvisory(json: String): AiAdvisory {
        val root = JSONObject(json)
        return parseAdvisoryObject(root.objectOrNull("data") ?: root)
    }

    fun parseAdvisoryAckId(json: String): String? {
        val root = JSONObject(json)
        return root.optString("advisory_id").takeIf(String::isNotBlank)
            ?: root.objectOrNull("data")?.optString("advisory_id")?.takeIf(String::isNotBlank)
    }

    fun parseEvents(json: String): List<SafetyEvent> {
        val trimmed = json.trimStart()
        val array = if (trimmed.startsWith("[")) {
            JSONArray(json)
        } else {
            JSONObject(json).optJSONArray("events") ?: return emptyList()
        }
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(parseEventObject(it)) }
            }
        }
    }

    fun parseEvent(json: String): SafetyEvent {
        val root = JSONObject(json)
        val candidate = root.objectOrNull("data") ?: root
        return parseEventObject(candidate)
    }

    fun parseEventAckId(json: String): Long? {
        val root = JSONObject(json)
        return root.numberOrNull("event_id")?.toLong()
            ?: root.objectOrNull("data")?.numberOrNull("id")?.toLong()
    }

    fun parseCommand(json: String): MobileCommandStatus {
        return parseCommand(json, fallbackAction = "")
    }

    fun parseCommand(
        json: String,
        fallbackAction: String,
        fallbackStatus: String = "queued",
    ): MobileCommandStatus {
        val root = JSONObject(json)
        val command = root.objectOrNull("command")
            ?: root.objectOrNull("data")
            ?: root
        return MobileCommandStatus(
            id = command.optLong("id"),
            action = command.optString(
                "action",
                command.optString("command", fallbackAction),
            ),
            status = command.optString("status", fallbackStatus),
            result = command.objectOrNull("result"),
        )
    }

    private fun parseEventObject(event: JSONObject): SafetyEvent {
        return SafetyEvent(
            id = event.optLong("id"),
            sourceEventId = event.numberOrNull("source_event_id")?.toLong()
                ?: event.numberOrNull("event_id")?.toLong(),
            eventType = event.optString("event_type", "ra8_safety"),
            state = event.opt("state")?.toString().orEmpty(),
            severity = event.optInt("severity", 0).coerceIn(0, 5),
            confidence = event.numberOrNull("confidence")?.toFloat() ?: 0f,
            receivedAt = event.optString("received_at"),
            acknowledged = event.booleanValue("acknowledged"),
            payload = event.objectOrNull("payload") ?: event.objectOrNull("payload_json"),
        )
    }

    private fun parseStoryObject(story: JSONObject): AiStory = AiStory(
        storyId = story.optString("story_id"),
        status = story.optString("status", "queued"),
        title = story.optString("title"),
        storyText = story.optString("story_text"),
        audioSizeBytes = story.optLong("audio_size_bytes", 0L),
        audioDurationMs = story.optLong("audio_duration_ms", 0L),
        commandId = story.numberOrNull("command_id")?.toLong(),
        error = story.optString("error"),
        createdAt = story.optString("created_at"),
        updatedAt = story.optString("updated_at"),
    )

    private fun parseAdvisoryObject(advisory: JSONObject): AiAdvisory = AiAdvisory(
        advisoryId = advisory.optString("advisory_id"),
        level = advisory.optString("level", "normal"),
        title = advisory.optString("title"),
        summary = advisory.optString("summary"),
        evidence = advisory.optJSONArray("evidence").stringList(),
        actions = advisory.optJSONArray("actions").stringList(),
        confidence = advisory.numberOrNull("confidence")?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
        requiresUserAttention = advisory.booleanValue("requires_user_attention"),
        sourceSequence = advisory.optLong("source_sequence", 0L),
        acknowledged = advisory.booleanValue("acknowledged"),
        createdAt = advisory.optString("created_at"),
    )

    private fun parseScd41(environment: JSONObject?, runtimeTick: Long?): Scd41Summary {
        if (environment == null) return Scd41Summary()
        val supported = environment.has("s") || environment.has("co2")
        if (!supported) return Scd41Summary()
        val status = environment.optInt("s", 0)
        val co2 = environment.numberOrNull("co2")?.toInt()
        val temperatureMc = environment.numberOrNull("t_mc")?.toInt()
        val humidityMp = environment.numberOrNull("rh_mp")?.toInt()
        val age = tickAgeMs(runtimeTick, environment.numberOrNull("last")?.toLong())
        val valid = status == 3 &&
            co2 != null && co2 in 0..40_000 &&
            temperatureMc != null && temperatureMc in -45_000..130_000 &&
            humidityMp != null && humidityMp in 0..100_000 &&
            (age == null || age <= 15_000L)
        return Scd41Summary(
            supported = true,
            valid = valid,
            status = status,
            co2Ppm = co2?.takeIf { it in 0..40_000 },
            temperatureC = temperatureMc?.takeIf { it in -45_000..130_000 }?.div(1000f),
            humidityPercent = humidityMp?.takeIf { it in 0..100_000 }?.div(1000f),
            sampleAgeMs = age,
            crcErrors = environment.optInt("crc_err", 0),
            i2cErrors = environment.optInt("i2c_err", 0),
        )
    }

    private fun parseBme688(bme: JSONObject?, runtimeTick: Long?): Bme688Summary {
        if (bme == null) return Bme688Summary()
        val status = bme.optInt("s", 0)
        val address = bme.numberOrNull("addr")?.toInt()
        val temperatureMc = bme.numberOrNull("t_mc")?.toInt()
        val humidityMp = bme.numberOrNull("rh_mp")?.toInt()
        val pressurePa = bme.numberOrNull("p_pa")?.toInt()
        val gasOhm = bme.numberOrNull("gas_ohm")?.toLong()
        val flags = bme.optInt("flags", 0)
        val age = tickAgeMs(runtimeTick, bme.numberOrNull("last")?.toLong())
        val climateValid = status == 3 && address in setOf(0x76, 0x77) &&
            temperatureMc != null && temperatureMc in -40_000..85_000 &&
            humidityMp != null && humidityMp in 0..100_000 &&
            pressurePa != null && pressurePa in 30_000..110_000 &&
            (age == null || age <= 15_000L)
        val gasValid = climateValid && flags and 0x06 == 0x06 &&
            gasOhm != null && gasOhm in 1..0xFFFF_FFFFL
        return Bme688Summary(
            supported = true,
            climateValid = climateValid,
            gasValid = gasValid,
            status = status,
            address = address,
            temperatureC = temperatureMc?.takeIf { it in -40_000..85_000 }?.div(1000f),
            humidityPercent = humidityMp?.takeIf { it in 0..100_000 }?.div(1000f),
            pressureHpa = pressurePa?.takeIf { it in 30_000..110_000 }?.div(100f),
            rawGasKohm = gasOhm?.takeIf { it in 1..0xFFFF_FFFFL }?.div(1000f),
            sampleAgeMs = age,
            errorCount = bme.optInt("err", 0),
        )
    }

    private fun findDevice(devices: JSONArray?, selectedId: String): JSONObject? {
        if (devices == null) return null
        var fallback: JSONObject? = null
        for (index in 0 until devices.length()) {
            val device = devices.optJSONObject(index) ?: continue
            if (fallback == null) fallback = device
            val id = device.optString("device_id", device.optString("id"))
            if (id == selectedId) return device
        }
        return fallback
    }

    private fun mediaAvailable(media: JSONObject?): Boolean {
        if (media == null) return false
        return if (media.has("available")) media.booleanValue("available") else true
    }

    private fun parseVisionOverlay(overlay: JSONObject?): VisionOverlaySummary {
        if (overlay == null) return VisionOverlaySummary()
        val sourceFrame = overlay.objectOrNull("source_frame") ?: JSONObject()
        val rawBoxes = overlay.optJSONArray("boxes") ?: JSONArray()
        val boxes = buildList {
            for (index in 0 until rawBoxes.length()) {
                val raw = rawBoxes.optJSONObject(index) ?: continue
                val x = raw.numberOrNull("x")?.toFloat()?.coerceIn(0f, 1f) ?: continue
                val y = raw.numberOrNull("y")?.toFloat()?.coerceIn(0f, 1f) ?: continue
                val width = raw.numberOrNull("width")?.toFloat()?.coerceIn(0f, 1f - x)
                    ?: continue
                val height = raw.numberOrNull("height")?.toFloat()?.coerceIn(0f, 1f - y)
                    ?: continue
                if (width <= 0f || height <= 0f) continue
                add(
                    VisionOverlayBox(
                        classId = raw.optInt("class_id", -1),
                        label = raw.optString("label"),
                        scoreX1000 = raw.optInt("score_x1000", 0).coerceIn(0, 1000),
                        x = x,
                        y = y,
                        width = width,
                        height = height,
                    )
                )
            }
        }
        return VisionOverlaySummary(
            enabled = overlay.booleanValue("enabled"),
            stale = overlay.booleanValue("stale"),
            expired = overlay.booleanValue("expired"),
            calibrationStatus = overlay.optString(
                "calibration_status",
                "identity_unverified",
            ),
            sourceSequence = sourceFrame.optLong("sequence", 0L),
            sourceAgeMs = sourceFrame.optLong("age_ms", 0L).coerceAtLeast(0L),
            boxes = boxes,
        )
    }
}

fun alertTierForSeverity(severity: Int): Int = when (severity.coerceIn(0, 5)) {
    0 -> 0
    1, 2 -> 1
    3, 4 -> 2
    else -> 3
}

fun safetyStateLabel(state: Int): String = when (state) {
    0 -> "状态正常"
    1 -> "接近床边"
    2 -> "宝宝离床"
    3 -> "温度过高"
    4 -> "温度偏低"
    5 -> "传感器异常"
    6 -> "检测到哭闹"
    7 -> "检测到面部遮挡"
    8 -> "宝宝站立"
    9 -> "宝宝活动中"
    10 -> "宝宝睡眠中"
    11 -> "宝宝清醒"
    255 -> "安全告警测试"
    else -> "未知状态 $state"
}

private fun JSONObject.objectOrNull(name: String): JSONObject? = opt(name) as? JSONObject

private fun JSONObject?.numberOrNull(name: String): Number? = this?.opt(name) as? Number

private fun JSONArray?.numberOrNull(index: Int): Number? {
    if (this == null || index !in 0 until length()) return null
    return opt(index) as? Number
}

private fun JSONArray?.containsAny(vararg candidates: String): Boolean {
    if (this == null) return false
    val expected = candidates.map { it.lowercase() }.toSet()
    for (index in 0 until length()) {
        val value = optString(index).lowercase()
        if (value in expected || expected.any { value.contains(it) }) return true
    }
    return false
}

private fun JSONArray?.stringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }
}

private fun JSONArray?.stringSet(): Set<String> = stringList().toSet()

private fun JSONArray.countPositiveValues(): Int {
    var count = 0
    for (index in 0 until length()) {
        if (optInt(index) > 0) count++
    }
    return count
}

private fun JSONObject.booleanValue(name: String): Boolean {
    return booleanFromAny(opt(name))
}

private fun booleanFromAny(value: Any?): Boolean = when (value) {
    is Boolean -> value
    is Number -> value.toInt() != 0
    is String -> value.equals("true", ignoreCase = true) || value == "1"
    else -> false
}

private fun tickAgeMs(now: Long?, then: Long?): Long? {
    if (now == null || then == null || then <= 0L) return null
    return if (now >= then) now - then else (0x1_0000_0000L - then) + now
}
