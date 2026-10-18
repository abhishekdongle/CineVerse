package com.cineverse.core.domain.util

import kotlin.coroutines.cancellation.CancellationException

/** Like [runCatching], but rethrows [CancellationException] so coroutine cancellation still works. */
suspend inline fun <T> suspendRunCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
