@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package com.cineverse.core.player.internal

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.cineverse.core.player.model.AudioTrackOption
import com.cineverse.core.player.model.SubtitleTrackOption
import com.cineverse.core.player.model.VideoQuality

/** A selectable track: the group it lives in and its index within that group. */
internal data class TrackRef(val group: TrackGroup, val index: Int)

/** Track lists derived from [Tracks], plus a lookup from option id back to the Media3 track. */
internal class TrackSnapshot(
    val videoQualities: List<VideoQuality>,
    val selectedVideoQualityId: String?,
    val audioTracks: List<AudioTrackOption>,
    val selectedAudioTrackId: String?,
    val subtitleTracks: List<SubtitleTrackOption>,
    val selectedSubtitleTrackId: String?,
    val refs: Map<String, TrackRef>,
) {
    companion object {
        val EMPTY = TrackSnapshot(emptyList(), null, emptyList(), null, emptyList(), null, emptyMap())
    }
}

internal object TrackMapper {

    fun map(tracks: Tracks, parameters: TrackSelectionParameters): TrackSnapshot {
        val refs = mutableMapOf<String, TrackRef>()
        val videoByHeight = mutableMapOf<Int, VideoQuality>()
        val audio = mutableListOf<AudioTrackOption>()
        val text = mutableListOf<SubtitleTrackOption>()
        var selectedAudio: String? = null
        var selectedText: String? = null

        tracks.groups.forEachIndexed { groupIndex, group ->
            for (i in 0 until group.length) {
                if (!group.isTrackSupported(i)) continue
                val format = group.getTrackFormat(i)
                val id = "${group.type}:$groupIndex:$i:${format.id.orEmpty()}"
                when (group.type) {
                    C.TRACK_TYPE_VIDEO -> {
                        if (format.height <= 0) continue
                        refs[id] = TrackRef(group.mediaTrackGroup, i)
                        // One entry per resolution: keep the highest bitrate at each height.
                        val existing = videoByHeight[format.height]
                        if (existing == null || format.bitrate > existing.bitrate) {
                            videoByHeight[format.height] = VideoQuality(id, format.width, format.height, format.bitrate)
                        }
                    }

                    C.TRACK_TYPE_AUDIO -> {
                        refs[id] = TrackRef(group.mediaTrackGroup, i)
                        audio += AudioTrackOption(id, format.language.normalized(), format.label, format.channelCount)
                        if (group.isTrackSelected(i)) selectedAudio = id
                    }

                    C.TRACK_TYPE_TEXT -> {
                        refs[id] = TrackRef(group.mediaTrackGroup, i)
                        text += SubtitleTrackOption(
                            id = id,
                            language = format.language.normalized(),
                            label = format.label,
                            isForced = format.selectionFlags and C.SELECTION_FLAG_FORCED != 0,
                            isClosedCaption = format.isClosedCaption(),
                        )
                        if (group.isTrackSelected(i)) selectedText = id
                    }
                }
            }
        }

        return TrackSnapshot(
            videoQualities = videoByHeight.values.sortedByDescending { it.height },
            selectedVideoQualityId = selectedVideoOverride(parameters, refs, videoByHeight.values),
            audioTracks = audio,
            selectedAudioTrackId = selectedAudio,
            subtitleTracks = text,
            selectedSubtitleTrackId = selectedText.takeUnless {
                C.TRACK_TYPE_TEXT in parameters.disabledTrackTypes
            },
            refs = refs,
        )
    }

    /** Builds new parameters with [ref] forced for its track type, or the override cleared when null. */
    fun override(parameters: TrackSelectionParameters, trackType: Int, ref: TrackRef?): TrackSelectionParameters {
        val builder = parameters.buildUpon().setTrackTypeDisabled(trackType, false)
        return if (ref == null) {
            builder.clearOverridesOfType(trackType).build()
        } else {
            builder.setOverrideForType(TrackSelectionOverride(ref.group, ref.index)).build()
        }
    }

    private fun selectedVideoOverride(
        parameters: TrackSelectionParameters,
        refs: Map<String, TrackRef>,
        qualities: Collection<VideoQuality>,
    ): String? {
        val override = parameters.overrides.values.firstOrNull { it.type == C.TRACK_TYPE_VIDEO } ?: return null
        val index = override.trackIndices.singleOrNull() ?: return null
        val selectedId = refs.entries.firstOrNull { (_, ref) ->
            ref.group == override.mediaTrackGroup && ref.index == index
        }?.key
        // Map to the representative entry for that height, which may be a different bitrate.
        val height = override.mediaTrackGroup.getFormat(index).height
        return qualities.firstOrNull { it.height == height }?.id ?: selectedId
    }

    private fun Format.isClosedCaption(): Boolean =
        sampleMimeType == androidx.media3.common.MimeTypes.APPLICATION_CEA608 ||
            sampleMimeType == androidx.media3.common.MimeTypes.APPLICATION_CEA708

    private fun String?.normalized(): String? = this?.takeUnless { it == C.LANGUAGE_UNDETERMINED || it.isBlank() }
}
