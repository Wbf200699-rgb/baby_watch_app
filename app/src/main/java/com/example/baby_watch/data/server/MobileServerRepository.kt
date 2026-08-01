package com.example.baby_watch.data.server

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.baby_watch.notification.AlertDispatcher
import com.example.baby_watch.service.log.LogManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.min

object MobileServerRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()
    private val mediaController = ServerMediaController(httpClient, scope)

    private val _state = MutableStateFlow(MonitorRepositoryState())
    val state: StateFlow<MonitorRepositoryState> = _state.asStateFlow()

    private val _events = MutableStateFlow<List<SafetyEvent>>(emptyList())
    val events: StateFlow<List<SafetyEvent>> = _events.asStateFlow()

    private val _lastCommand = MutableStateFlow<MobileCommandStatus?>(null)
    val lastCommand: StateFlow<MobileCommandStatus?> = _lastCommand.asStateFlow()

    private val _thermalMatrix = MutableStateFlow<MatrixFrame?>(null)
    val thermalMatrix: StateFlow<MatrixFrame?> = _thermalMatrix.asStateFlow()

    private val _pressureMatrix = MutableStateFlow<MatrixFrame?>(null)
    val pressureMatrix: StateFlow<MatrixFrame?> = _pressureMatrix.asStateFlow()

    private val _stories = MutableStateFlow<List<AiStory>>(emptyList())
    val stories: StateFlow<List<AiStory>> = _stories.asStateFlow()

    private val _advisories = MutableStateFlow<List<AiAdvisory>>(emptyList())
    val advisories: StateFlow<List<AiAdvisory>> = _advisories.asStateFlow()

    val videoState: StateFlow<VideoStreamState> = mediaController.videoState
    val audioState: StateFlow<AudioStreamState> = mediaController.audioState

    private val initializationLock = Any()
    private var appContext: Context? = null
    private var credentialStore: ServerCredentialStore? = null
    private var credentials: ServerCredentials? = null
    private var monitorJob: Job? = null
    private var sseCall: Call? = null

    fun start(context: Context) {
        ensureInitialized(context)
        reconnect()
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        sseCall?.cancel()
        sseCall = null
        mediaController.stop()
        _state.update {
            it.copy(
                connectionStatus = if (it.configuration.hasPassword) {
                    ServerConnectionStatus.Error
                } else {
                    ServerConnectionStatus.NotConfigured
                },
                errorMessage = if (it.configuration.hasPassword) "后台监护服务已停止" else null,
            )
        }
    }

    fun ensureInitialized(context: Context) {
        if (appContext != null) return
        synchronized(initializationLock) {
            if (appContext != null) return
            appContext = context.applicationContext
            credentialStore = ServerCredentialStore(context.applicationContext)
            credentials = credentialStore?.load()
            val view = credentialStore?.view() ?: ServerConfigurationView()
            _state.update {
                it.copy(
                    configuration = view,
                    connectionStatus = if (credentials?.isComplete == true) {
                        ServerConnectionStatus.Connecting
                    } else {
                        ServerConnectionStatus.NotConfigured
                    },
                )
            }
        }
    }

    fun saveConfiguration(
        context: Context,
        baseUrl: String,
        username: String,
        password: String,
    ) {
        ensureInitialized(context)
        val normalizedUrl = baseUrl.trim().trimEnd('/')
        val existingPassword = credentials?.password.orEmpty()
        val effectivePassword = password.ifBlank { existingPassword }
        val validationError = when {
            !normalizedUrl.startsWith("https://") -> "服务端地址必须使用 https://"
            username.isBlank() -> "请输入服务端用户名"
            effectivePassword.isBlank() -> "请输入服务端密码"
            else -> null
        }
        if (validationError != null) {
            _state.update {
                it.copy(
                    connectionStatus = ServerConnectionStatus.Error,
                    errorMessage = validationError,
                )
            }
            return
        }

        credentialStore?.save(
            baseUrl = normalizedUrl,
            username = username,
            newPassword = password.takeIf { it.isNotBlank() },
        )
        credentials = credentialStore?.load()
        _state.update {
            it.copy(
                configuration = credentialStore?.view() ?: it.configuration,
                connectionStatus = ServerConnectionStatus.Connecting,
                errorMessage = null,
            )
        }
        reconnect()
    }

    fun reconnect() {
        val activeCredentials = credentials ?: return
        monitorJob?.cancel()
        sseCall?.cancel()
        if (!activeCredentials.isComplete) {
            _state.update {
                it.copy(
                    connectionStatus = ServerConnectionStatus.NotConfigured,
                    errorMessage = null,
                )
            }
            return
        }

        _state.update {
            it.copy(
                connectionStatus = ServerConnectionStatus.Connecting,
                errorMessage = null,
            )
        }
        monitorJob = scope.launch {
            monitorLoop(activeCredentials)
        }
        if (mediaController.isWanted) mediaController.reconnect(activeCredentials)
    }

    fun startVideo() {
        credentials?.takeIf { it.isComplete }?.let(mediaController::startVideo)
    }

    fun stopVideo() {
        mediaController.stopVideo()
    }

    fun startAudio() {
        credentials?.takeIf { it.isComplete }?.let(mediaController::startAudio)
    }

    fun stopAudio() {
        mediaController.stopAudio()
    }

    fun stopMedia() {
        mediaController.stop()
    }

    fun setAudioMuted(muted: Boolean) {
        mediaController.setMuted(muted)
    }

    fun acknowledgeEvent(eventId: Long) {
        val activeCredentials = credentials ?: return
        scope.launch {
            runCatching {
                executePostJson(
                    activeCredentials,
                    "/api/mobile/v1/devices/${activeCredentials.deviceId}/events/$eventId/ack",
                    "{}",
                )
            }.onSuccess {
                _events.update { events ->
                    events.map { event ->
                        if (event.id == eventId) event.copy(acknowledged = true) else event
                    }
                }
                LogManager.notification("告警已确认", "事件 #$eventId")
            }.onFailure { error ->
                LogManager.system("确认告警失败", userMessage(error))
                _state.update { it.copy(errorMessage = userMessage(error)) }
            }
        }
    }

    fun createStory(
        theme: String,
        childAgeMonths: Int = 18,
        durationSeconds: Int = 60,
        style: String = "calm",
        autoplay: Boolean = true,
    ) {
        val activeCredentials = credentials ?: return
        if (!_state.value.capabilities.aiEnabled || theme.isBlank()) return
        scope.launch {
            val body = JSONObject()
                .put("theme", theme.trim())
                .put("child_age_months", childAgeMonths.coerceIn(0, 72))
                .put("duration_seconds", durationSeconds.coerceIn(30, 240))
                .put("language", "zh-CN")
                .put("style", style.takeIf { it in setOf("calm", "gentle", "playful") } ?: "calm")
                .put("autoplay", autoplay)
                .put("request_id", "phone-story-${UUID.randomUUID()}")
                .toString()
            runCatching {
                MobileProtocolParser.parseStory(
                    executePostJson(
                        activeCredentials,
                        "/api/mobile/v1/devices/${activeCredentials.deviceId}/stories",
                        body,
                    )
                )
            }.onSuccess(::upsertStory)
                .onFailure { _state.update { state -> state.copy(errorMessage = userMessage(it)) } }
        }
    }

    fun replayStory(storyId: String) {
        val activeCredentials = credentials ?: return
        if (storyId.isBlank()) return
        scope.launch {
            runCatching {
                MobileProtocolParser.parseStory(
                    executePostJson(
                        activeCredentials,
                        "/api/mobile/v1/devices/${activeCredentials.deviceId}/stories/$storyId/replay",
                        "{}",
                    )
                )
            }.onSuccess(::upsertStory)
                .onFailure { _state.update { state -> state.copy(errorMessage = userMessage(it)) } }
        }
    }

    fun acknowledgeAdvisory(advisoryId: String) {
        val activeCredentials = credentials ?: return
        if (advisoryId.isBlank()) return
        scope.launch {
            runCatching {
                executePostJson(
                    activeCredentials,
                    "/api/mobile/v1/devices/${activeCredentials.deviceId}/advisories/$advisoryId/ack",
                    "{}",
                )
            }.onSuccess {
                markAdvisoryAcknowledged(advisoryId)
            }.onFailure { _state.update { state -> state.copy(errorMessage = userMessage(it)) } }
        }
    }

    fun sendCommand(action: String, args: Map<String, Any> = emptyMap()) {
        val activeCredentials = credentials ?: return
        scope.launch {
            val body = JSONObject()
                .put("action", action)
                .put("args", JSONObject(args))
                .put("request_id", UUID.randomUUID().toString())
                .toString()
            runCatching {
                executePostJson(
                    activeCredentials,
                    "/api/mobile/v1/devices/${activeCredentials.deviceId}/commands",
                    body,
                )
            }.mapCatching(MobileProtocolParser::parseCommand)
                .onSuccess { command ->
                    _lastCommand.value = command
                    LogManager.notification(
                        "命令已提交",
                        "${command.action} · ${command.status}",
                    )
                }
                .onFailure { error ->
                    LogManager.system("命令发送失败", userMessage(error))
                    _state.update { it.copy(errorMessage = userMessage(error)) }
                }
        }
    }

    private suspend fun monitorLoop(activeCredentials: ServerCredentials) {
        var retrySeconds = 1L
        while (currentCoroutineContext().isActive) {
            try {
                bootstrap(activeCredentials)
                retrySeconds = 1L
                consumeSse(activeCredentials)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                val message = userMessage(error)
                _state.update {
                    it.copy(
                        connectionStatus = ServerConnectionStatus.Error,
                        errorMessage = message,
                    )
                }
                LogManager.system("服务端连接中断", message)
            }
            delay(retrySeconds * 1000L)
            retrySeconds = min(retrySeconds * 2L, 15L)
            _state.update {
                it.copy(
                    connectionStatus = ServerConnectionStatus.Connecting,
                    errorMessage = "正在重新连接服务端…",
                )
            }
        }
    }

    private fun bootstrap(activeCredentials: ServerCredentials) {
        val capabilities = MobileProtocolParser.parseCapabilities(
            executeGet(activeCredentials, "/api/mobile/v1/capabilities")
        )
        val devicesJson = executeGet(activeCredentials, "/api/mobile/v1/devices")
        check(MobileProtocolParser.containsDevice(devicesJson, activeCredentials.deviceId)) {
            "服务端未找到设备 ${activeCredentials.deviceId}"
        }

        val snapshot = MobileProtocolParser.parseSnapshot(
            executeGet(
                activeCredentials,
                "/api/mobile/v1/devices/${activeCredentials.deviceId}/snapshot",
            )
        )
        _state.update {
            it.copy(
                connectionStatus = ServerConnectionStatus.Connected,
                capabilities = capabilities,
                snapshot = snapshot,
                errorMessage = null,
            )
        }

        listOf("thermal", "pressure").forEach { kind ->
            runCatching {
                MobileProtocolParser.parseMatrix(
                    executeGet(
                        activeCredentials,
                        "/api/mobile/v1/devices/${activeCredentials.deviceId}/matrices/$kind",
                    )
                )
            }.onSuccess(::applyMatrix)
        }

        val serverEvents = MobileProtocolParser.parseEvents(
            executeGet(
                activeCredentials,
                "/api/mobile/v1/devices/${activeCredentials.deviceId}/events?limit=100",
            )
        ).sortedByDescending { it.id }
        _events.value = serverEvents
        handleFetchedEvents(serverEvents)
        if (capabilities.aiEnabled) {
            refreshAi(activeCredentials)
        } else {
            _stories.value = emptyList()
            _advisories.value = emptyList()
        }
        LogManager.system("服务端已连接", activeCredentials.deviceId)
    }

    private fun handleFetchedEvents(serverEvents: List<SafetyEvent>) {
        val store = credentialStore ?: return
        val maximumId = serverEvents.maxOfOrNull { it.id } ?: return
        val lastHandled = store.lastHandledEventId()
        if (lastHandled == null) {
            store.setLastHandledEventId(maximumId)
            return
        }
        serverEvents
            .asSequence()
            .filter { it.id > lastHandled }
            .sortedBy { it.id }
            .forEach(::handleNewEvent)
        if (maximumId > lastHandled) store.setLastHandledEventId(maximumId)
    }

    private fun handleNewEvent(event: SafetyEvent) {
        credentialStore?.setLastHandledEventId(event.id)
        if (!event.acknowledged && event.severity > 0) {
            appContext?.let { AlertDispatcher.dispatch(it, event) }
        }
    }

    private fun consumeSse(activeCredentials: ServerCredentials) {
        val request = authenticatedRequest(
            activeCredentials,
            "/api/mobile/v1/devices/${activeCredentials.deviceId}/stream",
        )
            .header("Accept", "text/event-stream")
            .build()
        val call = httpClient.newCall(request)
        sseCall = call
        try {
            call.execute().use { response ->
                ensureSuccessful(response)
                val source = response.body?.source() ?: throw IOException("服务端没有返回 SSE 数据")
                var eventName = "message"
                val data = StringBuilder()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    when {
                        line.startsWith(":") -> Unit
                        line.startsWith("event:") -> eventName = line.substringAfter(':').trim()
                        line.startsWith("data:") -> {
                            if (data.isNotEmpty()) data.append('\n')
                            data.append(line.substringAfter(':').trimStart())
                        }
                        line.isBlank() -> {
                            if (data.isNotEmpty()) {
                                processSseEvent(activeCredentials, eventName, data.toString())
                            }
                            eventName = "message"
                            data.clear()
                        }
                    }
                }
                throw IOException("SSE 连接已关闭")
            }
        } finally {
            if (sseCall === call) sseCall = null
        }
    }

    private fun processSseEvent(
        activeCredentials: ServerCredentials,
        eventName: String,
        json: String,
    ) {
        when (eventName) {
            "snapshot" -> {
                val next = MobileProtocolParser.parseSnapshot(json)
                _state.update {
                    it.copy(
                        connectionStatus = ServerConnectionStatus.Connected,
                        snapshot = next.copy(
                            pressureActivePoints = it.snapshot.pressureActivePoints,
                        ),
                        errorMessage = null,
                    )
                }
            }

            "matrix" -> {
                val matrix = MobileProtocolParser.parseMatrix(json)
                applyMatrix(matrix)
            }

            "event" -> {
                val parsed = runCatching { MobileProtocolParser.parseEvent(json) }.getOrNull()
                if (parsed == null || parsed.id <= 0L) {
                    refreshEvents(activeCredentials)
                } else {
                    _events.update { current ->
                        (listOf(parsed) + current.filterNot { it.id == parsed.id })
                            .sortedByDescending { it.id }
                            .take(200)
                    }
                    val lastHandled = credentialStore?.lastHandledEventId() ?: parsed.id
                    if (parsed.id > lastHandled) handleNewEvent(parsed)
                }
            }

            "event_ack" -> {
                MobileProtocolParser.parseEventAckId(json)?.let { eventId ->
                    _events.update { current ->
                        current.map { event ->
                            if (event.id == eventId) event.copy(acknowledged = true) else event
                        }
                    }
                }
            }

            "command" -> runCatching {
                MobileProtocolParser.parseCommand(json)
            }.onSuccess {
                _lastCommand.value = it
            }

            "ai_story" -> runCatching {
                MobileProtocolParser.parseStory(json)
            }.onSuccess(::upsertStory)

            "ai_advisory" -> runCatching {
                MobileProtocolParser.parseAdvisory(json)
            }.onSuccess(::upsertAdvisory)

            "ai_advisory_ack" -> {
                MobileProtocolParser.parseAdvisoryAckId(json)
                    ?.let(::markAdvisoryAcknowledged)
            }

            "log" -> {
                val root = JSONObject(json).optJSONObject("data") ?: JSONObject(json)
                LogManager.notification(
                    root.optString("component", "设备日志"),
                    root.optString("message"),
                )
            }
        }
    }

    private fun refreshEvents(activeCredentials: ServerCredentials) {
        runCatching {
            MobileProtocolParser.parseEvents(
                executeGet(
                    activeCredentials,
                    "/api/mobile/v1/devices/${activeCredentials.deviceId}/events?limit=100",
                )
            )
        }.onSuccess { refreshed ->
            val ordered = refreshed.sortedByDescending { it.id }
            _events.value = ordered
            handleFetchedEvents(ordered)
        }
    }

    private fun refreshAi(activeCredentials: ServerCredentials) {
        runCatching {
            MobileProtocolParser.parseStories(
                executeGet(
                    activeCredentials,
                    "/api/mobile/v1/devices/${activeCredentials.deviceId}/stories?limit=20",
                )
            )
        }.onSuccess { _stories.value = it }
        runCatching {
            MobileProtocolParser.parseAdvisories(
                executeGet(
                    activeCredentials,
                    "/api/mobile/v1/devices/${activeCredentials.deviceId}/advisories?limit=50",
                )
            )
        }.onSuccess { _advisories.value = it }
    }

    private fun upsertStory(story: AiStory) {
        if (story.storyId.isBlank()) return
        _stories.update { current ->
            (listOf(story) + current.filterNot { it.storyId == story.storyId })
                .sortedByDescending { it.updatedAt.ifBlank { it.createdAt } }
                .take(20)
        }
    }

    private fun upsertAdvisory(advisory: AiAdvisory) {
        if (advisory.advisoryId.isBlank()) return
        _advisories.update { current ->
            (listOf(advisory) + current.filterNot { it.advisoryId == advisory.advisoryId })
                .sortedByDescending(AiAdvisory::createdAt)
                .take(50)
        }
    }

    private fun markAdvisoryAcknowledged(advisoryId: String) {
        _advisories.update { current ->
            current.map { advisory ->
                if (advisory.advisoryId == advisoryId) {
                    advisory.copy(acknowledged = true)
                } else {
                    advisory
                }
            }
        }
    }

    private fun applyMatrix(matrix: MatrixFrame) {
        when (matrix.kind.lowercase()) {
            "pressure", "pressure_matrix" -> {
                _pressureMatrix.value = matrix
                _state.update {
                    it.copy(
                        snapshot = it.snapshot.copy(
                            pressureActivePoints = matrix.activePoints,
                        )
                    )
                }
            }

            "thermal", "thermal_matrix", "thermal_camera", "thermal_image" -> {
                _thermalMatrix.value = matrix
            }
        }
    }

    private fun executeGet(credentials: ServerCredentials, path: String): String {
        val request = authenticatedRequest(credentials, path).get().build()
        return execute(request)
    }

    private fun executePostJson(
        credentials: ServerCredentials,
        path: String,
        json: String,
    ): String {
        val request = authenticatedRequest(credentials, path)
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return execute(request)
    }

    private fun execute(request: Request): String {
        return httpClient.newCall(request).execute().use { response ->
            ensureSuccessful(response)
            response.body?.string().orEmpty()
        }
    }

    private fun authenticatedRequest(
        credentials: ServerCredentials,
        path: String,
    ): Request.Builder {
        return Request.Builder()
            .url(credentials.baseUrl + path)
            .header(
                "Authorization",
                Credentials.basic(credentials.username, credentials.password, Charsets.UTF_8),
            )
    }

    private fun ensureSuccessful(response: Response) {
        if (response.isSuccessful) return
        val errorText = runCatching { response.body?.string().orEmpty() }.getOrDefault("")
        val detail = runCatching {
            JSONObject(errorText).optString(
                "error",
                JSONObject(errorText).optString("detail"),
            )
        }.getOrDefault("")
        throw IOException(
            if (detail.isBlank()) "服务端返回 HTTP ${response.code}"
            else "服务端返回 HTTP ${response.code}: $detail"
        )
    }

    private fun userMessage(error: Throwable): String {
        return error.message?.take(240).orEmpty().ifBlank { "未知网络错误" }
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
}

private class ServerMediaController(
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
) {
    private data class AudioPacket(
        val bytes: ByteArray,
        val discontinuity: Boolean,
    )

    private val _videoState = MutableStateFlow(VideoStreamState())
    val videoState: StateFlow<VideoStreamState> = _videoState.asStateFlow()

    private val _audioState = MutableStateFlow(AudioStreamState())
    val audioState: StateFlow<AudioStreamState> = _audioState.asStateFlow()

    private val audioQueue = Channel<AudioPacket>(
        capacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var activeCredentials: ServerCredentials? = null
    private var videoSocket: WebSocket? = null
    private var audioSocket: WebSocket? = null
    private var videoReconnectJob: Job? = null
    private var audioReconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var heartbeatSequence = 1L

    @Volatile
    private var videoWanted: Boolean = false

    @Volatile
    private var audioWanted: Boolean = false

    val isWanted: Boolean
        get() = videoWanted || audioWanted

    init {
        scope.launch {
            for (packet in audioQueue) {
                val track = audioTrack ?: continue
                try {
                    if (packet.discontinuity) {
                        track.pause()
                        track.flush()
                        track.play()
                    }
                    if (!_audioState.value.muted) {
                        track.setVolume(AudioTrack.getMaxVolume())
                        track.write(packet.bytes, 0, packet.bytes.size, AudioTrack.WRITE_BLOCKING)
                    } else {
                        track.setVolume(0f)
                    }
                } catch (error: Exception) {
                    _audioState.update { it.copy(errorMessage = error.message) }
                }
            }
        }
    }

    fun reconnect(credentials: ServerCredentials) {
        activeCredentials = credentials
        closeSockets()
        if (videoWanted) connectVideo()
        if (audioWanted) {
            ensureAudioTrack()
            connectAudio()
        }
        ensureHeartbeat()
    }

    fun startVideo(credentials: ServerCredentials) {
        if (activeCredentials != credentials) {
            activeCredentials = credentials
            closeSockets()
            if (audioWanted) connectAudio()
        }
        videoWanted = true
        _videoState.update { it.copy(wanted = true, errorMessage = null) }
        if (videoSocket == null) connectVideo()
        ensureHeartbeat()
    }

    fun stopVideo() {
        videoWanted = false
        videoReconnectJob?.cancel()
        videoReconnectJob = null
        val socket = videoSocket
        videoSocket = null
        socket?.cancel()
        _videoState.value = VideoStreamState()
        stopHeartbeatIfIdle()
    }

    fun startAudio(credentials: ServerCredentials) {
        if (activeCredentials != credentials) {
            activeCredentials = credentials
            closeSockets()
            if (videoWanted) connectVideo()
        }
        audioWanted = true
        _audioState.update {
            it.copy(wanted = true, muted = false, errorMessage = null)
        }
        ensureAudioTrack()
        if (audioSocket == null) connectAudio()
        ensureHeartbeat()
    }

    fun stopAudio() {
        audioWanted = false
        audioReconnectJob?.cancel()
        audioReconnectJob = null
        val socket = audioSocket
        audioSocket = null
        socket?.cancel()
        while (audioQueue.tryReceive().isSuccess) Unit
        releaseAudioTrack()
        _audioState.value = AudioStreamState()
        stopHeartbeatIfIdle()
    }

    private fun ensureHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isWanted) {
                delay(20_000)
                val heartbeat = JSONObject()
                    .put("type", "media.heartbeat")
                    .put("version", 1)
                    .put("sequence", heartbeatSequence++)
                    .toString()
                videoSocket?.send(heartbeat)
                audioSocket?.send(heartbeat)
            }
        }
    }

    fun stop() {
        videoWanted = false
        audioWanted = false
        activeCredentials = null
        videoReconnectJob?.cancel()
        audioReconnectJob?.cancel()
        heartbeatJob?.cancel()
        videoReconnectJob = null
        audioReconnectJob = null
        heartbeatJob = null
        closeSockets()
        while (audioQueue.tryReceive().isSuccess) Unit
        releaseAudioTrack()
        _videoState.value = VideoStreamState()
        _audioState.value = AudioStreamState()
    }

    fun setMuted(muted: Boolean) {
        _audioState.update { it.copy(muted = muted) }
        audioTrack?.setVolume(if (muted) 0f else AudioTrack.getMaxVolume())
    }

    private fun connectVideo() {
        if (!videoWanted || videoSocket != null) return
        val credentials = activeCredentials ?: return
        val request = websocketRequest(
            credentials,
            "/api/mobile/v1/devices/${credentials.deviceId}/media/video/ws",
        )
        videoSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (videoSocket !== webSocket) return
                _videoState.update { it.copy(connected = true, errorMessage = null) }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (videoSocket !== webSocket) return
                scope.launch {
                    runCatching { S3MediaProtocol.decodeVideo(bytes.toByteArray()) }
                        .onSuccess { frame ->
                            _videoState.update { previous ->
                                val delta = if (previous.frameCount == 0L) {
                                    1L
                                } else {
                                    (frame.sequence - previous.sequence) and 0xFFFF_FFFFL
                                }
                                val missedFrames = if (delta in 2 until 0x8000_0000L) {
                                    delta - 1L
                                } else {
                                    0L
                                }
                                VideoStreamState(
                                    wanted = previous.wanted,
                                    connected = true,
                                    sequence = frame.sequence,
                                    width = frame.meta0,
                                    height = frame.meta1,
                                    quality = frame.meta2,
                                    frameCount = previous.frameCount + 1L,
                                    gapCount = previous.gapCount + missedFrames,
                                    frameBytes = frame.payload,
                                )
                            }
                        }
                        .onFailure { error ->
                            _videoState.update { it.copy(errorMessage = error.message) }
                        }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (videoSocket === webSocket) {
                    videoSocket = null
                    _videoState.update { it.copy(connected = false, errorMessage = reason) }
                    scheduleVideoReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (videoSocket === webSocket) {
                    videoSocket = null
                    _videoState.update { it.copy(connected = false, errorMessage = t.message) }
                    scheduleVideoReconnect()
                }
            }
        })
    }

    private fun connectAudio() {
        if (!audioWanted || audioSocket != null) return
        val credentials = activeCredentials ?: return
        ensureAudioTrack()
        val request = websocketRequest(
            credentials,
            "/api/mobile/v1/devices/${credentials.deviceId}/media/audio/ws",
        )
        audioSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (audioSocket !== webSocket) return
                ensureAudioTrack()
                _audioState.update { it.copy(connected = true, errorMessage = null) }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (audioSocket !== webSocket) return
                runCatching { S3MediaProtocol.decode(bytes.toByteArray()) }
                    .onSuccess { frame ->
                        if (frame.mediaType != S3MediaProtocol.TYPE_PCM16 ||
                            frame.meta0 != AUDIO_SAMPLE_RATE ||
                            frame.meta2 != 1 ||
                            frame.meta3 != 16
                        ) {
                            return@onSuccess
                        }
                        audioQueue.trySend(
                            AudioPacket(
                                bytes = frame.payload,
                                discontinuity = frame.discontinuity,
                            )
                        )
                        _audioState.update {
                            it.copy(
                                connected = true,
                                receiving = true,
                                sequence = frame.sequence,
                                errorMessage = null,
                            )
                        }
                    }
                    .onFailure { error ->
                        _audioState.update { it.copy(errorMessage = error.message) }
                    }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (audioSocket === webSocket) {
                    audioSocket = null
                    _audioState.update {
                        it.copy(
                            connected = false,
                            receiving = false,
                            errorMessage = reason,
                        )
                    }
                    scheduleAudioReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (audioSocket === webSocket) {
                    audioSocket = null
                    _audioState.update {
                        it.copy(
                            connected = false,
                            receiving = false,
                            errorMessage = t.message,
                        )
                    }
                    scheduleAudioReconnect()
                }
            }
        })
    }

    private fun scheduleVideoReconnect() {
        if (!videoWanted || videoReconnectJob?.isActive == true) return
        videoReconnectJob = scope.launch {
            delay(2_000)
            if (videoWanted && videoSocket == null) connectVideo()
        }
    }

    private fun scheduleAudioReconnect() {
        if (!audioWanted || audioReconnectJob?.isActive == true) return
        audioReconnectJob = scope.launch {
            delay(2_000)
            if (audioWanted && audioSocket == null) connectAudio()
        }
    }

    private fun websocketRequest(credentials: ServerCredentials, path: String): Request {
        val websocketBase = credentials.baseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        return Request.Builder()
            .url(websocketBase + path)
            .header(
                "Authorization",
                Credentials.basic(credentials.username, credentials.password, Charsets.UTF_8),
            )
            .build()
    }

    private fun ensureAudioTrack() {
        if (audioTrack != null) return
        val minimum = AudioTrack.getMinBufferSize(
            AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferBytes = maxOf(minimum, 640 * 8)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AUDIO_SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also {
                it.setVolume(if (_audioState.value.muted) 0f else AudioTrack.getMaxVolume())
                it.play()
            }
    }

    private fun releaseAudioTrack() {
        audioTrack?.runCatching {
            pause()
            flush()
            release()
        }
        audioTrack = null
    }

    private fun stopHeartbeatIfIdle() {
        if (isWanted) return
        heartbeatJob?.cancel()
        heartbeatJob = null
        activeCredentials = null
    }

    private fun closeSockets() {
        val video = videoSocket
        val audio = audioSocket
        videoSocket = null
        audioSocket = null
        video?.cancel()
        audio?.cancel()
    }

    private companion object {
        const val AUDIO_SAMPLE_RATE = 16_000
    }
}
