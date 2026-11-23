package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.ChannelEntity
import com.example.data.model.PlayerEngineMode
import com.example.i18n.LocalAppStrings
import com.example.player.IptvPlayerController
import com.example.player.StreamFailoverStatus
import com.example.ui.components.ChannelLogoArtwork
import com.example.ui.components.LiveIndicatorPill
import com.example.ui.components.TvFocusableSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyCyan
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: ChannelEntity,
    channelList: List<ChannelEntity>,
    isFavorite: Boolean,
    isOnline: Boolean,
    engineMode: PlayerEngineMode,
    autoPlayNextChannel: Boolean,
    onSelectChannel: (ChannelEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlaybackStarted: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showChannelDrawer by remember { mutableStateOf(false) }
    var showSourceMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var lastInteractionTick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val currentIndex = remember(channel.id, channelList) {
        channelList.indexOfFirst { it.id == channel.id }.coerceAtLeast(0)
    }

    val goNextChannel: () -> Unit = remember(currentIndex, channelList) {
        {
            if (channelList.size > 1) {
                val nextIdx = (currentIndex + 1) % channelList.size
                onSelectChannel(channelList[nextIdx])
            }
        }
    }

    val goPrevChannel: () -> Unit = remember(currentIndex, channelList) {
        {
            if (channelList.size > 1) {
                val prevIdx = if (currentIndex - 1 < 0) channelList.lastIndex else currentIndex - 1
                onSelectChannel(channelList[prevIdx])
            }
        }
    }

    val controller = remember(engineMode) {
        IptvPlayerController(
            context = context.applicationContext,
            engineMode = engineMode,
            scope = scope,
            onAutoAdvanceNext = {
                if (autoPlayNextChannel) {
                    goNextChannel()
                }
            },
        )
    }

    val playerState by controller.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(channel.id, controller) {
        onPlaybackStarted(channel.id)
        controller.playChannel(channel)
        controlsVisible = true
        lastInteractionTick = System.currentTimeMillis()
    }

    // Auto-hide controls while playing
    LaunchedEffect(controlsVisible, playerState.isPlaying, showChannelDrawer, showSourceMenu, showQualityMenu, lastInteractionTick) {
        if (controlsVisible && playerState.isPlaying && !showChannelDrawer && !showSourceMenu && !showQualityMenu) {
            delay(4500)
            controlsVisible = false
        }
    }

    // Lifecycle pause/resume management
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> controller.exoPlayer.pause()
                Lifecycle.Event.ON_STOP -> controller.exoPlayer.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Release controller on PlayerScreen exit
    DisposableEffect(controller) {
        onDispose {
            controller.release()
        }
    }

    // Manage fullscreen immersive mode and orientation safely
    val activity = remember(context) { context.findActivity() }
    LaunchedEffect(isFullscreen, activity) {
        val window = activity?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isFullscreen) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.let { act ->
                val insetsController = WindowCompat.getInsetsController(act.window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    BackHandler {
        when {
            showChannelDrawer -> showChannelDrawer = false
            isFullscreen -> isFullscreen = false
            else -> onBack()
        }
    }

    fun bumpControls() {
        controlsVisible = true
        lastInteractionTick = System.currentTimeMillis()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.ChannelUp, Key.PageUp -> {
                            bumpControls()
                            goNextChannel()
                            true
                        }
                        Key.ChannelDown, Key.PageDown -> {
                            bumpControls()
                            goPrevChannel()
                            true
                        }
                        Key.MediaPlayPause -> {
                            bumpControls()
                            controller.togglePlayPause()
                            true
                        }
                        Key.Menu, Key.Info -> {
                            showChannelDrawer = !showChannelDrawer
                            bumpControls()
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (!controlsVisible) {
                                bumpControls()
                                true
                            } else {
                                lastInteractionTick = System.currentTimeMillis()
                                false
                            }
                        }
                        else -> {
                            lastInteractionTick = System.currentTimeMillis()
                            false
                        }
                    }
                } else {
                    false
                }
            }
            .testTag("player_root"),
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    player = controller.exoPlayer
                }
            },
            update = { playerView ->
                if (playerView.player !== controller.exoPlayer) {
                    playerView.player = controller.exoPlayer
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (showChannelDrawer) {
                        showChannelDrawer = false
                    } else {
                        controlsVisible = !controlsVisible
                        if (controlsVisible) {
                            lastInteractionTick = System.currentTimeMillis()
                        }
                    }
                },
        )

        // Center Buffering / Failover / Error Status
        if (playerState.isBuffering && playerState.failoverStatus != StreamFailoverStatus.ALL_FAILED) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                ) {
                    CircularProgressIndicator(
                        color = SkyCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp),
                    )
                    val statusMsg = when (playerState.failoverStatus) {
                        StreamFailoverStatus.RETRYING_CURRENT -> strings.playerRetryingPrimary
                        StreamFailoverStatus.SWITCHING_BACKUP -> strings.playerTryingBackup(playerState.failoverSourceNumber)
                        else -> if (playerState.isPlaying) strings.playerBuffering else strings.playerConnecting
                    }
                    Text(
                        text = statusMsg,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (playerState.failoverStatus == StreamFailoverStatus.ALL_FAILED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .widthIn(max = 440.dp)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, LiveCrimson.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                        .padding(22.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = strings.playerAllStreamsFailed,
                        tint = LiveCrimson,
                        modifier = Modifier.size(44.dp),
                    )
                    Text(
                        text = if (!isOnline) strings.playerOfflineError else strings.playerAllStreamsFailed,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    if (!playerState.errorDetail.isNullOrBlank()) {
                        Text(
                            text = playerState.errorDetail ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { controller.retryCurrentChannel() },
                            modifier = Modifier.testTag("player_error_retry_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = strings.playerRetry,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.playerRetry)
                        }
                        if (channelList.size > 1) {
                            Button(
                                onClick = goNextChannel,
                                modifier = Modifier.testTag("player_error_next_button"),
                            ) {
                                Text(strings.playerNext)
                            }
                        }
                    }
                }
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC030612),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xDD030612),
                            ),
                        ),
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("player_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.playerBack,
                                tint = Color.White,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0A1022))
                                .padding(4.dp),
                        ) {
                            ChannelLogoArtwork(
                                logoUrl = channel.logo,
                                channelName = channel.name,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                LiveIndicatorPill(label = strings.liveBadge)
                                if (playerState.videoHeight > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ElectricBlue.copy(alpha = 0.8f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "${playerState.videoHeight}p",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = strings.translateCategory(channel.groupName),
                                style = MaterialTheme.typography.labelMedium,
                                color = SkyCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = {
                                bumpControls()
                                onToggleFavorite(channel.id)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_favorite_button"),
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) strings.removeFavorite else strings.addFavorite,
                                tint = if (isFavorite) LiveCrimson else Color.White,
                            )
                        }

                        IconButton(
                            onClick = {
                                bumpControls()
                                showChannelDrawer = !showChannelDrawer
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_channel_list_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = strings.playerChannelList,
                                tint = Color.White,
                            )
                        }

                        IconButton(
                            onClick = {
                                bumpControls()
                                isFullscreen = !isFullscreen
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_fullscreen_button"),
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = if (isFullscreen) strings.playerExitFullscreen else strings.playerFullscreen,
                                tint = Color.White,
                            )
                        }
                    }
                }

                // Bottom Controls Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xB8070B19))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Playback Transport
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    bumpControls()
                                    goPrevChannel()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("player_prev_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = strings.playerPrevious,
                                    tint = Color.White,
                                )
                            }

                            IconButton(
                                onClick = {
                                    bumpControls()
                                    controller.togglePlayPause()
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(ElectricBlue, RoyalPurple)))
                                    .testTag("player_play_pause_button"),
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) strings.playerPause else strings.playerPlay,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }

                            IconButton(
                                onClick = {
                                    bumpControls()
                                    goNextChannel()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("player_next_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = strings.playerNext,
                                    tint = Color.White,
                                )
                            }
                        }

                        // Source & Quality Menus
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (playerState.availableStreams.size > 1) {
                                Box {
                                    TvFocusableSurface(
                                        onClick = {
                                            bumpControls()
                                            showSourceMenu = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        containerColor = Color.White.copy(alpha = 0.12f),
                                        modifier = Modifier.testTag("player_source_button"),
                                    ) { _ ->
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.SettingsInputAntenna,
                                                contentDescription = strings.playerSource,
                                                tint = SkyCyan,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                text = "${strings.playerSource} ${playerState.failoverSourceNumber}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showSourceMenu,
                                        onDismissRequest = { showSourceMenu = false },
                                    ) {
                                        playerState.availableStreams.forEachIndexed { idx, src ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = strings.playerSourceLabel(src.index, src.label),
                                                        fontWeight = if (idx == playerState.activeStreamIndex) FontWeight.Bold else FontWeight.Normal,
                                                    )
                                                },
                                                onClick = {
                                                    controller.selectStreamSource(idx)
                                                    showSourceMenu = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            Box {
                                TvFocusableSurface(
                                    onClick = {
                                        bumpControls()
                                        showQualityMenu = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.testTag("player_quality_button"),
                                ) { _ ->
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.HighQuality,
                                            contentDescription = strings.playerQuality,
                                            tint = SkyCyan,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = if (playerState.selectedQualityId == "auto") {
                                                strings.playerQualityAuto
                                            } else {
                                                playerState.selectedQualityId
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = strings.playerQualityAuto,
                                                fontWeight = if (playerState.selectedQualityId == "auto") FontWeight.Bold else FontWeight.Normal,
                                            )
                                        },
                                        onClick = {
                                            controller.selectQuality(null)
                                            showQualityMenu = false
                                        },
                                    )
                                    playerState.availableQualities.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.label,
                                                    fontWeight = if (playerState.selectedQualityId == option.id) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            },
                                            onClick = {
                                                controller.selectQuality(option)
                                                showQualityMenu = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Volume Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = {
                                bumpControls()
                                controller.toggleMute()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("player_mute_button"),
                        ) {
                            Icon(
                                imageVector = if (playerState.isMuted) Icons.AutoMirrored.Filled.VolumeOff
                                else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (playerState.isMuted) strings.playerUnmute else strings.playerMute,
                                tint = Color.White,
                            )
                        }

                        Slider(
                            value = if (playerState.isMuted) 0f else playerState.volume,
                            onValueChange = {
                                bumpControls()
                                controller.setVolume(it)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = SkyCyan,
                                activeTrackColor = ElectricBlue,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_volume_slider"),
                        )
                    }
                }
            }
        }

        // Quick Channel Switcher Drawer
        AnimatedVisibility(
            visible = showChannelDrawer,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 270.dp, max = 340.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = strings.playerChannelList,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        onClick = { showChannelDrawer = false },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_drawer_close_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = strings.cancel,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(channelList, key = { "drawer_${it.id}" }) { item ->
                        val active = item.id == channel.id
                        TvFocusableSurface(
                            onClick = {
                                onSelectChannel(item)
                                showChannelDrawer = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            containerColor = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_drawer_item_${item.id}"),
                        ) { _ ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0A1022))
                                        .padding(4.dp),
                                ) {
                                    ChannelLogoArtwork(
                                        logoUrl = item.logo,
                                        channelName = item.name,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                        color = if (active) SkyCyan else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = strings.translateCategory(item.groupName),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
