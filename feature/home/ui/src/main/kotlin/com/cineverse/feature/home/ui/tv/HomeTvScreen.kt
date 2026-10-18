package com.cineverse.feature.home.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.cineverse.core.domain.model.MediaItem
import com.cineverse.core.domain.model.Rail
import com.cineverse.feature.home.presentation.HomeUiState
import com.cineverse.feature.home.presentation.HomeViewModel
import com.cineverse.feature.home.ui.R
import com.cineverse.feature.home.ui.common.PosterImage
import com.cineverse.feature.home.ui.common.badgeLabel

/**
 * TV home: 10-foot layout driven by D-pad focus. The hero shows whichever card has focus,
 * and the first card takes focus on launch so the remote works immediately.
 */
@Composable
fun HomeTvScreen(
    onPlay: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeTvContent(
        uiState = uiState,
        onItemClick = { item ->
            viewModel.onItemSelected(item)
            onPlay(item)
        },
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
internal fun HomeTvContent(
    uiState: HomeUiState,
    onItemClick: (MediaItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        HomeUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
            Text("…", style = MaterialTheme.typography.displaySmall)
        }

        is HomeUiState.Error -> {
            val retryFocus = remember { FocusRequester() }
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.home_error), style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .focusRequester(retryFocus),
                ) {
                    Text(stringResource(R.string.home_retry))
                }
            }
            LaunchedEffect(Unit) { retryFocus.requestFocus() }
        }

        is HomeUiState.Success -> {
            // Focused item is TV-only UI state, so it lives here rather than in the shared ViewModel.
            var focusedItem by remember(uiState) { mutableStateOf(uiState.featured) }
            val firstCardFocus = remember { FocusRequester() }

            Column(modifier.fillMaxSize()) {
                focusedItem?.let { TvHero(it) }
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    itemsIndexed(uiState.rails, key = { _, rail -> rail.id }) { railIndex, rail ->
                        TvRail(
                            rail = rail,
                            firstCardModifier = if (railIndex == 0) {
                                Modifier.focusRequester(firstCardFocus)
                            } else {
                                Modifier
                            },
                            onItemFocused = { focusedItem = it },
                            onItemClick = onItemClick,
                        )
                    }
                }
            }
            LaunchedEffect(uiState) { firstCardFocus.requestFocus() }
        }
    }
}

@Composable
private fun TvHero(item: MediaItem) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        PosterImage(item.backdropUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.9f),
                        0.6f to Color.Transparent,
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
                .width(480.dp),
        ) {
            Text(item.title, style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text(
                item.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 3,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun TvRail(
    rail: Rail,
    firstCardModifier: Modifier,
    onItemFocused: (MediaItem) -> Unit,
    onItemClick: (MediaItem) -> Unit,
) {
    Column {
        Text(
            rail.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 48.dp, bottom = 12.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(rail.items, key = MediaItem::id) { item ->
                Card(
                    onClick = { onItemClick(item) },
                    scale = CardDefaults.scale(focusedScale = 1.1f),
                    modifier = (if (item == rail.items.first()) firstCardModifier else Modifier)
                        .size(width = 132.dp, height = 198.dp)
                        .onFocusChanged { if (it.isFocused) onItemFocused(item) },
                ) {
                    Box {
                        PosterImage(item.posterUrl, item.title, Modifier.fillMaxSize())
                        item.badgeLabel()?.let { badge ->
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
