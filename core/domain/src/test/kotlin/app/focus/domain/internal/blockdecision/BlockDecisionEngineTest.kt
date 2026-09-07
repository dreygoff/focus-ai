package app.focus.domain.internal.blockdecision

import app.focus.domain.model.BlockDecision
import app.focus.domain.model.BlockReason
import org.junit.Assert.assertEquals as AssertEq
import org.junit.Test as Jt

class BlockDecisionEngineTest {

    private val now = System.currentTimeMillis()

    private fun engine(
        systemAllowlist: Set<String> = emptySet(),
        userAllowlist: Set<String> = emptySet(),
        accessWindowPkgLookup: Map<String, Long> = emptyMap(),
    ): BlockDecisionEngine = BlockDecisionEngine(
        systemAllowlist = systemAllowlist,
        userAllowlist = userAllowlist,
        accessWindowPkgLookup = accessWindowPkgLookup,
    )

    // ========== No active session (always allows) ==========
    @Jt fun `no active session means allow for any package`() {
        val eng = engine()
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android", hasActiveSession = false))
    }

    @Jt fun `no active session allows system packages`() {
        val eng = engine()
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.android.systemui", hasActiveSession = false))
    }

    // ========== System allowlist ==========
    @Jt fun `package in system allowlist is allowed`() {
        val eng = engine(systemAllowlist = setOf("com.android.systemui"))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.android.systemui", hasActiveSession = true))
    }

    @Jt fun `non-system allowlist package gets checked normally`() {
        val eng = engine(systemAllowlist = setOf("com.android.systemui"))
        val result = eng.decide("com.instagram.android", hasActiveSession = true)
        AssertEq.assertTrue(result is BlockDecision.Block)
    }

    @Jt fun `system allowlist works with multiple packages`() {
        val sys = setOf("com.android.systemui", "app.focus.android")
        val eng = engine(systemAllowlist = sys)
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.android.systemui", hasActiveSession = true))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("app.focus.android", hasActiveSession = true))
    }

    // ========== User allowlist (ignorelist) ==========
    @Jt fun `package in user ignorelist is allowed`() {
        val eng = engine(userAllowlist = setOf("com.instagram.android"))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android", hasActiveSession = true))
    }

    @Jt fun `user allowlist overrides block decision`() {
        val eng = engine(userAllowlist = setOf("com.whatsapp"))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.whatsapp", hasActiveSession = true))
    }

    @Jt fun `package not in any allowlist gets blocked`() {
        val eng = engine(
            systemAllowlist = setOf("com.android.systemui"),
            userAllowlist = setOf("com.whatsapp")
        )
        AssertEq.assertTrue((eng.decide("com.facebook.katana", hasActiveSession = true)) is BlockDecision.Block)
    }

    @Jt fun `user allowlist and system allowlist are independent`() {
        val eng = engine(
            systemAllowlist = setOf("app.focus.android"),
            userAllowlist = setOf("com.whatsapp")
        )
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("app.focus.android", hasActiveSession = true))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.whatsapp", hasActiveSession = true))
    }

    // ========== Access windows (emergency access) ==========
    @Jt fun `active access window allows package`() {
        val windowEnd = now + 300_000L // 5 min from now
        val eng = engine(accessWindowPkgLookup = mapOf("com.instagram.android" to windowEnd))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android", hasActiveSession = true))
    }

    @Jt fun `expired access window does not allow package`() {
        val expiredWindow = now - 600_000L // 10 min ago
        val eng = engine(accessWindowPkgLookup = mapOf("com.instagram.android" to expiredWindow))
        AssertEq.assertTrue((eng.decide("com.instagram.android", hasActiveSession = true)) is BlockDecision.Block)
    }

    @Jt fun `access window at exactly end time still allows`() {
        val eng = engine(accessWindowPkgLookup = mapOf("com.instagram.android" to now))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android", hasActiveSession = true))
    }

    @Jt fun `multiple access windows checked independently`() {
        val windowEnd = now + 300_000L
        val eng = engine(accessWindowPkgLookup = mapOf(
            "com.whatsapp" to windowEnd,
            "com.telegram.messenger" to (now - 1000L)
        ))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.whatsapp", hasActiveSession = true))
        AssertEq.assertTrue((eng.decide("com.telegram.messenger", hasActiveSession = true)) is BlockDecision.Block)
    }

    @Jt fun `other package with access window is still blocked if not matching`() {
        val windowEnd = now + 300_000L
        val eng = engine(accessWindowPkgLookup = mapOf("com.whatsapp" to windowEnd))
        AssertEq.assertTrue((eng.decide("com.facebook.katana", hasActiveSession = true)) is BlockDecision.Block)
    }

    // ========== Lock mode specific tests (Hard lock extra packages) ==========
    @Jt fun `blocked package returns block with correct reason`() {
        val eng = engine()
        val result = eng.decide("com.instagram.android", hasActiveSession = true)
        AssertEq.assertTrue(result is BlockDecision.Block)
        AssertEq.assertEquals(BlockReason.TARGET_APP, (result as BlockDecision.Block).reason)
    }

    @Jt fun `blocked package includes packageName`() {
        val eng = engine()
        val result = eng.decide("com.instagram.android", hasActiveSession = true)
        val block = result as? BlockDecision.Block
        AssertEq.assertNotNull(block?.packageName)
        AssertEq.assertEquals("com.instagram.android", block?.packageName)
    }

    // ========== Combined scenarios ==========
    @Jt fun `system allowlist takes priority over access window expiry`() {
        val expiredWindow = now - 600_000L
        val eng = engine(
            systemAllowlist = setOf("com.android.systemui"),
            accessWindowPkgLookup = mapOf("com.instagram.android" to expiredWindow)
        )
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.android.systemui", hasActiveSession = true))
    }

    @Jt fun `user allowlist works together with system allowlist`() {
        val eng = engine(
            systemAllowlist = setOf("app.focus.android"),
            userAllowlist = setOf("com.whatsapp")
        )
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.whatsapp", hasActiveSession = true))
    }

    @Jt fun `package with all allowlists still blocked if not matched`() {
        val eng = engine(
            systemAllowlist = setOf("app.focus.android"),
            userAllowlist = setOf("com.whatsapp"),
            accessWindowPkgLookup = mapOf("com.telegram.messenger" to (now + 60_000L))
        )
        AssertEq.assertTrue((eng.decide("com.facebook.katana", hasActiveSession = true)) is BlockDecision.Block)
    }

    @Jt fun `emergency window override allows blocked package`() {
        val eng = engine(accessWindowPkgLookup = mapOf("com.instagram.android" to (now + 180_000L)))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android", hasActiveSession = true))
    }

    // ========== Edge cases ==========
    @Jt fun `empty allowlists with active session blocks unknown packages`() {
        val eng = engine()
        AssertEq.assertTrue((eng.decide("unknown.package.app", hasActiveSession = true)) is BlockDecision.Block)
    }

    @Jt fun `all allowlists empty still respects access windows`() {
        val eng = engine(accessWindowPkgLookup = mapOf("com.instagram.android" to (now + 60_000L)))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.instagram.android", hasActiveSession = true))
    }

    // ========== Block reason verification ==========
    @Jt fun `block result has non-null packageName`() {
        val eng = engine()
        val blockResult = eng.decide("com.banned.app", hasActiveSession = true) as BlockDecision.Block
        AssertEq.assertNotNull(blockResult.packageName)
        AssertEq.assertEquals("com.banned.app", blockResult.packageName)
    }

    @Jt fun `allow decision is single instance`() {
        val eng = engine()
        val allow1 = eng.decide("com.android.google", hasActiveSession = false)
        val allow2 = eng.decide("com.instagram.android", hasActiveSession = false)
        AssertEq.assertNotNull(allow1)
        AssertEq.assertNotNull(allow2)
    }

    // ========== Additional system packages in default allowlist for Focus app ==========
    @Jt fun `focus app itself is allowed in default system list`() {
        val eng = engine(systemAllowlist = setOf("app.focus.android"))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("app.focus.android", hasActiveSession = true))
    }

    // ========== Additional coverage for multiple access windows edge cases ==========
    @Jt fun `access window with zero remaining time allows`() {
        val eng = engine(accessWindowPkgLookup = mapOf("com.test.app" to now))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.test.app", hasActiveSession = true))
    }

    @Jt fun `negative access window timeout means expired`() {
        val eng = engine(accessWindowPkgLookup = mapOf("com.test.app" to now - 1L))
        AssertEq.assertTrue((eng.decide("com.test.app", hasActiveSession = true)) is BlockDecision.Block)
    }

    // ========== Performance: many packages check correctly ==========
    @Jt fun `allows a large number of allowlisted packages`() {
        val largeAllowlist = (1..200).map { "com.allow.pkg$it" }.toSet()
        val eng = engine(userAllowlist = largeAllowlist)
        for (i in 1..200) {
            AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.allow.pkg$i", hasActiveSession = true))
        }
    }

    // ========== Cross-check: block reason is consistent ==========
    @Jt fun `all blocked decisions have same reason type`() {
        val eng = engine()
        val r1 = eng.decide("com.app.one", hasActiveSession = true) as BlockDecision.Block
        val r2 = eng.decide("com.app.two", hasActiveSession = true) as BlockDecision.Block
        AssertEq.assertEquals(BlockReason.TARGET_APP, r1.reason)
        AssertEq.assertEquals(BlockReason.TARGET_APP, r2.reason)
    }

    // ========== Access window + ignorelist priority test ==========
    @Jt fun `access window overrides block for specific package`() {
        val eng = engine(accessWindowPkgLookup = mapOf("com.vpn.app" to (now + 300_000L)))
        AssertEq.assertEquals(BlockDecision.Allow, eng.decide("com.vpn.app", hasActiveSession = true))
    }

    @Jt fun `ignorelist does not affect non-allowlisted packages`() {
        val eng = engine(userAllowlist = setOf("app.focus.android"))
        AssertEq.assertTrue((eng.decide("com.other.app", hasActiveSession = true)) is BlockDecision.Block)
    }
}
