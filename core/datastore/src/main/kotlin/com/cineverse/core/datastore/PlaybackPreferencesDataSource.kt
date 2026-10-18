package com.cineverse.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlaybackPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val playbackPreferences: Flow<PlaybackPreferences> = dataStore.data.map { prefs ->
        PlaybackPreferences(
            autoplayNextEpisode = prefs[Keys.AUTOPLAY_NEXT] ?: true,
            dataSaver = prefs[Keys.DATA_SAVER] ?: false,
        )
    }

    suspend fun setAutoplayNextEpisode(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTOPLAY_NEXT] = enabled }
    }

    suspend fun setDataSaver(enabled: Boolean) {
        dataStore.edit { it[Keys.DATA_SAVER] = enabled }
    }

    private object Keys {
        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next_episode")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
    }
}
