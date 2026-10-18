package com.cineverse.core.player

import com.cineverse.core.player.model.AudioTrackOption
import com.cineverse.core.player.model.SubtitleTrackOption
import java.util.Locale

/**
 * Human-readable track names, shared by every player UI. Tracks are labeled by language name in
 * the user's locale ("English", "Deutsch" → "German"), with the manifest label added when two
 * tracks share a language.
 */
object TrackLabels {

    fun audioNames(tracks: List<AudioTrackOption>, locale: Locale = Locale.getDefault()): Map<String, String> =
        disambiguate(
            names = tracks.map { it.id to (languageName(it.language, locale) ?: it.label) },
            details = listOf(
                { id -> channelLabel(tracks.first { it.id == id }.channelCount) },
                { id -> tracks.first { it.id == id }.label },
            ),
        )

    fun subtitleNames(tracks: List<SubtitleTrackOption>, locale: Locale = Locale.getDefault()): Map<String, String> =
        disambiguate(
            names = tracks.map { track ->
                val base = languageName(track.language, locale) ?: track.label
                track.id to when {
                    base == null -> null
                    track.isClosedCaption -> "$base CC"
                    track.isForced -> "$base (forced)"
                    else -> base
                }
            },
            details = listOf { id -> tracks.first { it.id == id }.label },
        )

    fun languageName(language: String?, locale: Locale = Locale.getDefault()): String? {
        if (language.isNullOrBlank()) return null
        val name = Locale.forLanguageTag(language).getDisplayName(locale)
        if (name.isBlank() || name.equals(language, ignoreCase = true)) return null
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    private fun channelLabel(channels: Int): String? = when {
        channels <= 0 -> null
        channels == 1 -> "Mono"
        channels == 2 -> "Stereo"
        channels == 6 -> "5.1"
        channels == 8 -> "7.1"
        else -> "$channels ch"
    }

    /**
     * Makes names unique: for names that occur more than once, tries each [details] function in
     * order and uses the first that tells the tracks apart ("English (Stereo)", "English (5.1)").
     * Falls back to numbering, and numbers tracks without any name.
     */
    private fun disambiguate(
        names: List<Pair<String, String?>>,
        details: List<(id: String) -> String?>,
    ): Map<String, String> {
        val result = names.mapIndexed { index, (id, name) -> id to (name ?: "Track ${index + 1}") }.toMap().toMutableMap()
        result.values.groupBy { it }.filterValues { it.size > 1 }.keys.forEach { duplicate ->
            val ids = result.filterValues { it == duplicate }.keys.toList()
            val detail = details.firstNotNullOfOrNull { describe ->
                ids.associateWith(describe).takeIf { labels -> labels.values.none { it == null } && labels.values.toSet().size == ids.size }
            }
            ids.forEachIndexed { index, id ->
                result[id] = detail?.get(id)?.let { "$duplicate ($it)" } ?: "$duplicate ${index + 1}"
            }
        }
        return result
    }
}
