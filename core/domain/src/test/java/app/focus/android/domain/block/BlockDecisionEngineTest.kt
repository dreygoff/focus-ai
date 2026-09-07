package app.focus.android.domain.block

import app.focus.domain.model.AccessWindow
import app.focus.domain.model.BlockDecision
import app.focus.domain.model.BlockReason
import app.focus.domain.model.LockMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tabular tests for BlockDecisionEngine (TR-06) with ≥ 30 test cases.
 * Tests priority-based decision rules as specified in TR-06.
 */
class BlockDecisionEngineTest {

    private lateinit var engine: BlockDecisionEngine

    @Before
    fun setUp() {
        engine = BlockDecisionEngine()
    }

    // ─── RULE 1: No running session ──────────────────────────────

    @Test
    fun `should allow when no session is active`() {
        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = false,
            lockMode = null,
            targetPackages = emptyList(),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `shouldBlock returns false when no session`() {
        assertFalse(engine.shouldBlock(
            foregroundPackage = "com.instagram.android",
            sessionActive = false,
            lockMode = null,
            targetPackages = emptyList(),
        ))
    }

    // ─── RULE 2: Allowlist ───────────────────────────────────────

    @Test
    fun `should allow package in system allowlist`() {
        val decision = engine.decide(
            foregroundPackage = "com.android.systemui",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.instagram.android"),
            systemAllowlist = BlockDecisionEngine.DEFAULT_SYSTEM_ALLOWLIST,
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `should allow package in user allowlist`() {
        val decision = engine.decide(
            foregroundPackage = "com.whatsapp",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.whatsapp"),
            userAllowlist = setOf("com.whatsapp"),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `should block package not in any allowlist`() {
        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
            systemAllowlist = BlockDecisionEngine.DEFAULT_SYSTEM_ALLOWLIST,
            userAllowlist = emptySet(),
        )

        assertTrue(decision is BlockDecision.Block)
    }

    // ─── RULE 3: Phone call + dialer ─────────────────────────────

    @Test
    fun `should allow dialer during active call`() {
        val decision = engine.decide(
            foregroundPackage = "com.android.dialer",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.android.dialer"), // even if in targets
            isDialer = true,
            isInCall = true,
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `should block dialer when no call active`() {
        val decision = engine.decide(
            foregroundPackage = "com.android.dialer",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.android.dialer"),
            isDialer = true,
            isInCall = false, // no call
        )

        assertTrue(decision is BlockDecision.Block)
    }

    @Test
    fun `should block non-dialer during active call`() {
        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.instagram.android"),
            isDialer = false,
            isInCall = true,
        )

        assertTrue(decision is BlockDecision.Block)
    }

    // ─── RULE 4: Active access window ─────────────────────────────

    @Test
    fun `should allow package with active access window`() {
        val now = System.currentTimeMillis()
        val activeWindow = AccessWindow(
            id = "window-1",
            sessionId = "session-1",
            packageName = "com.instagram.android",
            grantedAt = now - 60_000L,
            expiresAt = now + 300_000L, // 5 min from now
            reason = "bypass",
            restrictedToActivity = null,
        )

        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
            activeAccessWindows = listOf(activeWindow),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `should block package with expired access window`() {
        val now = System.currentTimeMillis()
        val expiredWindow = AccessWindow(
            id = "window-1",
            sessionId = "session-1",
            packageName = "com.instagram.android",
            grantedAt = now - 600_000L,
            expiresAt = now - 60_000L, // expired 1 min ago
            reason = "bypass",
            restrictedToActivity = null,
        )

        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
            activeAccessWindows = listOf(expiredWindow),
        )

        assertTrue(decision is BlockDecision.Block)
    }

    @Test
    fun `should allow package with matching restrictedToActivity`() {
        val now = System.currentTimeMillis()
        val activeWindow = AccessWindow(
            id = "window-1",
            sessionId = "session-1",
            packageName = "com.android.settings",
            grantedAt = now - 60_000L,
            expiresAt = now + 300_000L,
            reason = "wifi settings",
            restrictedToActivity = "SettingsWifiActivity", // specific activity allowed
        )

        val decision = engine.decide(
            foregroundPackage = "com.android.settings",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.android.settings"),
            activeAccessWindows = listOf(activeWindow),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    // ─── RULE 5: Target packages ─────────────────────────────────

    @Test
    fun `should block package in target list`() {
        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android", "com.twitter.android"),
        )

        assertTrue(decision is BlockDecision.Block)
        assertEquals(BlockReason.TargetApp, (decision as BlockDecision.Block).reason)
    }

    @Test
    fun `should block hard lock extra packages`() {
        val decision = engine.decide(
            foregroundPackage = "com.android.settings",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.instagram.android"),
            hardLockExtras = setOf("com.android.settings"),
        )

        assertTrue(decision is BlockDecision.Block)
        assertEquals(BlockReason.HardLockExtra, (decision as BlockDecision.Block).reason)
    }

    @Test
    fun `should allow package not in any targets`() {
        val decision = engine.decide(
            foregroundPackage = "com.spotify.music",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    // ─── PRIORITY ORDER TESTS ────────────────────────────────────

    @Test
    fun `allowlist should take priority over targets (rule 2 > rule 5)`() {
        val decision = engine.decide(
            foregroundPackage = "com.whatsapp", // in both allowlist and targets
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.whatsapp"),
            userAllowlist = setOf("com.whatsapp"),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `phone call should take priority over targets (rule 3 > rule 5)`() {
        val decision = engine.decide(
            foregroundPackage = "com.android.dialer",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.android.dialer"), // in targets
            isDialer = true,
            isInCall = true,
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `access window should take priority over targets (rule 4 > rule 5)`() {
        val now = System.currentTimeMillis()
        val activeWindow = AccessWindow(
            id = "window-1",
            sessionId = "session-1",
            packageName = "com.instagram.android",
            grantedAt = now - 60_000L,
            expiresAt = now + 300_000L,
            reason = null,
            restrictedToActivity = null,
        )

        val decision = engine.decide(
            foregroundPackage = "com.instagram.android", // in targets
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
            activeAccessWindows = listOf(activeWindow),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `system allowlist should take priority over hard lock extras (rule 2 > rule 5)`() {
        val decision = engine.decide(
            foregroundPackage = "com.android.systemui",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.android.systemui"),
            hardLockExtras = setOf("com.android.systemui"),
            systemAllowlist = BlockDecisionEngine.DEFAULT_SYSTEM_ALLOWLIST,
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    // ─── EDGE CASES ──────────────────────────────────────────────

    @Test
    fun `should allow empty package name`() {
        val decision = engine.decide(
            foregroundPackage = "",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `should handle null hardLockExtras`() {
        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
            hardLockExtras = emptySet(),
        )

        assertTrue(decision is BlockDecision.Block)
    }

    @Test
    fun `should handle empty allowlists`() {
        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
            systemAllowlist = emptySet(),
            userAllowlist = emptySet(),
        )

        assertTrue(decision is BlockDecision.Block)
    }

    @Test
    fun `should handle multiple active access windows`() {
        val now = System.currentTimeMillis()
        val activeWindow1 = AccessWindow(
            id = "window-1",
            sessionId = "session-1",
            packageName = "com.instagram.android",
            grantedAt = now - 60_000L,
            expiresAt = now + 300_000L,
            reason = null,
            restrictedToActivity = null,
        )
        val expiredWindow2 = AccessWindow(
            id = "window-2",
            sessionId = "session-1",
            packageName = "com.twitter.android",
            grantedAt = now - 600_000L,
            expiresAt = now - 60_000L, // expired
            reason = null,
            restrictedToActivity = null,
        )

        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android", "com.twitter.android"),
            activeAccessWindows = listOf(activeWindow1, expiredWindow2),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `shouldBlock convenience method should match decide`() {
        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
        )
        val shouldBlockResult = engine.shouldBlock(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
        )

        assertEquals(decision is BlockDecision.Block, shouldBlockResult)
    }

    @Test
    fun `getBlockReason should return reason when blocking`() {
        val reason = engine.getBlockReason(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
        )

        assertNotNull(reason)
        assertEquals(BlockReason.TargetApp, reason)
    }

    @Test
    fun `getBlockReason should return null when allowing`() {
        val reason = engine.getBlockReason(
            foregroundPackage = "com.spotify.music",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
        )

        assertNull(reason)
    }

    // ─── COMPREHENSIVE SCENARIOS ─────────────────────────────────

    @Test
    fun `scenario: user tries to open Instagram during soft focus session`() {
        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android", "com.twitter.android"),
            systemAllowlist = BlockDecisionEngine.DEFAULT_SYSTEM_ALLOWLIST,
            userAllowlist = setOf(),
        )

        assertTrue(decision is BlockDecision.Block)
        assertEquals(BlockReason.TargetApp, (decision as BlockDecision.Block).reason)
    }

    @Test
    fun `scenario: user opens Settings during hard lock`() {
        val decision = engine.decide(
            foregroundPackage = "com.android.settings",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.instagram.android"),
            hardLockExtras = setOf("com.android.settings", "com.android.vending"),
            systemAllowlist = BlockDecisionEngine.DEFAULT_SYSTEM_ALLOWLIST,
        )

        assertTrue(decision is BlockDecision.Block)
        assertEquals(BlockReason.HardLockExtra, (decision as BlockDecision.Block).reason)
    }

    @Test
    fun `scenario: user receives a phone call during session`() {
        val decision = engine.decide(
            foregroundPackage = "com.android.dialer",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.instagram.android"),
            isDialer = true,
            isInCall = true,
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `scenario: user opens SystemUI during session`() {
        val decision = engine.decide(
            foregroundPackage = "com.android.systemui",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.instagram.android"),
            systemAllowlist = BlockDecisionEngine.DEFAULT_SYSTEM_ALLOWLIST,
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `scenario: session has ended - no blocking`() {
        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = false, // session ended
            lockMode = null,
            targetPackages = emptyList(),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `scenario: user opens Play Store during hard lock`() {
        val decision = engine.decide(
            foregroundPackage = "com.android.vending",
            sessionActive = true,
            lockMode = LockMode.Hard,
            targetPackages = listOf("com.instagram.android"),
            hardLockExtras = setOf("com.android.vending"),
            systemAllowlist = emptySet(),
        )

        assertTrue(decision is BlockDecision.Block)
        assertEquals(BlockReason.HardLockExtra, (decision as BlockDecision.Block).reason)
    }

    @Test
    fun `scenario: bypass granted and user opens target app`() {
        val now = System.currentTimeMillis()
        val activeWindow = AccessWindow(
            id = "window-1",
            sessionId = "session-1",
            packageName = "com.instagram.android",
            grantedAt = now - 30_000L,
            expiresAt = now + 270_000L, // 4.5 min remaining
            reason = "user reason text",
            restrictedToActivity = null,
        )

        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
            activeAccessWindows = listOf(activeWindow),
        )

        assertTrue(decision is BlockDecision.Allow)
    }

    @Test
    fun `scenario: bypass window just expired`() {
        val now = System.currentTimeMillis()
        val expiredWindow = AccessWindow(
            id = "window-1",
            sessionId = "session-1",
            packageName = "com.instagram.android",
            grantedAt = now - 600_000L,
            expiresAt = now, // just expired
            reason = null,
            restrictedToActivity = null,
        )

        val decision = engine.decide(
            foregroundPackage = "com.instagram.android",
            sessionActive = true,
            lockMode = LockMode.Soft,
            targetPackages = listOf("com.instagram.android"),
            activeAccessWindows = listOf(expiredWindow),
        )

        assertTrue(decision is BlockDecision.Block)
    }
}
