package com.cineverse.core.analytics

data class AnalyticsEvent(
    val name: String,
    val params: Map<String, String> = emptyMap(),
) {
    companion object {
        fun screenView(screenName: String) =
            AnalyticsEvent(Types.SCREEN_VIEW, mapOf(Params.SCREEN_NAME to screenName))

        fun contentSelected(contentId: String, source: String) =
            AnalyticsEvent(Types.CONTENT_SELECTED, mapOf(Params.CONTENT_ID to contentId, Params.SOURCE to source))
    }

    object Types {
        const val SCREEN_VIEW = "screen_view"
        const val CONTENT_SELECTED = "content_selected"
        const val PLAYBACK_STARTED = "playback_started"
        const val PLAYBACK_ERROR = "playback_error"
    }

    object Params {
        const val SCREEN_NAME = "screen_name"
        const val CONTENT_ID = "content_id"
        const val SOURCE = "source"
        const val IS_LIVE = "is_live"
        const val ERROR_CODE = "error_code"
    }
}
