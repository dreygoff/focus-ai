package app.focus.domain.usecase

interface MandatoryPermissionsGateway {
    fun areMandatoryGranted(): Boolean
}

interface LastUsedProfileProvider {
    suspend fun profileId(): String?
    suspend fun durationMinutes(): Int
    suspend fun save(profileId: String, durationMinutes: Int)
}
