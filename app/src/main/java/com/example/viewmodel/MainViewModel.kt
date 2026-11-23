package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelGroupEntity
import com.example.data.model.PlayerEngineMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.repository.ChannelRepository
import com.example.i18n.AppLanguage
import com.example.network.NetworkMonitor
import com.example.security.AccessControlManager
import com.example.security.AuthVerificationResult
import com.example.security.UserRole
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class MainUiState(
    val isReady: Boolean = false,
    val authRole: UserRole = UserRole.NONE,
    val allChannels: List<ChannelEntity> = emptyList(),
    val enabledChannels: List<ChannelEntity> = emptyList(),
    val featuredChannels: List<ChannelEntity> = emptyList(),
    val recentlyWatchedChannels: List<ChannelEntity> = emptyList(),
    val favoriteChannels: List<ChannelEntity> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val favoriteCategories: Set<String> = emptySet(),
    val groups: List<ChannelGroupEntity> = emptyList(),
    val allGroups: List<ChannelGroupEntity> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val settings: UserSettings = UserSettings(),
    val isOnline: Boolean = true,
) {
    val visibleChannelsByCategory: List<ChannelEntity>
        get() = if (selectedCategory == null) {
            enabledChannels
        } else {
            enabledChannels.filter { it.groupName == selectedCategory }
        }

    val searchResults: List<ChannelEntity>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return enabledChannels
            return enabledChannels.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.groupName.contains(q, ignoreCase = true)
            }
        }
}

sealed interface UiFeedbackEvent {
    data class ToastMessage(val messageKey: FeedbackType, val count: Int = 0) : UiFeedbackEvent
    data class ExportReady(val jsonContent: String) : UiFeedbackEvent
    data class AutoOpenPlayer(val channelId: String) : UiFeedbackEvent
    data object AdminAuthenticated : UiFeedbackEvent
    data object LoggedOut : UiFeedbackEvent
}

enum class FeedbackType {
    CACHE_CLEARED,
    IMPORT_SUCCESS,
    IMPORT_ERROR,
    EXPORT_SUCCESS,
    RESTORE_SUCCESS,
    CHANGES_SAVED,
    CODE_UPDATED,
    CODE_VALIDATION_ERROR,
    SESSIONS_CLEARED,
    ADMIN_REQUIRED,
    LOGGED_OUT,
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val accessControl = AccessControlManager()
    private val db = AppDatabase.getInstance(application)
    private val prefs = UserPreferencesRepository(application, accessControl)
    private val repository = ChannelRepository(application, db.iptvDao(), prefs)
    private val networkMonitor = NetworkMonitor(application)

    private val selectedCategory = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val isReady = MutableStateFlow(false)
    private val authRole = MutableStateFlow(UserRole.NONE)

    private val _events = MutableSharedFlow<UiFeedbackEvent>()
    val events: SharedFlow<UiFeedbackEvent> = _events.asSharedFlow()

    private val _activePlayerChannelId = MutableStateFlow<String?>(null)
    val activePlayerChannelId: StateFlow<String?> = _activePlayerChannelId.asStateFlow()

    val uiState: StateFlow<MainUiState> = combine(
        repository.allChannels,
        repository.enabledChannels,
        repository.groups,
        repository.favorites,
        repository.favoriteCategories,
        repository.watchHistory,
        prefs.settingsFlow,
        selectedCategory,
        searchQuery,
        isReady,
        networkMonitor.isOnline,
        authRole,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val all = values[0] as List<ChannelEntity>
        @Suppress("UNCHECKED_CAST")
        val enabled = values[1] as List<ChannelEntity>
        @Suppress("UNCHECKED_CAST")
        val groups = values[2] as List<ChannelGroupEntity>
        @Suppress("UNCHECKED_CAST")
        val favs = values[3] as List<com.example.data.model.FavoriteEntity>
        @Suppress("UNCHECKED_CAST")
        val favCats = values[4] as List<com.example.data.model.FavoriteCategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val history = values[5] as List<com.example.data.model.WatchHistoryEntity>
        val settings = values[6] as UserSettings
        val selCat = values[7] as String?
        val query = values[8] as String
        val ready = values[9] as Boolean
        val online = values[10] as Boolean
        val role = values[11] as UserRole

        val enabledById = enabled.associateBy { it.id }
        val favIds = favs.map { it.channelId }.toSet()
        val favCatNames = favCats.map { it.categoryName }.toSet()

        val featured = enabled
            .filter { it.featured }
            .sortedWith(compareBy({ it.featuredOrder }, { it.orderIndex }))
            .ifEmpty { enabled.take(5) }

        val recent = history.mapNotNull { enabledById[it.channelId] }
        val favChannels = favs.mapNotNull { enabledById[it.channelId] }

        val activeGroupNames = enabled.map { it.groupName }.toSet()
        val sortedActiveGroups = groups
            .filter { it.name in activeGroupNames }
            .sortedWith(compareBy({ it.orderIndex }, { it.name }))

        val sortedAllGroups = groups.sortedWith(compareBy({ it.orderIndex }, { it.name }))

        MainUiState(
            isReady = ready,
            authRole = role,
            allChannels = all,
            enabledChannels = enabled,
            featuredChannels = featured,
            recentlyWatchedChannels = recent,
            favoriteChannels = favChannels,
            favoriteIds = favIds,
            favoriteCategories = favCatNames,
            groups = sortedActiveGroups,
            allGroups = sortedAllGroups,
            selectedCategory = selCat,
            searchQuery = query,
            settings = settings,
            isOnline = online,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(),
    )

    init {
        viewModelScope.launch {
            repository.ensureBundledPlaylistSeeded()
            val currentSettings = prefs.settingsFlow.first()
            if (currentSettings.hasSelectedLanguage &&
                (!currentSettings.userLoginRequired || currentSettings.hasRememberedUserSession)
            ) {
                authRole.value = UserRole.USER
            }
            isReady.value = true

            if (authRole.value == UserRole.USER) {
                val lastId = currentSettings.lastWatchedChannelId
                if (currentSettings.autoPlayLastChannel && !lastId.isNullOrBlank()) {
                    _events.emit(UiFeedbackEvent.AutoOpenPlayer(lastId))
                }
            }
        }
    }

    fun confirmLanguageSelection(language: AppLanguage) {
        viewModelScope.launch {
            prefs.setLanguage(language, markSelected = true)
            val currentSettings = prefs.settingsFlow.first()
            if (!currentSettings.userLoginRequired || currentSettings.hasRememberedUserSession) {
                authRole.value = UserRole.USER
            }
        }
    }

    fun attemptUserLogin(code: String, rememberSession: Boolean): AuthVerificationResult {
        val currentSettings = uiState.value.settings
        val result = accessControl.verifyUserCode(code, currentSettings.customUserCredential)
        if (result is AuthVerificationResult.Success) {
            authRole.value = UserRole.USER
            viewModelScope.launch {
                val shouldSave = rememberSession && currentSettings.rememberUserSession
                prefs.saveUserSession(shouldSave)
                val lastId = currentSettings.lastWatchedChannelId
                if (currentSettings.autoPlayLastChannel && !lastId.isNullOrBlank()) {
                    _events.emit(UiFeedbackEvent.AutoOpenPlayer(lastId))
                }
            }
        }
        return result
    }

    fun attemptAdminLogin(code: String): AuthVerificationResult {
        val currentSettings = uiState.value.settings
        val result = accessControl.verifyAdminCode(code, currentSettings.customAdminCredential)
        if (result is AuthVerificationResult.Success) {
            authRole.value = UserRole.ADMIN
            viewModelScope.launch {
                _events.emit(UiFeedbackEvent.AdminAuthenticated)
            }
        }
        return result
    }

    fun logoutUser() {
        viewModelScope.launch {
            prefs.clearUserSession()
            authRole.value = UserRole.NONE
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.LOGGED_OUT))
            _events.emit(UiFeedbackEvent.LoggedOut)
        }
    }

    fun logoutAdmin() {
        viewModelScope.launch {
            authRole.value = UserRole.NONE
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.LOGGED_OUT))
            _events.emit(UiFeedbackEvent.LoggedOut)
        }
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { prefs.setLanguage(language, markSelected = true) }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(themeMode) }
    }

    fun setAutoPlayLastChannel(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoPlayLastChannel(enabled) }
    }

    fun setAutoPlayNextChannel(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoPlayNextChannel(enabled) }
    }

    fun setPlayerEngineMode(mode: PlayerEngineMode) {
        viewModelScope.launch { prefs.setPlayerEngineMode(mode) }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch { repository.toggleFavorite(channelId) }
    }

    fun toggleFavoriteCategory(categoryName: String) {
        viewModelScope.launch { repository.toggleFavoriteCategory(categoryName) }
    }

    fun onChannelPlaybackStarted(channelId: String) {
        _activePlayerChannelId.value = channelId
        viewModelScope.launch { repository.recordWatchHistory(channelId) }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearAppCache()
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CACHE_CLEARED))
        }
    }

    // --- Admin-only mutations (strictly guarded by UserRole.ADMIN) ---

    private suspend fun requireAdmin(): Boolean {
        if (authRole.value != UserRole.ADMIN) {
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.ADMIN_REQUIRED))
            return false
        }
        return true
    }

    fun saveChannel(
        existingId: String?,
        name: String,
        groupName: String,
        logo: String,
        backdrop: String,
        url: String,
        url2: String,
        url3: String,
        orderIndex: Int,
        userAgent: String,
        referer: String,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            val existing = if (existingId != null) {
                uiState.value.allChannels.firstOrNull { it.id == existingId }
            } else {
                null
            }
            val channel = (existing ?: ChannelEntity(
                id = "custom_${UUID.randomUUID().toString().take(8)}",
                name = name.trim(),
                groupName = groupName.trim().ifEmpty { "KURD TV" },
                isCustom = true,
            )).copy(
                name = name.trim(),
                groupName = groupName.trim().ifEmpty { "KURD TV" },
                logo = logo.trim(),
                backdrop = backdrop.trim(),
                url = url.trim(),
                url2 = url2.trim(),
                url3 = url3.trim(),
                orderIndex = orderIndex,
                userAgent = userAgent.trim(),
                referer = referer.trim(),
                enabled = enabled,
            )
            repository.saveChannel(channel)
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CHANGES_SAVED))
        }
    }

    fun toggleChannelEnabled(channelId: String, enabled: Boolean) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            repository.setChannelEnabled(channelId, enabled)
        }
    }

    fun moveChannelUp(channel: ChannelEntity, orderedList: List<ChannelEntity>) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            val idx = orderedList.indexOfFirst { it.id == channel.id }
            if (idx > 0) {
                repository.swapChannelOrder(channel, orderedList[idx - 1])
            }
        }
    }

    fun moveChannelDown(channel: ChannelEntity, orderedList: List<ChannelEntity>) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            val idx = orderedList.indexOfFirst { it.id == channel.id }
            if (idx in 0 until orderedList.lastIndex) {
                repository.swapChannelOrder(channel, orderedList[idx + 1])
            }
        }
    }

    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            repository.deleteChannel(channelId)
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CHANGES_SAVED))
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            repository.addGroup(name)
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CHANGES_SAVED))
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            repository.renameGroup(oldName, newName)
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CHANGES_SAVED))
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            repository.deleteGroup(name)
            if (selectedCategory.value == name) {
                selectedCategory.value = null
            }
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CHANGES_SAVED))
        }
    }

    fun moveCategoryUp(group: ChannelGroupEntity) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            val list = uiState.value.allGroups
            val idx = list.indexOfFirst { it.name == group.name }
            if (idx > 0) {
                repository.swapGroupOrder(group, list[idx - 1])
            }
        }
    }

    fun moveCategoryDown(group: ChannelGroupEntity) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            val list = uiState.value.allGroups
            val idx = list.indexOfFirst { it.name == group.name }
            if (idx in 0 until list.lastIndex) {
                repository.swapGroupOrder(group, list[idx + 1])
            }
        }
    }

    fun importPlaylistJson(jsonText: String) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            val result = repository.importPlaylistJson(jsonText)
            result.onSuccess { count ->
                _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.IMPORT_SUCCESS, count))
            }.onFailure {
                _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.IMPORT_ERROR))
            }
        }
    }

    fun requestExportPlaylist() {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            val json = repository.exportPlaylistJson()
            _events.emit(UiFeedbackEvent.ExportReady(json))
        }
    }

    fun notifyExportSuccess() {
        viewModelScope.launch {
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.EXPORT_SUCCESS))
        }
    }

    fun restoreBundledPlaylist() {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            val count = repository.restoreBundledPlaylist()
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.RESTORE_SUCCESS, count))
        }
    }

    fun setUserLoginRequired(required: Boolean) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            prefs.setUserLoginRequired(required)
        }
    }

    fun setAllowRememberSession(allowed: Boolean) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            prefs.setRememberUserSession(allowed)
        }
    }

    fun updateUserAccessCode(newCode: String) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            val clean = newCode.trim()
            if (clean.length < 4 || !clean.all { it.isDigit() }) {
                _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CODE_VALIDATION_ERROR))
                return@launch
            }
            prefs.updateUserCredential(accessControl.createCredential(clean))
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CODE_UPDATED))
        }
    }

    fun updateAdminAccessCode(newCode: String) {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            val clean = newCode.trim()
            if (clean.length < 4 || !clean.all { it.isDigit() }) {
                _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CODE_VALIDATION_ERROR))
                return@launch
            }
            prefs.updateAdminCredential(accessControl.createCredential(clean))
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CODE_UPDATED))
        }
    }

    fun clearAllUserSessions() {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            prefs.clearUserSession()
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.SESSIONS_CLEARED))
        }
    }

    fun notifyAdminChangesSaved() {
        viewModelScope.launch {
            if (!requireAdmin()) return@launch
            _events.emit(UiFeedbackEvent.ToastMessage(FeedbackType.CHANGES_SAVED))
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.release()
    }
}
