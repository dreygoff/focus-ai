package app.focus.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import app.focus.android.datastore.ActiveSessionSnapshot as ActiveSessionSnapshotProto
import app.focus.domain.internal.statemachine.SessionSnapshot
import app.focus.domain.usecase.ActiveSessionSnapshotStorage
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream

/**
 * Device-protected Proto DataStore for active session snapshot (§9.2, TR-05).
 */
class ProtoActiveSessionSnapshotStorage(
    private val dataStore: DataStore<ActiveSessionSnapshotProto>,
) : ActiveSessionSnapshotStorage {

    override suspend fun save(snapshot: SessionSnapshot) {
        dataStore.updateData {
            ActiveSessionSnapshotProto.newBuilder()
                .setSessionId(snapshot.sessionId)
                .setLockMode(snapshot.lockMode)
                .setPlannedEndAt(snapshot.plannedEndAtMillis)
                .addAllTargetPackages(snapshot.targetPackages)
                .addAllHardLockExtraPackages(snapshot.hardLockExtraPackages)
                .setDefaultLauncher(snapshot.defaultLauncherPkg.orEmpty())
                .setPomodoro(snapshot.isPomodoro)
                .setPhaseEndAt(snapshot.phaseEndAtMillis)
                .setPhase(snapshot.currentPhase)
                .build()
        }
    }

    override suspend fun load(): SessionSnapshot? {
        val proto = dataStore.data.first()
        if (proto.sessionId.isBlank()) return null
        return SessionSnapshot(
            sessionId = proto.sessionId,
            lockMode = proto.lockMode.ifBlank { "SOFT" },
            plannedEndAtMillis = proto.plannedEndAt,
            targetPackages = proto.targetPackagesList,
            hardLockExtraPackages = proto.hardLockExtraPackagesList,
            defaultLauncherPkg = proto.defaultLauncher.takeIf { it.isNotBlank() },
            isPomodoro = proto.pomodoro,
            currentPhase = proto.phase.ifBlank { "FOCUS" },
            phaseEndAtMillis = proto.phaseEndAt,
        )
    }

    override suspend fun clear() {
        dataStore.updateData { ActiveSessionSnapshotProto.getDefaultInstance() }
    }

    companion object {
        fun create(context: Context): ProtoActiveSessionSnapshotStorage {
            val deviceContext = context.createDeviceProtectedStorageContext()
            val dataStore = DataStoreFactory.create(
                serializer = ActiveSessionSnapshotSerializer,
                produceFile = {
                    deviceContext.filesDir.resolve("datastore/active_session.pb")
                },
            )
            return ProtoActiveSessionSnapshotStorage(dataStore)
        }
    }
}

private object ActiveSessionSnapshotSerializer : Serializer<ActiveSessionSnapshotProto> {
    override val defaultValue: ActiveSessionSnapshotProto =
        ActiveSessionSnapshotProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): ActiveSessionSnapshotProto =
        ActiveSessionSnapshotProto.parseFrom(input)

    override suspend fun writeTo(t: ActiveSessionSnapshotProto, output: OutputStream) {
        t.writeTo(output)
    }
}
