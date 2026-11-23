package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import com.example.data.model.ChannelEntity
import com.example.data.model.PlayerEngineMode
import com.example.data.model.StreamSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class VideoQualityOption(
    val id: String,
    val label: String,
    val height: Int,
    val bitrate: Int,
)

enum class StreamFailoverStatus {
    NONE,
    RETRYING_CURRENT,
    SWITCHING_BACKUP,
    ALL_FAILED,
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val isMuted: Boolean = false,
    val volume: Float = 1f,
    val activeStreamIndex: Int = 0,
    val availableStreams: List<StreamSource> = emptyList(),
    val availableQualities: List<VideoQualityOption> = emptyList(),
    val selectedQualityId: String = "auto",
    val failoverStatus: StreamFailoverStatus = StreamFailoverStatus.NONE,
    val failoverSourceNumber: Int = 1,
    val errorDetail: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
)

@OptIn(UnstableApi::class)
class IptvPlayerController(
    private val context: Context,
    private val engineMode: PlayerEngineMode,
    private val scope: CoroutineScope,
    private val onAutoAdvanceNext: () -> Unit,
) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    val exoPlayer: ExoPlayer = buildExoPlayer(engineMode)

    private var currentChannel: ChannelEntity? = null
    private var streams: List<StreamSource> = emptyList()
    private var currentStreamListPos: Int = 0
    private var retryAttemptOnCurrentStream: Int = 0
    private var retryJob: Job? = null
    private var isAudioOnlyStream: Boolean = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _uiState.update { it.copy(isBuffering = true) }
                }
                Player.STATE_READY -> {
                    retryAttemptOnCurrentStream = 0
                    val format = exoPlayer.videoFormat
                    _uiState.update {
                        it.copy(
                            isBuffering = false,
                            failoverStatus = StreamFailoverStatus.NONE,
                            errorDetail = null,
                            videoWidth = format?.width ?: 0,
                            videoHeight = format?.height ?: 0,
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    _uiState.update { it.copy(isBuffering = false, isPlaying = false) }
                    onAutoAdvanceNext()
                }
                Player.STATE_IDLE -> {
                    // Handled via error or explicit stop
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            val qualities = mutableListOf<VideoQualityOption>()
            val seenHeights = mutableSetOf<Int>()
            for (group in tracks.groups) {
                if (group.type == C.TRACK_TYPE_VIDEO && group.isSupported) {
                    for (i in 0 until group.length) {
                        if (group.isTrackSupported(i)) {
                            val fmt = group.getTrackFormat(i)
                            if (fmt.height > 0 && seenHeights.add(fmt.height)) {
                                qualities.add(
                                    VideoQualityOption(
                                        id = "${fmt.height}p",
                                        label = "${fmt.height}p",
                                        height = fmt.height,
                                        bitrate = fmt.bitrate,
                                    )
                                )
                            }
                        }
                    }
                }
            }
            qualities.sortByDescending { it.height }
            _uiState.update { it.copy(availableQualities = qualities) }
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlaybackFailure(error.localizedMessage ?: error.errorCodeName)
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    private fun buildExoPlayer(mode: PlayerEngineMode): ExoPlayer {
        val loadControl = when (mode) {
            PlayerEngineMode.LOW_LATENCY -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(4_000, 12_000, 800, 1_500)
                .build()
            PlayerEngineMode.HIGH_BUFFER -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 60_000, 3_500, 6_000)
                .build()
            PlayerEngineMode.STANDARD -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(8_000, 30_000, 1_500, 3_000)
                .build()
        }

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    fun playChannel(channel: ChannelEntity, startSourceIndex: Int = 0) {
        retryJob?.cancel()
        currentChannel = channel
        streams = channel.availableStreams()
        if (streams.isEmpty()) {
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    availableStreams = emptyList(),
                    failoverStatus = StreamFailoverStatus.ALL_FAILED,
                    errorDetail = "Empty stream URL",
                )
            }
            return
        }
        currentStreamListPos = startSourceIndex.coerceIn(0, streams.lastIndex)
        retryAttemptOnCurrentStream = 0
        _uiState.update {
            it.copy(
                availableStreams = streams,
                activeStreamIndex = currentStreamListPos,
                availableQualities = emptyList(),
                selectedQualityId = "auto",
                failoverStatus = StreamFailoverStatus.NONE,
                failoverSourceNumber = streams[currentStreamListPos].index,
                errorDetail = null,
                isBuffering = true,
            )
        }
        selectAutoQuality()
        prepareCurrentStream()
    }

    fun selectStreamSource(listIndex: Int) {
        if (listIndex !in streams.indices) return
        retryJob?.cancel()
        currentStreamListPos = listIndex
        retryAttemptOnCurrentStream = 0
        _uiState.update {
            it.copy(
                activeStreamIndex = listIndex,
                failoverStatus = StreamFailoverStatus.NONE,
                failoverSourceNumber = streams[listIndex].index,
                errorDetail = null,
                isBuffering = true,
            )
        }
        prepareCurrentStream()
    }

    fun retryCurrentChannel() {
        val channel = currentChannel ?: return
        playChannel(channel, startSourceIndex = 0)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                prepareCurrentStream()
            } else {
                exoPlayer.play()
            }
        }
    }

    fun setMuted(muted: Boolean) {
        exoPlayer.volume = if (muted) 0f else _uiState.value.volume
        _uiState.update { it.copy(isMuted = muted) }
    }

    fun toggleMute() {
        setMuted(!_uiState.value.isMuted)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer.volume = clamped
        _uiState.update { it.copy(volume = clamped, isMuted = clamped == 0f) }
    }

    fun selectQuality(option: VideoQualityOption?) {
        if (option == null) {
            selectAutoQuality()
            return
        }
        val params = exoPlayer.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .setMaxVideoSize(Int.MAX_VALUE, option.height)
            .setMinVideoSize(0, 0)
            .build()
        exoPlayer.trackSelectionParameters = params
        _uiState.update { it.copy(selectedQualityId = option.id) }
    }

    private fun selectAutoQuality() {
        val params = exoPlayer.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .clearVideoSizeConstraints()
            .build()
        exoPlayer.trackSelectionParameters = params
        _uiState.update { it.copy(selectedQualityId = "auto") }
    }

    private fun handlePlaybackFailure(reason: String) {
        if (streams.isEmpty()) return

        retryJob?.cancel()
        retryJob = scope.launch(Dispatchers.Main) {
            // Step 1: Retry current stream once
            if (retryAttemptOnCurrentStream == 0) {
                retryAttemptOnCurrentStream = 1
                _uiState.update {
                    it.copy(
                        isBuffering = true,
                        failoverStatus = StreamFailoverStatus.RETRYING_CURRENT,
                        failoverSourceNumber = streams[currentStreamListPos].index,
                        errorDetail = reason,
                    )
                }
                delay(900)
                prepareCurrentStream()
                return@launch
            }

            // Step 2 & 3: Switch to url2 / url3 if available
            if (currentStreamListPos + 1 < streams.size) {
                currentStreamListPos += 1
                retryAttemptOnCurrentStream = 0
                val nextSource = streams[currentStreamListPos]
                _uiState.update {
                    it.copy(
                        isBuffering = true,
                        activeStreamIndex = currentStreamListPos,
                        failoverStatus = StreamFailoverStatus.SWITCHING_BACKUP,
                        failoverSourceNumber = nextSource.index,
                        errorDetail = reason,
                    )
                }
                delay(500)
                prepareCurrentStream()
                return@launch
            }

            // Step 4: All streams failed
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    failoverStatus = StreamFailoverStatus.ALL_FAILED,
                    errorDetail = reason,
                )
            }
        }
    }

    private fun prepareCurrentStream() {
        val source = streams.getOrNull(currentStreamListPos) ?: return
        val parsed = parseStreamAndDrm(source)
        isAudioOnlyStream = parsed.cleanUrl.contains("zeno.fm", ignoreCase = true) ||
            parsed.cleanUrl.endsWith("/stream", ignoreCase = true)

        val headers = mutableMapOf<String, String>()
        val cleanReferer = source.referer.takeIf { it.isNotBlank() && it != "." }
        if (cleanReferer != null) {
            headers["Referer"] = cleanReferer
        }
        val cleanUserAgent = source.userAgent.takeIf { it.isNotBlank() && it != "." }
            ?: "D09TV/1.0 (Android; Media3)"

        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(cleanUserAgent)
            .apply {
                if (headers.isNotEmpty()) {
                    setDefaultRequestProperties(headers)
                }
            }

        val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(parsed.cleanUrl))
        val lower = parsed.cleanUrl.lowercase()

        val mediaSource: MediaSource = when {
            lower.contains(".m3u8") -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                HlsMediaSource.Factory(httpFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItemBuilder.build())
            }
            lower.contains(".mpd") -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                val dashFactory = DashMediaSource.Factory(httpFactory)
                buildClearKeyDrmManager(parsed.drmScheme, parsed.drmLicense)?.let { drmManager ->
                    dashFactory.setDrmSessionManagerProvider { drmManager }
                }
                dashFactory.createMediaSource(mediaItemBuilder.build())
            }
            lower.endsWith(".ts") || lower.contains(".ts?") -> {
                val extractorsFactory = DefaultExtractorsFactory()
                    .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)
                ProgressiveMediaSource.Factory(httpFactory, extractorsFactory)
                    .createMediaSource(mediaItemBuilder.build())
            }
            else -> {
                DefaultMediaSourceFactory(httpFactory)
                    .createMediaSource(mediaItemBuilder.build())
            }
        }

        exoPlayer.stop()
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private data class ParsedStreamUri(
        val cleanUrl: String,
        val drmScheme: String,
        val drmLicense: String,
    )

    private fun parseStreamAndDrm(source: StreamSource): ParsedStreamUri {
        var url = source.rawUrl
        var scheme = source.drmScheme
        var license = source.drmLicense

        // Handle inline ?|drmScheme=...&drmLicense=... parameters present in some playlist entries
        val pipeIdx = url.indexOf("?|")
        if (pipeIdx > 0) {
            val suffix = url.substring(pipeIdx + 2)
            url = url.substring(0, pipeIdx)
            suffix.split("&").forEach { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    when (parts[0].lowercase()) {
                        "drmscheme" -> if (scheme.isBlank()) scheme = parts[1]
                        "drmlicense" -> if (license.isBlank()) license = parts[1]
                    }
                }
            }
        }

        // Strip trailing fragment metadata (e.g. #hash) if not needed
        val hashIdx = url.indexOf('#')
        if (hashIdx > 0) {
            url = url.substring(0, hashIdx)
        }

        return ParsedStreamUri(url.trim(), scheme.trim(), license.trim())
    }

    private fun buildClearKeyDrmManager(
        drmScheme: String,
        drmLicense: String,
    ): DefaultDrmSessionManager? {
        if (!drmScheme.equals("clearkey", ignoreCase = true)) return null
        val parts = drmLicense.split(":")
        if (parts.size != 2) return null

        return runCatching {
            val kidB64 = hexToBase64Url(parts[0].trim())
            val keyB64 = hexToBase64Url(parts[1].trim())
            val json = """{"keys":[{"kty":"oct","k":"$keyB64","kid":"$kidB64"}],"type":"temporary"}"""
            val callback = LocalMediaDrmCallback(json.toByteArray(Charsets.UTF_8))
            DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                .build(callback)
        }.getOrNull()
    }

    private fun hexToBase64Url(hex: String): String {
        val clean = hex.replace("-", "")
        val bytes = ByteArray(clean.length / 2)
        for (i in bytes.indices) {
            bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun release() {
        retryJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
