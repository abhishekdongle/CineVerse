package com.cineverse.core.common.dispatchers

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: CineVerseDispatchers)

enum class CineVerseDispatchers {
    Default,
    IO,
}

/** A [kotlinx.coroutines.CoroutineScope] that lives as long as the application process. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
