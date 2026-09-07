package app.focus.domain.model

import java.util.UUID

sealed interface LockMode {
    data object Soft : LockMode
    data object Hard : LockMode
}
