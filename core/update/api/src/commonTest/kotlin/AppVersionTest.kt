import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class AppVersionTest {

    @Test
    fun parsesLowercaseAlphaVersion() {
        assertEquals(AppVersion(0, 1, 1, AppVersion.Stage.Alpha, 1), "0.1.1-alpha.1".normalized())
    }

    @Test
    fun parsesUppercaseAlphaVersion() {
        assertEquals(AppVersion(0, 1, 1, AppVersion.Stage.Alpha, 1), "0.1.1-Alpha.1".normalized())
    }

    @Test
    fun parsesBetaVersion() {
        assertEquals(AppVersion(1, 2, 3, AppVersion.Stage.Beta, 2), "1.2.3-Beta.2".normalized())
    }

    @Test
    fun parsesRcVersion() {
        assertEquals(AppVersion(1, 2, 3, AppVersion.Stage.RC, 10), "1.2.3-RC.10".normalized())
    }

    @Test
    fun parsesReleaseVersion() {
        assertEquals(AppVersion(1, 2, 3, AppVersion.Stage.Release, 10), "1.2.3-Release.10".normalized())
    }

    @Test
    fun parsesZeroVersion() {
        assertEquals(AppVersion(0, 0, 0, AppVersion.Stage.Release, 0), "0.0.0-Release.0".normalized())
    }

    @Test
    fun parsesLeadingZeros() {
        assertEquals(AppVersion(1, 2, 3, AppVersion.Stage.Alpha, 7), "01.02.03-Alpha.007".normalized())
    }

    @Test
    fun parsesMultiDigitNumbers() {
        assertEquals(AppVersion(10, 20, 30, AppVersion.Stage.Release, 999), "10.20.30-Release.999".normalized())
    }

    @Test
    fun emptyStringFails() {
        assertNormalizeError("")
    }

    @Test
    fun garbageFails() {
        assertNormalizeError("not-a-version")
    }

    @Test
    fun missingStageFails() {
        assertNormalizeError("1.2.3")
    }

    @Test
    fun missingPatchFails() {
        assertNormalizeError("1.2-Release.1")
    }

    @Test
    fun versionPrefixFails() {
        assertNormalizeError("v1.2.3-Release.1")
    }

    @Test
    fun nonNumericBuildFails() {
        assertNormalizeError("1.2.3-Release.x")
    }

    @Test
    fun emptyStageFails() {
        assertNormalizeError("1.2.3--.1")
    }

    @Test
    fun trailingMetadataFails() {
        assertNormalizeError("1.2.3-Release.5+build.42")
    }

    @Test
    fun unknownStageFails() {
        assertNormalizeError("1.2.3-Snapshot.1")
    }

    @Test
    fun numberOverflowFails() {
        assertNormalizeError("99999999999.0.0-Release.0")
    }

    @Test
    fun equalVersionsAreEqual() {
        assertEquals("1.2.3-Release.5".normalized(), "1.2.3-Release.5".normalized())
    }

    @Test
    fun versionsDifferingByBuildAreNotEqual() {
        assertNotEquals("1.2.3-Release.5".normalized(), "1.2.3-Release.6".normalized())
    }

    private fun assertNormalizeError(version: String) {
        val exception = assertFailsWith<IllegalStateException> { version.normalized() }
        assertEquals(AppVersion.NORMALIZE_ERROR, exception.message, "Неожиданное сообщение для '$version'")
    }
}
