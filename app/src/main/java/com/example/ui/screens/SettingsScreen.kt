package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.PlayerEngineMode
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppStrings
import com.example.ui.components.D09LogoBadge
import com.example.ui.components.TvFocusableSurface
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyCyan
import com.example.viewmodel.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onSetAutoPlayLast: (Boolean) -> Unit,
    onSetAutoPlayNext: (Boolean) -> Unit,
    onSetPlayerEngine: (PlayerEngineMode) -> Unit,
    onClearCacheClick: () -> Unit,
    onLogoutUserClick: () -> Unit,
    onOpenAdminPortalClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "settings_header") {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Language Selection
        item(key = "settings_language_section") {
            SettingsCard(
                icon = Icons.Filled.Language,
                title = strings.settingsLanguage,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val selected = uiState.settings.language == lang
                        TvFocusableSurface(
                            onClick = { onSelectLanguage(lang) },
                            shape = RoundedCornerShape(14.dp),
                            containerColor = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.testTag("settings_lang_${lang.code}"),
                        ) { _ ->
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = lang.nativeLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${lang.code.uppercase()} • ${if (lang.isRtl) "RTL" else "LTR"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.White.copy(alpha = 0.85f) else SkyCyan,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Theme Selection
        item(key = "settings_theme_section") {
            SettingsCard(
                icon = Icons.Filled.Palette,
                title = strings.settingsTheme,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val themes = listOf(
                        AppThemeMode.MIDNIGHT_NAVY to strings.themeMidnightNavy,
                        AppThemeMode.OLED_BLACK to strings.themeOledBlack,
                        AppThemeMode.ROYAL_PURPLE to strings.themeRoyalPurple,
                    )
                    themes.forEach { (mode, label) ->
                        val selected = uiState.settings.themeMode == mode
                        TvFocusableSurface(
                            onClick = { onSelectTheme(mode) },
                            shape = RoundedCornerShape(14.dp),
                            containerColor = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.testTag("settings_theme_${mode.id}"),
                        ) { _ ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }

        // Playback & Player Engine
        item(key = "settings_playback_section") {
            SettingsCard(
                icon = Icons.Filled.PlayCircle,
                title = strings.settingsPlayerEngine,
            ) {
                SettingsToggleRow(
                    title = strings.settingsAutoPlayLast,
                    subtitle = strings.settingsAutoPlayLastDesc,
                    checked = uiState.settings.autoPlayLastChannel,
                    onCheckedChange = onSetAutoPlayLast,
                    testTag = "settings_autoplay_last_switch",
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    title = strings.settingsAutoPlayNext,
                    subtitle = strings.settingsAutoPlayNextDesc,
                    checked = uiState.settings.autoPlayNextChannel,
                    onCheckedChange = onSetAutoPlayNext,
                    testTag = "settings_autoplay_next_switch",
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = strings.settingsPlayerEngine,
                        tint = SkyCyan,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = strings.settingsPlayerEngine,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val engines = listOf(
                        PlayerEngineMode.STANDARD to strings.playerEngineStandard,
                        PlayerEngineMode.LOW_LATENCY to strings.playerEngineLowLatency,
                        PlayerEngineMode.HIGH_BUFFER to strings.playerEngineHighBuffer,
                    )
                    engines.forEach { (mode, label) ->
                        val selected = uiState.settings.playerEngineMode == mode
                        TvFocusableSurface(
                            onClick = { onSetPlayerEngine(mode) },
                            shape = RoundedCornerShape(14.dp),
                            containerColor = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            modifier = Modifier.testTag("settings_engine_${mode.id}"),
                        ) { _ ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }

        // User Actions: Clear Cache, Logout, Admin Portal (no channel links or admin settings exposed here)
        item(key = "settings_account_section") {
            SettingsCard(
                icon = Icons.Filled.AdminPanelSettings,
                title = strings.settingsPlaylistData,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SettingsActionButton(
                        icon = Icons.Filled.CleaningServices,
                        label = strings.settingsClearCache,
                        tint = SkyCyan,
                        onClick = onClearCacheClick,
                        testTag = "settings_clear_cache_button",
                    )
                    SettingsActionButton(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        label = strings.logoutButton,
                        tint = LiveCrimson,
                        onClick = onLogoutUserClick,
                        testTag = "settings_logout_button",
                    )
                    SettingsActionButton(
                        icon = Icons.Filled.AdminPanelSettings,
                        label = strings.adminPortalButton,
                        tint = RoyalPurple,
                        onClick = onOpenAdminPortalClick,
                        testTag = "settings_admin_portal_button",
                    )
                }
            }
        }

        // App & Developer Info
        item(key = "settings_info_section") {
            SettingsCard(
                icon = Icons.Filled.Info,
                title = strings.settingsAppInfo,
            ) {
                D09LogoBadge(iconSize = 44.dp, showSubtitle = true)
                Spacer(modifier = Modifier.height(10.dp))

                InfoKeyValueRow(label = strings.settingsDeveloper, value = strings.settingsDeveloperName)
                InfoKeyValueRow(label = strings.settingsVersion, value = BuildConfig.VERSION_NAME)
                InfoKeyValueRow(
                    label = strings.settingsTotalChannels,
                    value = uiState.enabledChannels.size.toString(),
                )
                InfoKeyValueRow(
                    label = strings.settingsTotalCategories,
                    value = uiState.groups.size.toString(),
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SkyCyan,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    TvFocusableSurface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    ) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun SettingsActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    TvFocusableSurface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag(testTag),
    ) { _ ->
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InfoKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
