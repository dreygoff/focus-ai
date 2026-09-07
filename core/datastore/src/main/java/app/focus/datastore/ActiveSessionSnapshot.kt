package app.focus.datastore

/** Manages session snapshot persistence for recovery across device restarts. */
interface ActiveSessionSnapshotStore {
    suspend fun save(snapshot: app.focus.domain.internal.statemachine.SessionSnapshot)
    suspend fun load(): app.focus.domain.internal.statemachine.SessionSnapshot?
    suspend fun clear()
}
