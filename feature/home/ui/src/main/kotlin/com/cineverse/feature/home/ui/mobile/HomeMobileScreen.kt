package com.cineverse.feature.home.ui.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cineverse.core.domain.model.MediaItem
import com.cineverse.core.domain.model.Rail
import com.cineverse.feature.home.presentation.HomeUiState
import com.cineverse.feature.home.presentation.HomeViewModel
import com.cineverse.feature.home.ui.R
import com.cineverse.feature.home.ui.common.PosterImage
import com.cineverse.feature.home.ui.common.badgeLabel
import com.cineverse.feature.home.ui.common.playLabel

/** Phone/tablet home: touch-first, vertically scrolling rails with a hero at the top. */
@Composable
fun HomeMobileScreen(
    onPlay: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeMobileContent(
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
internal fun HomeMobileContent(
    uiState: HomeUiState,
    onItemClick: (MediaItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        HomeUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }

        is HomeUiState.Error -> Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.home_error))
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.home_retry))
            }
        }

        is HomeUiState.Success -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            uiState.featured?.let { featured ->
                item(key = "hero") { MobileHero(featured, onPlay = { onItemClick(featured) }) }
            }
            items(uiState.rails, key = Rail::id) { rail ->
                MobileRail(rail, onItemClick)
            }
        }
    }
}

@Composable
private fun MobileHero(item: MediaItem, onPlay: () -> Unit) {
    Column {
        PosterImage(
            url = item.backdropUrl,
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(item.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                item.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(onClick = onPlay) { Text(item.playLabel()) }
        }
    }
}

@Composable
private fun MobileRail(rail: Rail, onItemClick: (MediaItem) -> Unit) {
    Column {
        Text(
            rail.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(rail.items, key = MediaItem::id) { item ->
                Card(onClick = { onItemClick(item) }, modifier = Modifier.width(120.dp)) {
                    PosterImage(
                        url = item.posterUrl,
                        contentDescription = item.title,
                        modifier = Modifier.aspectRatio(2f / 3f),
                    )
                    item.badgeLabel()?.let { badge ->
                        AssistChip(
                            onClick = { onItemClick(item) },
                            label = { Text(badge, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
