package com.cineverse.core.notifications

/** Provider-agnostic push payload, so a second provider (e.g. Amazon ADM on Fire TV) can reuse it. */
data class PushMessage(
    val title: String,
    val body: String,
    val channelId: String,
    /** Optional deep link, e.g. cineverse://watch/{id}. */
    val deepLink: String? = null,
)
