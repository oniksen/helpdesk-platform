import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class AppVersionTest {

    @Test
    fun parsesVersionString() {
        PARSING_CASES.forEach { (raw, expected) ->
            assertEquals(expected, raw.normalized(), "Неверный разбор версии '$raw'")
        }
    }

    @Test
    fun rejectsInvalidVersionStrings() {
        INVALID_CASES.forEach { assertNormalizeError(it) }
    }

    @Test
    fun equalityDependsOnEveryPart() {
        assertEquals(BASE_VERSION, BASE_VERSION.copy(), "Копия версии не равна оригиналу")
        assertEquals(BASE_VERSION, BASE_VERSION.toString().normalized(), "Версия не равна себе же после разбора")
        VARIANT_CASES.forEach { (variant, raw) ->
            val message = "Версии отличаются одной частью, но равны: $BASE_VERSION и $variant"
            assertNotEquals(BASE_VERSION, variant, message)
            assertNotEquals(BASE_VERSION, raw.normalized(), message)
        }
    }

    @Test
    fun negativeNumbersAreRejected() {
        NEGATIVE_CASES.forEach { (major, minor, patch, build) ->
            val exception = assertFailsWith<IllegalArgumentException> {
                AppVersion(major, minor, patch, AppVersion.Stage.Release, build)
            }
            val message = "Неожиданное сообщение для $major.$minor.$patch-Release.$build"
            assertEquals(AppVersion.INVALID_NUMBER_ERROR, exception.message, message)
        }
    }

    private fun assertNormalizeError(version: String) {
        val exception = assertFailsWith<IllegalStateException> { version.normalized() }
        assertEquals(AppVersion.NORMALIZE_ERROR, exception.message, "Неожиданное сообщение для '$version'")
    }

    private companion object {
        val BASE_VERSION = AppVersion(1, 2, 3, AppVersion.Stage.RC, 4)

        val PARSING_CASES = listOf(
            "0.1.1-alpha.1" to AppVersion(0, 1, 1, AppVersion.Stage.Alpha, 1),
            "0.1.1-Alpha.1" to AppVersion(0, 1, 1, AppVersion.Stage.Alpha, 1),
            "1.2.3-Beta.2" to AppVersion(1, 2, 3, AppVersion.Stage.Beta, 2),
            "1.2.3-RC.10" to AppVersion(1, 2, 3, AppVersion.Stage.RC, 10),
            "1.2.3-Release.10" to AppVersion(1, 2, 3, AppVersion.Stage.Release, 10),
            "0.0.0-Release.0" to AppVersion(0, 0, 0, AppVersion.Stage.Release, 0),
            "01.02.03-Alpha.007" to AppVersion(1, 2, 3, AppVersion.Stage.Alpha, 7),
            "10.20.30-Release.999" to AppVersion(10, 20, 30, AppVersion.Stage.Release, 999),
        )

        val VARIANT_CASES = listOf(
            BASE_VERSION.copy(major = 9) to "9.2.3-RC.4",
            BASE_VERSION.copy(minor = 9) to "1.9.3-RC.4",
            BASE_VERSION.copy(patch = 9) to "1.2.9-RC.4",
            BASE_VERSION.copy(stage = AppVersion.Stage.Release) to "1.2.3-Release.4",
            BASE_VERSION.copy(build = 9) to "1.2.3-RC.9",
        )

        val INVALID_CASES = listOf(
            "", "not-a-version", "1.2.3", "1.2-Release.1", "v1.2.3-Release.1",
            "1.2.3-Release.x", "1.2.3--.1", "1.2.3-Release.5+build.42",
            "1.2.3-Snapshot.1", "99999999999.0.0-Release.0",
        )

        val NEGATIVE_CASES = listOf(
            listOf(-1, 2, 3, 4),
            listOf(1, -2, 3, 4),
            listOf(1, 2, -3, 4),
            listOf(1, 2, 3, -4),
        )
    }
}
