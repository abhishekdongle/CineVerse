package com.cineverse.core.common.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Maps each value and drops consecutive duplicates of the result. */
fun <T, R> Flow<T>.mapDistinct(transform: (T) -> R): Flow<R> =
    map(transform).distinctUntilChanged()
