package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PlayerEngineMode
import com.example.i18n.AppLanguage
import com.example.security.AccessControlManager
import com.example.security.StoredCredential
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "d09_tv_prefs")

data class UserSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val hasSelectedLanguage: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.MIDNIGHT_NAVY,
    val autoPlayLastChannel: Boolean = false,
    val autoPlayNextChannel: Boolean = false,
    val playerEngineMode: PlayerEngineMode = PlayerEngineMode.STANDARD,
    val lastWatchedChannelId: String? = null,
    val initialPlaylistSeeded: Boolean = false,
    val userLoginRequired: Boolean = true,
    val rememberUserSession: Boolean = true,
    val hasRememberedUserSession: Boolean = false,
    val customUserCredential: StoredCredential? = null,
    val customAdminCredential: StoredCredential? = null,
)

class UserPreferencesRepository(
    private val context: Context,
    private val accessControl: AccessControlManager = AccessControlManager(),
) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val HAS_SELECTED_LANGUAGE = booleanPreferencesKey("has_selected_language")
        val THEME = stringPreferencesKey("theme_mode")
        val AUTO_PLAY_LAST = booleanPreferencesKey("auto_play_last")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val PLAYER_MODE = stringPreferencesKey("player_engine_mode")
        val LAST_WATCHED_CHANNEL = stringPreferencesKey("last_watched_channel_id")
        val INITIAL_SEEDED = booleanPreferencesKey("initial_playlist_seeded")
        val USER_LOGIN_REQUIRED = booleanPreferencesKey("user_login_required")
        val REMEMBER_USER_SESSION = booleanPreferencesKey("remember_user_session")
        val USER_SESSION_TOKEN = stringPreferencesKey("user_session_token_sealed")
        val USER_CODE_SALT = stringPreferencesKey("user_code_salt_sealed")
        val USER_CODE_HASH = stringPreferencesKey("user_code_hash_sealed")
        val ADMIN_CODE_SALT = stringPreferencesKey("admin_code_salt_sealed")
        val ADMIN_CODE_HASH = stringPreferencesKey("admin_code_hash_sealed")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val userSalt = accessControl.unsealString(prefs[Keys.USER_CODE_SALT])
        val userHash = accessControl.unsealString(prefs[Keys.USER_CODE_HASH])
        val customUserCred = if (!userSalt.isNullOrBlank() && !userHash.isNullOrBlank()) {
            StoredCredential(userSalt, userHash)
        } else {
            null
        }

        val adminSalt = accessControl.unsealString(prefs[Keys.ADMIN_CODE_SALT])
        val adminHash = accessControl.unsealString(prefs[Keys.ADMIN_CODE_HASH])
        val customAdminCred = if (!adminSalt.isNullOrBlank() && !adminHash.isNullOrBlank()) {
            StoredCredential(adminSalt, adminHash)
        } else {
            null
        }

        val rememberSession = prefs[Keys.REMEMBER_USER_SESSION] ?: true
        val token = accessControl.unsealString(prefs[Keys.USER_SESSION_TOKEN])

        UserSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            hasSelectedLanguage = prefs[Keys.HAS_SELECTED_LANGUAGE] ?: false,
            themeMode = AppThemeMode.fromId(prefs[Keys.THEME]),
            autoPlayLastChannel = prefs[Keys.AUTO_PLAY_LAST] ?: false,
            autoPlayNextChannel = prefs[Keys.AUTO_PLAY_NEXT] ?: false,
            playerEngineMode = PlayerEngineMode.fromId(prefs[Keys.PLAYER_MODE]),
            lastWatchedChannelId = prefs[Keys.LAST_WATCHED_CHANNEL],
            initialPlaylistSeeded = prefs[Keys.INITIAL_SEEDED] ?: false,
            userLoginRequired = prefs[Keys.USER_LOGIN_REQUIRED] ?: true,
            rememberUserSession = rememberSession,
            hasRememberedUserSession = rememberSession && !token.isNullOrBlank(),
            customUserCredential = customUserCred,
            customAdminCredential = customAdminCred,
        )
    }

    suspend fun setLanguage(language: AppLanguage, markSelected: Boolean = true) {
        context.dataStore.edit {
            it[Keys.LANGUAGE] = language.code
            if (markSelected) {
                it[Keys.HAS_SELECTED_LANGUAGE] = true
            }
        }
    }

    suspend fun markLanguageSelected() {
        context.dataStore.edit { it[Keys.HAS_SELECTED_LANGUAGE] = true }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = themeMode.id }
    }

    suspend fun setAutoPlayLastChannel(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_LAST] = enabled }
    }

    suspend fun setAutoPlayNextChannel(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setPlayerEngineMode(mode: PlayerEngineMode) {
        context.dataStore.edit { it[Keys.PLAYER_MODE] = mode.id }
    }

    suspend fun setLastWatchedChannelId(channelId: String?) {
        context.dataStore.edit {
            if (channelId == null) {
                it.remove(Keys.LAST_WATCHED_CHANNEL)
            } else {
                it[Keys.LAST_WATCHED_CHANNEL] = channelId
            }
        }
    }

    suspend fun markInitialPlaylistSeeded() {
        context.dataStore.edit { it[Keys.INITIAL_SEEDED] = true }
    }

    suspend fun setUserLoginRequired(required: Boolean) {
        context.dataStore.edit { it[Keys.USER_LOGIN_REQUIRED] = required }
    }

    suspend fun setRememberUserSession(remember: Boolean) {
        context.dataStore.edit {
            it[Keys.REMEMBER_USER_SESSION] = remember
            if (!remember) {
                it.remove(Keys.USER_SESSION_TOKEN)
            }
        }
    }

    suspend fun saveUserSession(remember: Boolean) {
        context.dataStore.edit {
            it[Keys.REMEMBER_USER_SESSION] = remember
            if (remember) {
                val token = accessControl.generateSessionToken()
                it[Keys.USER_SESSION_TOKEN] = accessControl.sealString(token)
            } else {
                it.remove(Keys.USER_SESSION_TOKEN)
            }
        }
    }

    suspend fun clearUserSession() {
        context.dataStore.edit {
            it.remove(Keys.USER_SESSION_TOKEN)
        }
    }

    suspend fun updateUserCredential(credential: StoredCredential?) {
        context.dataStore.edit {
            if (credential == null) {
                it.remove(Keys.USER_CODE_SALT)
                it.remove(Keys.USER_CODE_HASH)
            } else {
                it[Keys.USER_CODE_SALT] = accessControl.sealString(credential.saltHex)
                it[Keys.USER_CODE_HASH] = accessControl.sealString(credential.hashHex)
            }
        }
    }

    suspend fun updateAdminCredential(credential: StoredCredential?) {
        context.dataStore.edit {
            if (credential == null) {
                it.remove(Keys.ADMIN_CODE_SALT)
                it.remove(Keys.ADMIN_CODE_HASH)
            } else {
                it[Keys.ADMIN_CODE_SALT] = accessControl.sealString(credential.saltHex)
                it[Keys.ADMIN_CODE_HASH] = accessControl.sealString(credential.hashHex)
            }
        }
    }
}
