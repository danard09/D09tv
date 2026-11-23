package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChannelEntity
import com.example.data.model.ChannelGroupEntity
import com.example.data.model.PlayerEngineMode
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppStrings
import com.example.security.UserRole
import com.example.ui.components.ChannelLogoArtwork
import com.example.ui.components.D09LogoBadge
import com.example.ui.components.TvFocusableSurface
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyCyan
import com.example.viewmodel.MainUiState

private enum class AdminTab {
    OVERVIEW,
    CHANNELS,
    LOGOS,
    CATEGORIES,
    PLAYLIST_SETTINGS,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    uiState: MainUiState,
    onPlayChannel: (ChannelEntity) -> Unit,
    onSaveChannel: (
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
    ) -> Unit,
    onToggleChannelEnabled: (String, Boolean) -> Unit,
    onMoveChannelUp: (ChannelEntity, List<ChannelEntity>) -> Unit,
    onMoveChannelDown: (ChannelEntity, List<ChannelEntity>) -> Unit,
    onDeleteChannel: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onMoveCategoryUp: (ChannelGroupEntity) -> Unit,
    onMoveCategoryDown: (ChannelGroupEntity) -> Unit,
    onImportJsonFileClick: () -> Unit,
    onPasteJsonSubmit: (String) -> Unit,
    onExportJsonClick: () -> Unit,
    onShareJsonClick: () -> Unit,
    onRestoreBundledClick: () -> Unit,
    onSetUserLoginRequired: (Boolean) -> Unit,
    onSetAllowRememberSession: (Boolean) -> Unit,
    onUpdateUserCode: (String) -> Unit,
    onUpdateAdminCode: (String) -> Unit,
    onClearSavedSessions: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onSetAutoPlayLast: (Boolean) -> Unit,
    onSetAutoPlayNext: (Boolean) -> Unit,
    onSetPlayerEngine: (PlayerEngineMode) -> Unit,
    onSaveChangesClick: () -> Unit,
    onLogoutAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current

    // Strict UI defense-in-depth: never render dashboard unless ADMIN is authenticated
    if (uiState.authRole != UserRole.ADMIN) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = strings.accessDeniedAdminOnly,
                style = MaterialTheme.typography.titleLarge,
                color = LiveCrimson,
            )
        }
        return
    }

    var activeTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    var filterCategory by remember { mutableStateOf<String?>(null) }

    var editingChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var showChannelEditor by remember { mutableStateOf(false) }
    var channelForLinksEditor by remember { mutableStateOf<ChannelEntity?>(null) }
    var channelForLogoEditor by remember { mutableStateOf<ChannelEntity?>(null) }
    var channelPendingDelete by remember { mutableStateOf<ChannelEntity?>(null) }
    var showPasteJsonDialog by remember { mutableStateOf(false) }

    val availableCategories = remember(uiState.allChannels, uiState.allGroups) {
        (uiState.allGroups.map { it.name } + uiState.allChannels.map { it.groupName })
            .filter { it.isNotBlank() }
            .distinct()
    }

    val filteredChannels = remember(uiState.allChannels, filterCategory) {
        if (filterCategory == null) uiState.allChannels
        else uiState.allChannels.filter { it.groupName == filterCategory }
    }

    val activeCount = remember(uiState.allChannels) { uiState.allChannels.count { it.enabled } }
    val disabledCount = uiState.allChannels.size - activeCount

    BackHandler {
        if (activeTab != AdminTab.OVERVIEW) {
            activeTab = AdminTab.OVERVIEW
        } else {
            onLogoutAdmin()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Admin Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                D09LogoBadge(iconSize = 38.dp, showSubtitle = false)
                Column {
                    Text(
                        text = strings.adminDashboardTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${strings.adminTotalChannels}: ${uiState.allChannels.size} • ${strings.adminActiveChannels}: $activeCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvFocusableSurface(
                    onClick = onSaveChangesClick,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = EmeraldLive.copy(alpha = 0.22f),
                    unfocusedBorderColor = EmeraldLive.copy(alpha = 0.6f),
                    modifier = Modifier.testTag("admin_save_changes_top"),
                ) { _ ->
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = strings.adminSaveChanges,
                            tint = EmeraldLive,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = strings.save,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                }

                TvFocusableSurface(
                    onClick = onLogoutAdmin,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = LiveCrimson.copy(alpha = 0.20f),
                    unfocusedBorderColor = LiveCrimson.copy(alpha = 0.6f),
                    modifier = Modifier.testTag("admin_logout_top"),
                ) { _ ->
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = strings.adminLogout,
                            tint = LiveCrimson,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = strings.logoutButton,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // Admin Section Tabs (D-pad & Touch)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val tabs = listOf(
                Triple(AdminTab.OVERVIEW, strings.adminTabOverview, Icons.Filled.Dashboard),
                Triple(AdminTab.CHANNELS, strings.adminTabChannels, Icons.Filled.LiveTv),
                Triple(AdminTab.LOGOS, strings.adminTabLogos, Icons.Filled.Image),
                Triple(AdminTab.CATEGORIES, strings.adminTabCategories, Icons.Filled.Category),
                Triple(AdminTab.PLAYLIST_SETTINGS, strings.adminTabPlaylistSettings, Icons.Filled.Security),
            )
            items(tabs, key = { it.first.name }) { (tab, label, icon) ->
                val selected = activeTab == tab
                TvFocusableSurface(
                    onClick = { activeTab = tab },
                    shape = RoundedCornerShape(14.dp),
                    containerColor = if (selected) RoyalPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    focusedContainerColor = if (selected) RoyalPurple else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}"),
                ) { _ ->
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) Color.White else SkyCyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // Active Tab Body
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                AdminTab.OVERVIEW -> {
                    AdminOverviewContent(
                        totalChannels = uiState.allChannels.size,
                        activeChannels = activeCount,
                        disabledChannels = disabledCount,
                        totalCategories = uiState.allGroups.size,
                        onAddChannel = {
                            editingChannel = null
                            showChannelEditor = true
                        },
                        onEditChannels = { activeTab = AdminTab.CHANNELS },
                        onManageLogos = { activeTab = AdminTab.LOGOS },
                        onManageCategories = { activeTab = AdminTab.CATEGORIES },
                        onManageLinks = { activeTab = AdminTab.CHANNELS },
                        onImportPlaylist = onImportJsonFileClick,
                        onExportPlaylist = onExportJsonClick,
                        onUserAccessSettings = { activeTab = AdminTab.PLAYLIST_SETTINGS },
                        onSaveChanges = onSaveChangesClick,
                        onLogout = onLogoutAdmin,
                    )
                }

                AdminTab.CHANNELS -> {
                    AdminChannelsContent(
                        channels = filteredChannels,
                        categories = availableCategories,
                        selectedCategory = filterCategory,
                        onSelectCategory = { filterCategory = it },
                        onAddChannel = {
                            editingChannel = null
                            showChannelEditor = true
                        },
                        onPlayChannel = onPlayChannel,
                        onEditChannel = {
                            editingChannel = it
                            showChannelEditor = true
                        },
                        onEditLinks = { channelForLinksEditor = it },
                        onToggleEnabled = onToggleChannelEnabled,
                        onMoveUp = { ch -> onMoveChannelUp(ch, filteredChannels) },
                        onMoveDown = { ch -> onMoveChannelDown(ch, filteredChannels) },
                        onDelete = { channelPendingDelete = it },
                    )
                }

                AdminTab.LOGOS -> {
                    AdminLogosContent(
                        channels = uiState.allChannels,
                        onEditLogo = { channelForLogoEditor = it },
                    )
                }

                AdminTab.CATEGORIES -> {
                    AdminCategoriesContent(
                        groups = uiState.allGroups,
                        channels = uiState.allChannels,
                        onAddCategory = onAddCategory,
                        onRenameCategory = onRenameCategory,
                        onDeleteCategory = onDeleteCategory,
                        onMoveUp = onMoveCategoryUp,
                        onMoveDown = onMoveCategoryDown,
                    )
                }

                AdminTab.PLAYLIST_SETTINGS -> {
                    AdminPlaylistAndAccessContent(
                        uiState = uiState,
                        onImportJsonFileClick = onImportJsonFileClick,
                        onOpenPasteJson = { showPasteJsonDialog = true },
                        onExportJsonClick = onExportJsonClick,
                        onShareJsonClick = onShareJsonClick,
                        onRestoreBundledClick = onRestoreBundledClick,
                        onSetUserLoginRequired = onSetUserLoginRequired,
                        onSetAllowRememberSession = onSetAllowRememberSession,
                        onUpdateUserCode = onUpdateUserCode,
                        onUpdateAdminCode = onUpdateAdminCode,
                        onClearSavedSessions = onClearSavedSessions,
                        onSelectLanguage = onSelectLanguage,
                        onSelectTheme = onSelectTheme,
                        onSetAutoPlayLast = onSetAutoPlayLast,
                        onSetAutoPlayNext = onSetAutoPlayNext,
                        onSetPlayerEngine = onSetPlayerEngine,
                        onSaveChangesClick = onSaveChangesClick,
                    )
                }
            }
        }
    }

    if (showChannelEditor) {
        AdminChannelEditorDialog(
            initialChannel = editingChannel,
            availableCategories = availableCategories,
            suggestedOrder = (uiState.allChannels.maxOfOrNull { it.orderIndex } ?: 0) + 1,
            onDismiss = { showChannelEditor = false },
            onSave = { name, group, logo, backdrop, url, url2, url3, order, ua, referer, enabled ->
                onSaveChannel(
                    editingChannel?.id,
                    name,
                    group,
                    logo,
                    backdrop,
                    url,
                    url2,
                    url3,
                    order,
                    ua,
                    referer,
                    enabled,
                )
                showChannelEditor = false
            },
        )
    }

    val linksTarget = channelForLinksEditor
    if (linksTarget != null) {
        AdminLinksQuickEditorDialog(
            channel = linksTarget,
            onDismiss = { channelForLinksEditor = null },
            onSave = { url, url2, url3, ua, referer ->
                onSaveChannel(
                    linksTarget.id,
                    linksTarget.name,
                    linksTarget.groupName,
                    linksTarget.logo,
                    linksTarget.backdrop,
                    url,
                    url2,
                    url3,
                    linksTarget.orderIndex,
                    ua,
                    referer,
                    linksTarget.enabled,
                )
                channelForLinksEditor = null
            },
        )
    }

    val logoTarget = channelForLogoEditor
    if (logoTarget != null) {
        AdminLogoQuickEditorDialog(
            channel = logoTarget,
            onDismiss = { channelForLogoEditor = null },
            onSave = { logo, backdrop ->
                onSaveChannel(
                    logoTarget.id,
                    logoTarget.name,
                    logoTarget.groupName,
                    logo,
                    backdrop,
                    logoTarget.url,
                    logoTarget.url2,
                    logoTarget.url3,
                    logoTarget.orderIndex,
                    logoTarget.userAgent,
                    logoTarget.referer,
                    logoTarget.enabled,
                )
                channelForLogoEditor = null
            },
        )
    }

    val pendingDelete = channelPendingDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { channelPendingDelete = null },
            title = { Text(strings.deleteConfirmTitle) },
            text = { Text(strings.deleteConfirmMessage(pendingDelete.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChannel(pendingDelete.id)
                        channelPendingDelete = null
                    },
                    modifier = Modifier.testTag("admin_confirm_delete_channel"),
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { channelPendingDelete = null }) { Text(strings.cancel) }
            },
        )
    }

    if (showPasteJsonDialog) {
        var pasteText by remember { mutableStateOf("") }
        BackHandler { showPasteJsonDialog = false }
        AlertDialog(
            onDismissRequest = { showPasteJsonDialog = false },
            title = { Text(strings.pasteJsonTitle) },
            text = {
                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    placeholder = { Text(strings.pasteJsonHint) },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_paste_json_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteText.isNotBlank()) {
                            onPasteJsonSubmit(pasteText)
                            showPasteJsonDialog = false
                        }
                    },
                    modifier = Modifier.testTag("admin_paste_json_confirm"),
                ) { Text(strings.importAction) }
            },
            dismissButton = {
                TextButton(onClick = { showPasteJsonDialog = false }) { Text(strings.cancel) }
            },
        )
    }
}

@Composable
private fun AdminOverviewContent(
    totalChannels: Int,
    activeChannels: Int,
    disabledChannels: Int,
    totalCategories: Int,
    onAddChannel: () -> Unit,
    onEditChannels: () -> Unit,
    onManageLogos: () -> Unit,
    onManageCategories: () -> Unit,
    onManageLinks: () -> Unit,
    onImportPlaylist: () -> Unit,
    onExportPlaylist: () -> Unit,
    onUserAccessSettings: () -> Unit,
    onSaveChanges: () -> Unit,
    onLogout: () -> Unit,
) {
    val strings = LocalAppStrings.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        // KPI Cards
        item(key = "kpi_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AdminStatCard(
                    title = strings.adminTotalChannels,
                    value = totalChannels.toString(),
                    accent = ElectricBlue,
                    modifier = Modifier.weight(1f),
                )
                AdminStatCard(
                    title = strings.adminActiveChannels,
                    value = activeChannels.toString(),
                    accent = EmeraldLive,
                    modifier = Modifier.weight(1f),
                )
                AdminStatCard(
                    title = strings.adminDisabledChannels,
                    value = disabledChannels.toString(),
                    accent = AmberGold,
                    modifier = Modifier.weight(1f),
                )
                AdminStatCard(
                    title = strings.adminTotalCategories,
                    value = totalCategories.toString(),
                    accent = RoyalPurple,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Quick Action Grid
        val actions = listOf(
            Triple(strings.addChannel, Icons.Filled.Add, onAddChannel) to "admin_action_add_channel",
            Triple(strings.adminEditChannels, Icons.Filled.Edit, onEditChannels) to "admin_action_edit_channels",
            Triple(strings.adminManageLogos, Icons.Filled.Image, onManageLogos) to "admin_action_manage_logos",
            Triple(strings.adminManageCategories, Icons.Filled.Category, onManageCategories) to "admin_action_manage_categories",
            Triple(strings.adminManageLinks, Icons.Filled.Link, onManageLinks) to "admin_action_manage_links",
            Triple(strings.adminImportPlaylist, Icons.Filled.FileOpen, onImportPlaylist) to "admin_action_import",
            Triple(strings.adminExportPlaylist, Icons.Filled.Download, onExportPlaylist) to "admin_action_export",
            Triple(strings.adminUserAccessTitle, Icons.Filled.Security, onUserAccessSettings) to "admin_action_access",
            Triple(strings.adminSaveChanges, Icons.Filled.Save, onSaveChanges) to "admin_action_save",
            Triple(strings.adminLogout, Icons.AutoMirrored.Filled.Logout, onLogout) to "admin_action_logout",
        )

        items(actions.chunked(2)) { rowPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowPair.forEach { (itemTriple, tag) ->
                    val (label, icon, onClick) = itemTriple
                    TvFocusableSurface(
                        onClick = onClick,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(tag),
                    ) { _ ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = SkyCyan,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (rowPair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = accent,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AdminChannelsContent(
    channels: List<ChannelEntity>,
    categories: List<String>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    onAddChannel: () -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit,
    onEditChannel: (ChannelEntity) -> Unit,
    onEditLinks: (ChannelEntity) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onMoveUp: (ChannelEntity) -> Unit,
    onMoveDown: (ChannelEntity) -> Unit,
    onDelete: (ChannelEntity) -> Unit,
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "adm_cat_all") {
                    val active = selectedCategory == null
                    TvFocusableSurface(
                        onClick = { onSelectCategory(null) },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    ) { _ ->
                        Text(
                            text = strings.allCategories,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
                items(categories, key = { "adm_cat_$it" }) { cat ->
                    val active = selectedCategory == cat
                    TvFocusableSurface(
                        onClick = { onSelectCategory(cat) },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    ) { _ ->
                        Text(
                            text = strings.translateCategory(cat),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAddChannel,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("admin_add_channel_button"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = strings.addChannel, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.addChannel)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("admin_channel_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(channels, key = { "adm_ch_${it.id}" }) { ch ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0A1022))
                            .padding(4.dp),
                    ) {
                        ChannelLogoArtwork(
                            logoUrl = ch.logo,
                            channelName = ch.name,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#${ch.orderIndex}  ${ch.name}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${strings.translateCategory(ch.groupName)} • ${strings.backupSourcesCount(ch.availableStreams().size)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SkyCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Switch(
                        checked = ch.enabled,
                        onCheckedChange = { onToggleEnabled(ch.id, it) },
                        modifier = Modifier.testTag("admin_enable_${ch.id}"),
                    )

                    IconButton(
                        onClick = { onMoveUp(ch) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = strings.adminMoveUp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(
                        onClick = { onMoveDown(ch) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = strings.adminMoveDown, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(
                        onClick = { onEditLinks(ch) },
                        modifier = Modifier.size(48.dp).testTag("admin_links_${ch.id}"),
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = strings.adminEditLinksTitle, tint = SkyCyan)
                    }

                    IconButton(
                        onClick = { onEditChannel(ch) },
                        modifier = Modifier.size(48.dp).testTag("admin_edit_${ch.id}"),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = strings.editChannel, tint = Color.White)
                    }

                    IconButton(
                        onClick = { onPlayChannel(ch) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = strings.watchNow, tint = EmeraldLive)
                    }

                    IconButton(
                        onClick = { onDelete(ch) },
                        modifier = Modifier.size(48.dp).testTag("admin_delete_${ch.id}"),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = strings.deleteChannel, tint = LiveCrimson)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLogosContent(
    channels: List<ChannelEntity>,
    onEditLogo: (ChannelEntity) -> Unit,
) {
    val strings = LocalAppStrings.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_logos_grid"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(channels, key = { "logo_${it.id}" }) { ch ->
            TvFocusableSurface(
                onClick = { onEditLogo(ch) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("admin_logo_card_${ch.id}"),
            ) { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF090F21)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (ch.backdrop.isNotBlank()) {
                            ChannelLogoArtwork(
                                logoUrl = ch.backdrop,
                                channelName = ch.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xCC060B18))
                                .padding(6.dp),
                        ) {
                            ChannelLogoArtwork(
                                logoUrl = ch.logo,
                                channelName = ch.name,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Text(
                        text = ch.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = strings.adminEditLogoTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyCyan,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminCategoriesContent(
    groups: List<ChannelGroupEntity>,
    channels: List<ChannelEntity>,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onMoveUp: (ChannelGroupEntity) -> Unit,
    onMoveDown: (ChannelGroupEntity) -> Unit,
) {
    val strings = LocalAppStrings.current
    var newCategoryName by remember { mutableStateOf("") }
    var groupToRename by remember { mutableStateOf<ChannelGroupEntity?>(null) }
    var groupToDelete by remember { mutableStateOf<ChannelGroupEntity?>(null) }

    val counts = remember(channels) { channels.groupingBy { it.groupName }.eachCount() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                placeholder = { Text(strings.adminCategoryNameHint) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_new_category_input"),
            )
            Button(
                onClick = {
                    if (newCategoryName.isNotBlank()) {
                        onAddCategory(newCategoryName)
                        newCategoryName = ""
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("admin_add_category_button"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = strings.adminCategoryAdd)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.adminCategoryAdd)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("admin_categories_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(groups, key = { "adm_grp_${it.name}" }) { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.translateCategory(group.name),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${group.name} • ${strings.channelsCountLabel(counts[group.name] ?: 0)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SkyCyan,
                        )
                    }

                    IconButton(onClick = { onMoveUp(group) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = strings.adminMoveUp)
                    }
                    IconButton(onClick = { onMoveDown(group) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = strings.adminMoveDown)
                    }
                    IconButton(onClick = { groupToRename = group }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = strings.adminCategoryRename, tint = SkyCyan)
                    }
                    IconButton(onClick = { groupToDelete = group }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = strings.adminCategoryDelete, tint = LiveCrimson)
                    }
                }
            }
        }
    }

    val renameTarget = groupToRename
    if (renameTarget != null) {
        var renameValue by remember(renameTarget.name) { mutableStateOf(renameTarget.name) }
        AlertDialog(
            onDismissRequest = { groupToRename = null },
            title = { Text(strings.adminCategoryRename) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameValue.isNotBlank()) {
                            onRenameCategory(renameTarget.name, renameValue)
                            groupToRename = null
                        }
                    },
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { groupToRename = null }) { Text(strings.cancel) }
            },
        )
    }

    val deleteTarget = groupToDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text(strings.adminCategoryDelete) },
            text = { Text(strings.adminCategoryDeleteConfirm(deleteTarget.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(deleteTarget.name)
                        groupToDelete = null
                    },
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text(strings.cancel) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminPlaylistAndAccessContent(
    uiState: MainUiState,
    onImportJsonFileClick: () -> Unit,
    onOpenPasteJson: () -> Unit,
    onExportJsonClick: () -> Unit,
    onShareJsonClick: () -> Unit,
    onRestoreBundledClick: () -> Unit,
    onSetUserLoginRequired: (Boolean) -> Unit,
    onSetAllowRememberSession: (Boolean) -> Unit,
    onUpdateUserCode: (String) -> Unit,
    onUpdateAdminCode: (String) -> Unit,
    onClearSavedSessions: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onSetAutoPlayLast: (Boolean) -> Unit,
    onSetAutoPlayNext: (Boolean) -> Unit,
    onSetPlayerEngine: (PlayerEngineMode) -> Unit,
    onSaveChangesClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    var newUserCode by remember { mutableStateOf("") }
    var newAdminCode by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        // Playlist Import/Export
        item(key = "adm_playlist_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = strings.settingsPlaylistData,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AdminActionChip(Icons.Filled.FileOpen, strings.settingsImportJsonFile, onImportJsonFileClick, "admin_import_file")
                    AdminActionChip(Icons.Filled.Code, strings.settingsImportJsonPaste, onOpenPasteJson, "admin_import_paste")
                    AdminActionChip(Icons.Filled.Download, strings.settingsExportJson, onExportJsonClick, "admin_export_json")
                    AdminActionChip(Icons.Filled.Share, strings.settingsShareJson, onShareJsonClick, "admin_share_json")
                    AdminActionChip(Icons.Filled.Restore, strings.settingsResetDefaultPlaylist, onRestoreBundledClick, "admin_restore_bundled")
                }
            }
        }

        // User Access & Security Settings
        item(key = "adm_access_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, RoyalPurple.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = strings.adminUserAccessTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                AdminToggleItem(
                    title = strings.adminRequireUserLogin,
                    subtitle = strings.adminRequireUserLoginDesc,
                    checked = uiState.settings.userLoginRequired,
                    onCheckedChange = onSetUserLoginRequired,
                )

                AdminToggleItem(
                    title = strings.adminAllowRememberSession,
                    subtitle = strings.adminAllowRememberSessionDesc,
                    checked = uiState.settings.rememberUserSession,
                    onCheckedChange = onSetAllowRememberSession,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newUserCode,
                        onValueChange = { newUserCode = it.filter(Char::isDigit).take(8) },
                        label = { Text(strings.adminUpdateUserCode) },
                        placeholder = { Text(strings.adminNewCodeHint) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            onUpdateUserCode(newUserCode)
                            newUserCode = ""
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(54.dp),
                    ) {
                        Text(strings.adminUpdateCodeButton)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newAdminCode,
                        onValueChange = { newAdminCode = it.filter(Char::isDigit).take(8) },
                        label = { Text(strings.adminUpdateAdminCode) },
                        placeholder = { Text(strings.adminNewCodeHint) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            onUpdateAdminCode(newAdminCode)
                            newAdminCode = ""
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(54.dp),
                    ) {
                        Text(strings.adminUpdateCodeButton)
                    }
                }

                OutlinedButton(
                    onClick = onClearSavedSessions,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.adminClearSavedSessions)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RoyalPurple.copy(alpha = 0.14f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = strings.adminSecurityNoticeTitle,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = SkyCyan,
                    )
                    Text(
                        text = strings.adminSecurityNoticeBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // App Settings (Language, Theme, Player)
        item(key = "adm_app_settings_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val selected = uiState.settings.language == lang
                        TvFocusableSurface(
                            onClick = { onSelectLanguage(lang) },
                            shape = RoundedCornerShape(12.dp),
                            containerColor = if (selected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        ) { _ ->
                            Text(
                                text = lang.nativeLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        AppThemeMode.MIDNIGHT_NAVY to strings.themeMidnightNavy,
                        AppThemeMode.OLED_BLACK to strings.themeOledBlack,
                        AppThemeMode.ROYAL_PURPLE to strings.themeRoyalPurple,
                    ).forEach { (mode, label) ->
                        val selected = uiState.settings.themeMode == mode
                        TvFocusableSurface(
                            onClick = { onSelectTheme(mode) },
                            shape = RoundedCornerShape(12.dp),
                            containerColor = if (selected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        ) { _ ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }

                AdminToggleItem(
                    title = strings.settingsAutoPlayLast,
                    subtitle = strings.settingsAutoPlayLastDesc,
                    checked = uiState.settings.autoPlayLastChannel,
                    onCheckedChange = onSetAutoPlayLast,
                )

                AdminToggleItem(
                    title = strings.settingsAutoPlayNext,
                    subtitle = strings.settingsAutoPlayNextDesc,
                    checked = uiState.settings.autoPlayNextChannel,
                    onCheckedChange = onSetAutoPlayNext,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        PlayerEngineMode.STANDARD to strings.playerEngineStandard,
                        PlayerEngineMode.LOW_LATENCY to strings.playerEngineLowLatency,
                        PlayerEngineMode.HIGH_BUFFER to strings.playerEngineHighBuffer,
                    ).forEach { (mode, label) ->
                        val selected = uiState.settings.playerEngineMode == mode
                        TvFocusableSurface(
                            onClick = { onSetPlayerEngine(mode) },
                            shape = RoundedCornerShape(12.dp),
                            containerColor = if (selected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        ) { _ ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }

                Button(
                    onClick = onSaveChangesClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldLive, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_save_changes_bottom"),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = strings.adminSaveChanges)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.adminSaveChanges, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdminActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
) {
    TvFocusableSurface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag(testTag),
    ) { _ ->
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = label, tint = SkyCyan, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
private fun AdminToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    TvFocusableSurface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun AdminChannelEditorDialog(
    initialChannel: ChannelEntity?,
    availableCategories: List<String>,
    suggestedOrder: Int,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val strings = LocalAppStrings.current

    var name by remember { mutableStateOf(initialChannel?.name ?: "") }
    var groupName by remember {
        mutableStateOf(initialChannel?.groupName ?: availableCategories.firstOrNull() ?: "KURD TV")
    }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var logo by remember { mutableStateOf(initialChannel?.logo ?: "") }
    var backdrop by remember { mutableStateOf(initialChannel?.backdrop ?: "") }
    var url by remember { mutableStateOf(initialChannel?.url ?: "") }
    var url2 by remember { mutableStateOf(initialChannel?.url2 ?: "") }
    var url3 by remember { mutableStateOf(initialChannel?.url3 ?: "") }
    var orderText by remember { mutableStateOf((initialChannel?.orderIndex ?: suggestedOrder).toString()) }
    var userAgent by remember { mutableStateOf(initialChannel?.userAgent ?: "") }
    var referer by remember { mutableStateOf(initialChannel?.referer ?: "") }
    var enabled by remember { mutableStateOf(initialChannel?.enabled ?: true) }
    var showValidationError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (initialChannel == null) strings.addChannel else strings.editChannel,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (showValidationError) {
                    Text(
                        text = strings.validationRequiredNameUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LiveCrimson,
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showValidationError = false
                    },
                    label = { Text(strings.fieldChannelName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("editor_name_input"),
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text(strings.fieldCategory) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { showCategoryDropdown = true },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = strings.fieldCategory)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("editor_category_input"),
                    )
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                    ) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${strings.translateCategory(cat)} ($cat)") },
                                onClick = {
                                    groupName = cat
                                    showCategoryDropdown = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        showValidationError = false
                    },
                    label = { Text(strings.fieldPrimaryUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("editor_url1_input"),
                )

                OutlinedTextField(
                    value = url2,
                    onValueChange = { url2 = it },
                    label = { Text(strings.fieldBackupUrl2) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("editor_url2_input"),
                )

                OutlinedTextField(
                    value = url3,
                    onValueChange = { url3 = it },
                    label = { Text(strings.fieldBackupUrl3) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("editor_url3_input"),
                )

                OutlinedTextField(
                    value = logo,
                    onValueChange = { logo = it },
                    label = { Text(strings.fieldLogoUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("editor_logo_input"),
                )

                OutlinedTextField(
                    value = backdrop,
                    onValueChange = { backdrop = it },
                    label = { Text(strings.fieldBackdropUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("editor_backdrop_input"),
                )

                OutlinedTextField(
                    value = orderText,
                    onValueChange = { orderText = it.filter { ch -> ch.isDigit() || ch == '-' } },
                    label = { Text(strings.fieldOrder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("editor_order_input"),
                )

                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    label = { Text(strings.fieldUserAgent) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("editor_ua_input"),
                )

                OutlinedTextField(
                    value = referer,
                    onValueChange = { referer = it },
                    label = { Text(strings.fieldReferer) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("editor_referer_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(strings.fieldEnabled, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        modifier = Modifier.testTag("editor_enabled_switch"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.testTag("editor_cancel_button")) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = {
                            val hasStream = url.isNotBlank() || url2.isNotBlank() || url3.isNotBlank()
                            if (name.isBlank() || !hasStream) {
                                showValidationError = true
                            } else {
                                onSave(
                                    name,
                                    groupName,
                                    logo,
                                    backdrop,
                                    url,
                                    url2,
                                    url3,
                                    orderText.toIntOrNull() ?: suggestedOrder,
                                    userAgent,
                                    referer,
                                    enabled,
                                )
                            }
                        },
                        modifier = Modifier.testTag("editor_save_button"),
                    ) {
                        Text(strings.save)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLinksQuickEditorDialog(
    channel: ChannelEntity,
    onDismiss: () -> Unit,
    onSave: (url: String, url2: String, url3: String, ua: String, referer: String) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val strings = LocalAppStrings.current
    var url by remember { mutableStateOf(channel.url) }
    var url2 by remember { mutableStateOf(channel.url2) }
    var url3 by remember { mutableStateOf(channel.url3) }
    var ua by remember { mutableStateOf(channel.userAgent) }
    var referer by remember { mutableStateOf(channel.referer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.adminEditLinksTitle}: ${channel.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text(strings.fieldPrimaryUrl) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url2, onValueChange = { url2 = it }, label = { Text(strings.fieldBackupUrl2) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url3, onValueChange = { url3 = it }, label = { Text(strings.fieldBackupUrl3) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ua, onValueChange = { ua = it }, label = { Text(strings.fieldUserAgent) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = referer, onValueChange = { referer = it }, label = { Text(strings.fieldReferer) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onSave(url, url2, url3, ua, referer) }) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}

@Composable
private fun AdminLogoQuickEditorDialog(
    channel: ChannelEntity,
    onDismiss: () -> Unit,
    onSave: (logo: String, backdrop: String) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val strings = LocalAppStrings.current
    var logo by remember { mutableStateOf(channel.logo) }
    var backdrop by remember { mutableStateOf(channel.backdrop) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.adminEditLogoTitle}: ${channel.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0A1022)),
                    contentAlignment = Alignment.Center,
                ) {
                    ChannelLogoArtwork(
                        logoUrl = logo,
                        channelName = channel.name,
                        modifier = Modifier.size(64.dp),
                    )
                }
                OutlinedTextField(value = logo, onValueChange = { logo = it }, label = { Text(strings.fieldLogoUrl) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = backdrop, onValueChange = { backdrop = it }, label = { Text(strings.fieldBackdropUrl) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onSave(logo, backdrop) }) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
