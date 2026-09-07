package app.focus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> ObserveAsEvents(flow: Flow<T>, onEvent: (T) -> Unit) {
    LaunchedEffect(flow) {
        flow.collect(onEvent)
    }
}

object UiConstants {
    const val APP_NAME = "Focus"
    const val PACKAGE_DEBUG_NAME = "app.focus.android"
}
