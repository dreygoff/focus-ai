package app.focus.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dispatcher qualifiers for dependency injection in Hilt modules.
 */
annotation class IoDispatcher
annotation class DefaultDispatcher
annotation class MainDispatcher

@IoDispatcher val IoDispatcher: CoroutineDispatcher = Dispatchers.IO
@DefaultDispatcher val DefaultDispatcher: CoroutineDispatcher = Dispatchers.Default
@MainDispatcher val MainDispatcher: CoroutineDispatcher = Dispatchers.Main
