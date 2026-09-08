package app.focus.domain.internal.blockdecision

import app.focus.domain.model.BlockDecision
import app.focus.domain.model.BlockReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockDecisionEngineTest {

    private val now = System.currentTimeMillis()

    private fun engine(
        systemAllowlist: Set<String> = emptySet(),
        userAllowlist: Set<String> = emptySet(),
        accessWindowPkgLookup: Map<String, Long> = emptyMap(),
        targetPackages: Set<String> = defaultTargets,
        hasActiveSession: Boolean = true,
        isHardLock: Boolean = false,
        hardLockExtraPackages: Set<String> = emptySet(),
        inPomodoroBreak: Boolean = false,
    ): BlockDecisionEngine = BlockDecisionEngine(
        systemAllowlist = systemAllowlist,
        userAllowlistProvider = { userAllowlist },
        accessWindowLookup = { accessWindowPkgLookup },
        sessionState = BlockDecisionEngine.SessionCheckState(
            hasActiveSession = hasActiveSession,
            isHardLock = isHardLock,
            sessionId = "test-session",
            targetPackages = targetPackages,
            hardLockExtraPackages = hardLockExtraPackages,
            inPomodoroBreak = inPomodoroBreak,
        ),
    )

    @Test
    fun `no active session means allow for any package`() {
        val eng = engine(hasActiveSession = false)
        assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android"))
    }

    @Test
    fun `no active session allows system packages`() {
        val eng = engine(hasActiveSession = false)
        assertEquals(BlockDecision.Allow, eng.decide("com.android.systemui"))
    }

    @Test
    fun `package in system allowlist is allowed`() {
        val eng = engine(systemAllowlist = setOf("com.android.systemui"))
        assertEquals(BlockDecision.Allow, eng.decide("com.android.systemui"))
    }

    @Test
    fun `target package gets blocked`() {
        val eng = engine(systemAllowlist = setOf("com.android.systemui"))
        val result = eng.decide("com.instagram.android")
        assertTrue(result is BlockDecision.Block)
    }

    @Test
    fun `system allowlist works with multiple packages`() {
        val sys = setOf("com.android.systemui", "app.focus.android")
        val eng = engine(systemAllowlist = sys)
        assertEquals(BlockDecision.Allow, eng.decide("com.android.systemui"))
        assertEquals(BlockDecision.Allow, eng.decide("app.focus.android"))
    }

    @Test
    fun `package in user allowlist is allowed`() {
        val eng = engine(userAllowlist = setOf("com.instagram.android"))
        assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android"))
    }

    @Test
    fun `user allowlist overrides block decision`() {
        val eng = engine(userAllowlist = setOf("com.whatsapp"))
        assertEquals(BlockDecision.Allow, eng.decide("com.whatsapp"))
    }

    @Test
    fun `package not in any allowlist gets blocked when targeted`() {
        val eng = engine(
            systemAllowlist = setOf("com.android.systemui"),
            userAllowlist = setOf("com.whatsapp"),
            targetPackages = setOf("com.facebook.katana"),
        )
        assertTrue(eng.decide("com.facebook.katana") is BlockDecision.Block)
    }

    @Test
    fun `non-target package is allowed`() {
        val eng = engine(
            systemAllowlist = setOf("app.focus.android"),
            userAllowlist = setOf("com.whatsapp"),
            targetPackages = setOf("com.instagram.android"),
        )
        assertEquals(BlockDecision.Allow, eng.decide("com.facebook.katana"))
    }

    @Test
    fun `active access window allows package`() {
        val windowEnd = now + 300_000L
        val eng = engine(accessWindowPkgLookup = mapOf("com.instagram.android" to windowEnd))
        assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android"))
    }

    @Test
    fun `expired access window does not allow package`() {
        val expiredWindow = now - 600_000L
        val eng = engine(accessWindowPkgLookup = mapOf("com.instagram.android" to expiredWindow))
        assertTrue(eng.decide("com.instagram.android") is BlockDecision.Block)
    }

    @Test
    fun `access window at exactly end time still allows`() {
        val windowEnd = System.currentTimeMillis() + 1000L
        val eng = engine(accessWindowPkgLookup = mapOf("com.instagram.android" to windowEnd))
        assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android"))
    }

    @Test
    fun `multiple access windows checked independently`() {
        val windowEnd = now + 300_000L
        val eng = engine(
            accessWindowPkgLookup = mapOf(
                "com.whatsapp" to windowEnd,
                "com.telegram.messenger" to (now - 1000L),
            ),
            targetPackages = setOf("com.whatsapp", "com.telegram.messenger"),
        )
        assertEquals(BlockDecision.Allow, eng.decide("com.whatsapp"))
        assertTrue(eng.decide("com.telegram.messenger") is BlockDecision.Block)
    }

    @Test
    fun `blocked package returns block with correct reason`() {
        val eng = engine()
        val result = eng.decide("com.instagram.android")
        assertTrue(result is BlockDecision.Block)
        assertEquals(BlockReason.TARGET_APP, (result as BlockDecision.Block).reason)
    }

    @Test
    fun `hard lock extra package is blocked with hard lock reason`() {
        val eng = engine(
            isHardLock = true,
            targetPackages = emptySet(),
            hardLockExtraPackages = setOf("com.android.settings"),
        )
        val result = eng.decide("com.android.settings") as BlockDecision.Block
        assertEquals(BlockReason.HARD_LOCK_EXTRA, result.reason)
    }

    @Test
    fun `pomodoro break allows all packages`() {
        val eng = engine(inPomodoroBreak = true)
        assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android"))
    }

    companion object {
        private val defaultTargets = setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.banned.app",
            "com.app.one",
            "com.app.two",
            "com.other.app",
            "com.test.app",
            "unknown.package.app",
            "com.vpn.app",
        )
    }
}
