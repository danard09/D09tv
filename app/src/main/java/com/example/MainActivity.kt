package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppStrings
import com.example.i18n.stringsForLanguage
import com.example.security.UserRole
import com.example.ui.components.SplashOverlay
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LanguageSelectionScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UserLoginScreen
import com.example.ui.theme.D09TvTheme
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SkyCyan
import com.example.viewmodel.FeedbackType
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.UiFeedbackEvent
import kotlinx.coroutines.delay

object D09Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val PLAYER = "player/{channelId}"

    fun playerRoute(channelId: String): String = "player/${Uri.encode(channelId)}"
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            D09TvRootApp()
        }
    }
}

@Composable
fun D09TvRootApp(
    viewModel: MainViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val language = uiState.settings.language
    val strings = remember(language) { stringsForLanguage(language) }
    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val isArabicScript = language != AppLanguage.ENGLISH

    var minSplashElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1200)
        minSplashElapsed = true
    }
    val showSplash = !uiState.isReady || !minSplashElapsed

    var showLanguagePickerOverride by remember { mutableStateOf(false) }
    var showAdminLoginPortal by remember { mutableStateOf(false) }
    var adminPreviewChannelId by remember { mutableStateOf<String?>(null) }

    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var shareInsteadOfSave by remember { mutableStateOf(false) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { content ->
                viewModel.importPlaylistJson(content)
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val json = pendingExportJson
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
            }.onSuccess {
                viewModel.notifyExportSuccess()
            }
        }
        pendingExportJson = null
    }

    LaunchedEffect(viewModel, strings) {
        viewModel.events.collect { event ->
            when (event) {
                is UiFeedbackEvent.ToastMessage -> {
                    val msg = when (event.messageKey) {
                        FeedbackType.CACHE_CLEARED -> strings.settingsCacheCleared
                        FeedbackType.IMPORT_SUCCESS -> strings.settingsImportSuccess(event.count)
                        FeedbackType.IMPORT_ERROR -> strings.settingsImportError
                        FeedbackType.EXPORT_SUCCESS -> strings.settingsExportSuccess
                        FeedbackType.RESTORE_SUCCESS -> strings.settingsImportSuccess(event.count)
                        FeedbackType.CHANGES_SAVED -> strings.adminChangesSaved
                        FeedbackType.CODE_UPDATED -> strings.adminCodeUpdated
                        FeedbackType.CODE_VALIDATION_ERROR -> strings.adminCodeValidationError
                        FeedbackType.SESSIONS_CLEARED -> strings.adminSessionsCleared
                        FeedbackType.ADMIN_REQUIRED -> strings.accessDeniedAdminOnly
                        FeedbackType.LOGGED_OUT -> strings.loggedOutMessage
                    }
                    snackbarHostState.showSnackbar(msg)
                }

                is UiFeedbackEvent.ExportReady -> {
                    if (shareInsteadOfSave) {
                        shareInsteadOfSave = false
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "D09 TV Playlist")
                            putExtra(Intent.EXTRA_TEXT, event.jsonContent)
                        }
                        context.startActivity(
                            Intent.createChooser(sendIntent, strings.settingsShareJson),
                        )
                    } else {
                        pendingExportJson = event.jsonContent
                        createDocumentLauncher.launch("d09_tv_playlist.json")
                    }
                }

                is UiFeedbackEvent.AutoOpenPlayer -> {
                    navController.navigate(D09Routes.playerRoute(event.channelId)) {
                        launchSingleTop = true
                    }
                }

                is UiFeedbackEvent.AdminAuthenticated -> {
                    showAdminLoginPortal = false
                    adminPreviewChannelId = null
                }

                is UiFeedbackEvent.LoggedOut -> {
                    showAdminLoginPortal = false
                    adminPreviewChannelId = null
                    navController.popBackStack(D09Routes.HOME, inclusive = false)
                }
            }
        }
    }

    D09TvTheme(
        themeMode = uiState.settings.themeMode,
        isArabicScript = isArabicScript,
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection,
            LocalAppStrings provides strings,
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val isOnPlayer = currentRoute == D09Routes.PLAYER || adminPreviewChannelId != null

            val needsLanguageScreen = uiState.isReady && (!uiState.settings.hasSelectedLanguage || showLanguagePickerOverride)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    contentWindowInsets = if (isOnPlayer) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
                    bottomBar = {
                        // Bottom bar shown only for authenticated USER on compact screens when not in player/onboarding/admin login
                        if (!showSplash &&
                            !needsLanguageScreen &&
                            !showAdminLoginPortal &&
                            uiState.authRole == UserRole.USER &&
                            !isOnPlayer
                        ) {
                            BoxWithConstraints {
                                if (maxWidth < 600.dp) {
                                    val navItems = remember(strings) {
                                        listOf(
                                            NavDestinationItem(D09Routes.HOME, strings.navHome, Icons.Filled.Home, "nav_home"),
                                            NavDestinationItem(D09Routes.SEARCH, strings.navSearch, Icons.Filled.Search, "nav_search"),
                                            NavDestinationItem(D09Routes.FAVORITES, strings.navFavorites, Icons.Filled.Favorite, "nav_favorites"),
                                            NavDestinationItem(D09Routes.SETTINGS, strings.navSettings, Icons.Filled.Settings, "nav_settings"),
                                        )
                                    }
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 4.dp,
                                    ) {
                                        navItems.forEach { item ->
                                            val selected = currentRoute == item.route
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = item.label,
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = item.label,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = SkyCyan,
                                                    selectedTextColor = SkyCyan,
                                                    indicatorColor = ElectricBlue.copy(alpha = 0.22f),
                                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                ),
                                                modifier = Modifier.testTag(item.testTag),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        when {
                            showSplash -> Unit

                            needsLanguageScreen -> {
                                LanguageSelectionScreen(
                                    selectedLanguage = language,
                                    onSelectLanguage = { viewModel.setLanguage(it) },
                                    onContinue = {
                                        viewModel.confirmLanguageSelection(language)
                                        showLanguagePickerOverride = false
                                    },
                                )
                            }

                            showAdminLoginPortal -> {
                                AdminLoginScreen(
                                    onAdminLoginAttempt = { code -> viewModel.attemptAdminLogin(code) },
                                    onBack = { showAdminLoginPortal = false },
                                )
                            }

                            uiState.authRole == UserRole.NONE -> {
                                UserLoginScreen(
                                    allowRememberSession = uiState.settings.rememberUserSession,
                                    onLoginAttempt = { code, remember ->
                                        viewModel.attemptUserLogin(code, remember)
                                    },
                                    onOpenLanguagePicker = { showLanguagePickerOverride = true },
                                    onOpenAdminPortal = { showAdminLoginPortal = true },
                                )
                            }

                            uiState.authRole == UserRole.ADMIN -> {
                                val previewChannel = adminPreviewChannelId?.let { id ->
                                    uiState.allChannels.firstOrNull { it.id == id }
                                }
                                if (previewChannel != null) {
                                    PlayerScreen(
                                        channel = previewChannel,
                                        channelList = uiState.allChannels.ifEmpty { listOf(previewChannel) },
                                        isFavorite = previewChannel.id in uiState.favoriteIds,
                                        isOnline = uiState.isOnline,
                                        engineMode = uiState.settings.playerEngineMode,
                                        autoPlayNextChannel = uiState.settings.autoPlayNextChannel,
                                        onSelectChannel = { next -> adminPreviewChannelId = next.id },
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onPlaybackStarted = { viewModel.onChannelPlaybackStarted(it) },
                                        onBack = { adminPreviewChannelId = null },
                                    )
                                } else {
                                    AdminDashboardScreen(
                                        uiState = uiState,
                                        onPlayChannel = { ch -> adminPreviewChannelId = ch.id },
                                        onSaveChannel = { id, name, group, logo, backdrop, url, url2, url3, order, ua, referer, enabled ->
                                            viewModel.saveChannel(
                                                existingId = id,
                                                name = name,
                                                groupName = group,
                                                logo = logo,
                                                backdrop = backdrop,
                                                url = url,
                                                url2 = url2,
                                                url3 = url3,
                                                orderIndex = order,
                                                userAgent = ua,
                                                referer = referer,
                                                enabled = enabled,
                                            )
                                        },
                                        onToggleChannelEnabled = { id, enabled -> viewModel.toggleChannelEnabled(id, enabled) },
                                        onMoveChannelUp = { ch, list -> viewModel.moveChannelUp(ch, list) },
                                        onMoveChannelDown = { ch, list -> viewModel.moveChannelDown(ch, list) },
                                        onDeleteChannel = { viewModel.deleteChannel(it) },
                                        onAddCategory = { viewModel.addCategory(it) },
                                        onRenameCategory = { old, new -> viewModel.renameCategory(old, new) },
                                        onDeleteCategory = { viewModel.deleteCategory(it) },
                                        onMoveCategoryUp = { viewModel.moveCategoryUp(it) },
                                        onMoveCategoryDown = { viewModel.moveCategoryDown(it) },
                                        onImportJsonFileClick = {
                                            openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                        },
                                        onPasteJsonSubmit = { viewModel.importPlaylistJson(it) },
                                        onExportJsonClick = {
                                            shareInsteadOfSave = false
                                            viewModel.requestExportPlaylist()
                                        },
                                        onShareJsonClick = {
                                            shareInsteadOfSave = true
                                            viewModel.requestExportPlaylist()
                                        },
                                        onRestoreBundledClick = { viewModel.restoreBundledPlaylist() },
                                        onSetUserLoginRequired = { viewModel.setUserLoginRequired(it) },
                                        onSetAllowRememberSession = { viewModel.setAllowRememberSession(it) },
                                        onUpdateUserCode = { viewModel.updateUserAccessCode(it) },
                                        onUpdateAdminCode = { viewModel.updateAdminAccessCode(it) },
                                        onClearSavedSessions = { viewModel.clearAllUserSessions() },
                                        onSelectLanguage = { viewModel.setLanguage(it) },
                                        onSelectTheme = { viewModel.setThemeMode(it) },
                                        onSetAutoPlayLast = { viewModel.setAutoPlayLastChannel(it) },
                                        onSetAutoPlayNext = { viewModel.setAutoPlayNextChannel(it) },
                                        onSetPlayerEngine = { viewModel.setPlayerEngineMode(it) },
                                        onSaveChangesClick = { viewModel.notifyAdminChangesSaved() },
                                        onLogoutAdmin = { viewModel.logoutAdmin() },
                                    )
                                }
                            }

                            else -> {
                                // Authenticated Ordinary User Experience (NavigationRail on Tablets/TV, NavigationBar on Phones)
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val useNavRail = maxWidth >= 600.dp
                                    val navItems = remember(strings) {
                                        listOf(
                                            NavDestinationItem(D09Routes.HOME, strings.navHome, Icons.Filled.Home, "nav_home"),
                                            NavDestinationItem(D09Routes.SEARCH, strings.navSearch, Icons.Filled.Search, "nav_search"),
                                            NavDestinationItem(D09Routes.FAVORITES, strings.navFavorites, Icons.Filled.Favorite, "nav_favorites"),
                                            NavDestinationItem(D09Routes.SETTINGS, strings.navSettings, Icons.Filled.Settings, "nav_settings"),
                                        )
                                    }

                                    fun navigateToTab(route: String) {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }

                                    fun openPlayer(channelId: String) {
                                        navController.navigate(D09Routes.playerRoute(channelId)) {
                                            launchSingleTop = true
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxSize()) {
                                        if (useNavRail && !isOnPlayer) {
                                            NavigationRail(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .windowInsetsPadding(WindowInsets(0, 0, 0, 0)),
                                            ) {
                                                Spacer(modifier = Modifier.weight(0.15f))
                                                navItems.forEach { item ->
                                                    val selected = currentRoute == item.route
                                                    NavigationRailItem(
                                                        selected = selected,
                                                        onClick = { navigateToTab(item.route) },
                                                        icon = {
                                                            Icon(
                                                                imageVector = item.icon,
                                                                contentDescription = item.label,
                                                                modifier = Modifier.size(24.dp),
                                                            )
                                                        },
                                                        label = {
                                                            Text(
                                                                text = item.label,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                            )
                                                        },
                                                        colors = NavigationRailItemDefaults.colors(
                                                            selectedIconColor = SkyCyan,
                                                            selectedTextColor = SkyCyan,
                                                            indicatorColor = ElectricBlue.copy(alpha = 0.22f),
                                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        ),
                                                        modifier = Modifier.testTag("${item.testTag}_rail"),
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }

                                        NavHost(
                                            navController = navController,
                                            startDestination = D09Routes.HOME,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                        ) {
                                            composable(D09Routes.HOME) {
                                                HomeScreen(
                                                    uiState = uiState,
                                                    onChannelClick = { ch -> openPlayer(ch.id) },
                                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                    onSelectCategory = { viewModel.selectCategory(it) },
                                                    onToggleFavoriteCategory = { viewModel.toggleFavoriteCategory(it) },
                                                    onSelectLanguage = { viewModel.setLanguage(it) },
                                                    onNavigateSearch = { navigateToTab(D09Routes.SEARCH) },
                                                    onNavigateSettings = { navigateToTab(D09Routes.SETTINGS) },
                                                )
                                            }

                                            composable(D09Routes.SEARCH) {
                                                SearchScreen(
                                                    uiState = uiState,
                                                    onQueryChange = { viewModel.updateSearchQuery(it) },
                                                    onChannelClick = { ch -> openPlayer(ch.id) },
                                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                )
                                            }

                                            composable(D09Routes.FAVORITES) {
                                                FavoritesScreen(
                                                    uiState = uiState,
                                                    onChannelClick = { ch -> openPlayer(ch.id) },
                                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                    onToggleFavoriteCategory = { viewModel.toggleFavoriteCategory(it) },
                                                    onOpenCategoryOnHome = { cat ->
                                                        viewModel.selectCategory(cat)
                                                        navigateToTab(D09Routes.HOME)
                                                    },
                                                )
                                            }

                                            composable(D09Routes.SETTINGS) {
                                                SettingsScreen(
                                                    uiState = uiState,
                                                    onSelectLanguage = { viewModel.setLanguage(it) },
                                                    onSelectTheme = { viewModel.setThemeMode(it) },
                                                    onSetAutoPlayLast = { viewModel.setAutoPlayLastChannel(it) },
                                                    onSetAutoPlayNext = { viewModel.setAutoPlayNextChannel(it) },
                                                    onSetPlayerEngine = { viewModel.setPlayerEngineMode(it) },
                                                    onClearCacheClick = { viewModel.clearCache() },
                                                    onLogoutUserClick = { viewModel.logoutUser() },
                                                    onOpenAdminPortalClick = { showAdminLoginPortal = true },
                                                )
                                            }

                                            composable(
                                                route = D09Routes.PLAYER,
                                                arguments = listOf(navArgument("channelId") { type = NavType.StringType }),
                                            ) { entry ->
                                                val channelId = entry.arguments?.getString("channelId").orEmpty()
                                                val channel = uiState.enabledChannels.firstOrNull { it.id == channelId }
                                                    ?: uiState.enabledChannels.firstOrNull()

                                                if (channel != null) {
                                                    val zapList = uiState.visibleChannelsByCategory
                                                        .takeIf { list -> list.any { it.id == channel.id } }
                                                        ?: uiState.enabledChannels.ifEmpty { listOf(channel) }

                                                    PlayerScreen(
                                                        channel = channel,
                                                        channelList = zapList,
                                                        isFavorite = channel.id in uiState.favoriteIds,
                                                        isOnline = uiState.isOnline,
                                                        engineMode = uiState.settings.playerEngineMode,
                                                        autoPlayNextChannel = uiState.settings.autoPlayNextChannel,
                                                        onSelectChannel = { next ->
                                                            navController.navigate(D09Routes.playerRoute(next.id)) {
                                                                popUpTo(D09Routes.PLAYER) { inclusive = true }
                                                                launchSingleTop = true
                                                            }
                                                        },
                                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                        onPlaybackStarted = { viewModel.onChannelPlaybackStarted(it) },
                                                        onBack = { navController.popBackStack() },
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black),
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

                SplashOverlay(visible = showSplash)
            }
        }
    }
}
