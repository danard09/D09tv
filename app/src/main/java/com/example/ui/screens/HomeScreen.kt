package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChannelEntity
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppStrings
import com.example.ui.components.ChannelCard
import com.example.ui.components.D09LogoBadge
import com.example.ui.components.FeaturedHeroCard
import com.example.ui.components.TvFocusableSurface
import com.example.ui.theme.AmberGold
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SkyCyan
import com.example.viewmodel.MainUiState

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onChannelClick: (ChannelEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onToggleFavoriteCategory: (String) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    var showLanguageMenu by remember { mutableStateOf(false) }

    val channelCountsByGroup = remember(uiState.enabledChannels) {
        uiState.enabledChannels.groupingBy { it.groupName }.eachCount()
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("home_grid"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top Header Bar
        item(span = { GridItemSpan(maxLineSpan) }, key = "header_bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                D09LogoBadge(iconSize = 42.dp, showSubtitle = true, modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TvFocusableSurface(
                        onClick = onNavigateSearch,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("home_search_button"),
                    ) { _ ->
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = strings.navSearch,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Box {
                        TvFocusableSurface(
                            onClick = { showLanguageMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("home_language_button"),
                        ) { _ ->
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = strings.settingsLanguage,
                                    tint = SkyCyan,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = uiState.settings.language.shortBadge,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = lang.nativeLabel,
                                            fontWeight = if (lang == uiState.settings.language) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        onSelectLanguage(lang)
                                        showLanguageMenu = false
                                    },
                                    modifier = Modifier.testTag("lang_option_${lang.code}"),
                                )
                            }
                        }
                    }

                    TvFocusableSurface(
                        onClick = onNavigateSettings,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("home_settings_button"),
                    ) { _ ->
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = strings.navSettings,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }

        // Offline Banner
        if (!uiState.isOnline) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "offline_banner") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LiveCrimson.copy(alpha = 0.20f))
                        .border(1.dp, LiveCrimson.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiOff,
                        contentDescription = strings.offlineBanner,
                        tint = LiveCrimson,
                    )
                    Text(
                        text = strings.offlineBanner,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        // Featured Channels Carousel
        if (uiState.featuredChannels.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "featured_header") {
                SectionHeader(
                    title = strings.featuredTitle,
                    subtitle = strings.channelsCountLabel(uiState.featuredChannels.size),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "featured_row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(uiState.featuredChannels, key = { "feat_${it.id}" }) { channel ->
                        FeaturedHeroCard(
                            channel = channel,
                            isFavorite = channel.id in uiState.favoriteIds,
                            onPlayClick = { onChannelClick(channel) },
                            onToggleFavorite = { onToggleFavorite(channel.id) },
                        )
                    }
                }
            }
        }

        // Recently Watched / Continue Watching
        if (uiState.recentlyWatchedChannels.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "recent_header") {
                SectionHeader(
                    title = strings.continueWatching,
                    subtitle = strings.channelsCountLabel(uiState.recentlyWatchedChannels.size),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "recent_row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(uiState.recentlyWatchedChannels, key = { "rec_${it.id}" }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isFavorite = channel.id in uiState.favoriteIds,
                            onPlayClick = { onChannelClick(channel) },
                            onToggleFavorite = { onToggleFavorite(channel.id) },
                            compact = true,
                            modifier = Modifier.width(184.dp),
                        )
                    }
                }
            }
        }

        // Favorite Channels Row
        if (uiState.favoriteChannels.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "fav_header") {
                SectionHeader(
                    title = strings.favoriteChannels,
                    subtitle = strings.channelsCountLabel(uiState.favoriteChannels.size),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "fav_row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(uiState.favoriteChannels, key = { "fav_${it.id}" }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isFavorite = true,
                            onPlayClick = { onChannelClick(channel) },
                            onToggleFavorite = { onToggleFavorite(channel.id) },
                            compact = true,
                            modifier = Modifier.width(184.dp),
                        )
                    }
                }
            }
        }

        // Categories Chip Selector
        item(span = { GridItemSpan(maxLineSpan) }, key = "categories_row") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    title = uiState.selectedCategory?.let { strings.translateCategory(it) }
                        ?: strings.allCategories,
                    subtitle = strings.channelsCountLabel(uiState.visibleChannelsByCategory.size),
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    item(key = "cat_all") {
                        CategoryChipItem(
                            label = strings.allCategories,
                            count = uiState.enabledChannels.size,
                            selected = uiState.selectedCategory == null,
                            isFavoriteCategory = false,
                            showFavoriteToggle = false,
                            onClick = { onSelectCategory(null) },
                            onToggleFavoriteCategory = {},
                            testTag = "category_chip_all",
                        )
                    }

                    items(uiState.groups, key = { "cat_${it.name}" }) { group ->
                        val count = channelCountsByGroup[group.name] ?: 0
                        CategoryChipItem(
                            label = strings.translateCategory(group.name),
                            count = count,
                            selected = uiState.selectedCategory == group.name,
                            isFavoriteCategory = group.name in uiState.favoriteCategories,
                            showFavoriteToggle = true,
                            onClick = { onSelectCategory(group.name) },
                            onToggleFavoriteCategory = { onToggleFavoriteCategory(group.name) },
                            testTag = "category_chip_${group.name}",
                        )
                    }
                }
            }
        }

        // Channel Grid Items
        items(uiState.visibleChannelsByCategory, key = { "grid_${it.id}" }) { channel ->
            ChannelCard(
                channel = channel,
                isFavorite = channel.id in uiState.favoriteIds,
                onPlayClick = { onChannelClick(channel) },
                onToggleFavorite = { onToggleFavorite(channel.id) },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(listOf(ElectricBlue, RoyalPurple))),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryChipItem(
    label: String,
    count: Int,
    selected: Boolean,
    isFavoriteCategory: Boolean,
    showFavoriteToggle: Boolean,
    onClick: () -> Unit,
    onToggleFavoriteCategory: () -> Unit,
    testTag: String,
) {
    val strings = LocalAppStrings.current
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    }
    val focusedContainer = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    TvFocusableSurface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        containerColor = container,
        focusedContainerColor = focusedContainer,
        modifier = Modifier.testTag(testTag),
    ) { _ ->
        Row(
            modifier = Modifier.padding(
                start = 14.dp,
                end = if (showFavoriteToggle) 4.dp else 14.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) Color.White.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White else SkyCyan,
                )
            }
            if (showFavoriteToggle) {
                IconButton(
                    onClick = onToggleFavoriteCategory,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("${testTag}_fav"),
                ) {
                    Icon(
                        imageVector = if (isFavoriteCategory) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = strings.favoriteCategoryToggle,
                        tint = if (isFavoriteCategory) AmberGold
                        else if (selected) Color.White.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
