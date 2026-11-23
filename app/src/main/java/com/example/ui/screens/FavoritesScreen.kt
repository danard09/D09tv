package com.example.ui.screens

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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ChannelEntity
import com.example.i18n.LocalAppStrings
import com.example.ui.components.ChannelCard
import com.example.ui.components.TvFocusableSurface
import com.example.ui.theme.AmberGold
import com.example.viewmodel.MainUiState

@Composable
fun FavoritesScreen(
    uiState: MainUiState,
    onChannelClick: (ChannelEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleFavoriteCategory: (String) -> Unit,
    onOpenCategoryOnHome: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val favCategoriesList = uiState.favoriteCategories.toList().sorted()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_grid"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "fav_screen_title") {
            Text(
                text = strings.navFavorites,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Favorite Categories
        if (favCategoriesList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "fav_cats_header") {
                SectionHeader(title = strings.favoriteCategories)
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "fav_cats_row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(favCategoriesList, key = { "fcat_$it" }) { cat ->
                        TvFocusableSurface(
                            onClick = { onOpenCategoryOnHome(cat) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("fav_category_chip_$cat"),
                        ) { _ ->
                            Row(
                                modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = strings.translateCategory(cat),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                IconButton(
                                    onClick = { onToggleFavoriteCategory(cat) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("fav_category_remove_$cat"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = strings.favoriteCategoryToggle,
                                        tint = AmberGold,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Favorite Channels
        item(span = { GridItemSpan(maxLineSpan) }, key = "fav_channels_header") {
            SectionHeader(
                title = strings.favoriteChannels,
                subtitle = strings.channelsCountLabel(uiState.favoriteChannels.size),
            )
        }

        if (uiState.favoriteChannels.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "fav_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = strings.noFavoritesTitle,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = strings.noFavoritesTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = strings.noFavoritesSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            items(uiState.favoriteChannels, key = { "fav_ch_${it.id}" }) { channel ->
                ChannelCard(
                    channel = channel,
                    isFavorite = true,
                    onPlayClick = { onChannelClick(channel) },
                    onToggleFavorite = { onToggleFavorite(channel.id) },
                )
            }
        }

        // Recently Watched History
        item(span = { GridItemSpan(maxLineSpan) }, key = "history_header") {
            SectionHeader(
                title = strings.recentlyWatched,
                subtitle = strings.channelsCountLabel(uiState.recentlyWatchedChannels.size),
            )
        }

        if (uiState.recentlyWatchedChannels.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "history_empty") {
                Text(
                    text = strings.noHistoryYet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }, key = "history_row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.recentlyWatchedChannels, key = { "hist_${it.id}" }) { channel ->
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

        item(span = { GridItemSpan(maxLineSpan) }, key = "fav_bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
