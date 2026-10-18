package com.cineverse.core.data.repository

import com.cineverse.core.datastore.PlaybackPreferencesDataSource
import com.cineverse.core.domain.model.PlaybackSettings
import com.cineverse.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DefaultSettingsRepository @Inject constructor(
    private val dataSource: PlaybackPreferencesDataSource,
) : SettingsRepository {

    override val playbackSettings: Flow<PlaybackSettings> =
        dataSource.playbackPreferences.map {
            PlaybackSettings(autoplayNextEpisode = it.autoplayNextEpisode, dataSaver = it.dataSaver)
        }

    override suspend fun setAutoplayNextEpisode(enabled: Boolean) =
        dataSource.setAutoplayNextEpisode(enabled)

    override suspend fun setDataSaver(enabled: Boolean) = dataSource.setDataSaver(enabled)
}
