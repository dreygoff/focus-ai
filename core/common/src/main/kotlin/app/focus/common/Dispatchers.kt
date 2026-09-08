package app.focus.common

import javax.inject.Qualifier

/**
 * Dispatcher qualifiers for dependency injection in Hilt modules.
 * These annotations are used to distinguish which CoroutineDispatcher
 * should be injected by Hilt.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
