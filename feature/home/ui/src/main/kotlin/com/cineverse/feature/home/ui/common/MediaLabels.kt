package com.cineverse.feature.home.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cineverse.core.domain.model.AccessModel
import com.cineverse.core.domain.model.ContentType
import com.cineverse.core.domain.model.MediaItem
import com.cineverse.feature.home.ui.R

/** Badge text shared by the mobile and TV cards, or null when the item needs no badge. */
@Composable
internal fun MediaItem.badgeLabel(): String? = when {
    contentType == ContentType.LIVE -> stringResource(R.string.home_badge_live)
    access == AccessModel.AD_SUPPORTED -> stringResource(R.string.home_badge_free)
    else -> null
}

@Composable
internal fun MediaItem.playLabel(): String =
    if (contentType == ContentType.LIVE) {
        stringResource(R.string.home_watch_live)
    } else {
        stringResource(R.string.home_play)
    }
