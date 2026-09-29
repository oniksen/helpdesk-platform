import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AppVersionTest {

    @Test
    fun `parses valid version strings into typed objects`() {
        PARSING_CASES.forEach { (raw, expected) ->
            assertEquals(expected, raw.normalize(), "Неверный разбор версии '$raw'")
        }
    }

    @Test
    fun `rejects invalid version strings`() {
        INVALID_CASES.forEach { assertNormalizeError(it) }
    }

    @Test
    fun `equality depends on every version part`() {
        assertEquals(BASE_VERSION, BASE_VERSION.copy(), "Копия версии не равна оригиналу")
        assertEquals(BASE_VERSION, BASE_VERSION.toString().normalize(), "Версия не равна себе же после разбора")
        VARIANT_CASES.forEach { (variant, raw) ->
            val message = "Версии отличаются одной частью, но равны: $BASE_VERSION и $variant"
            assertNotEquals(BASE_VERSION, variant, message)
            assertNotEquals(BASE_VERSION, raw.normalize(), message)
        }
    }

    @Test
    fun `rejects negative version numbers`() {
        NEGATIVE_CASES.forEach { (major, minor, patch, build) ->
            val exception = assertFailsWith<IllegalArgumentException> {
                AppVersion(major, minor, patch, AppVersion.Stage.Release, build)
            }
            val message = "Неожиданное сообщение для $major.$minor.$patch-Release.$build"
            assertEquals(AppVersion.INVALID_NUMBER_ERROR, exception.message, message)
        }
    }

    @Test
    fun `comparison depends on every version part`() {
        assertEquals(0, BASE_VERSION.compareTo(BASE_VERSION), "Версия не равна себе же по compareTo")
        assertEquals(0, BASE_VERSION.compareTo(BASE_VERSION.copy()), "Копия версии не равна оригиналу по compareTo")

        GREATER_CASES.forEach { greater ->
            val message = "$greater должен быть больше $BASE_VERSION"
            assertTrue(greater > BASE_VERSION, message)
            assertTrue(BASE_VERSION < greater, message)
            assertTrue(greater.compareTo(BASE_VERSION) > 0, message)
        }

        LESS_CASES.forEach { less ->
            val message = "$less должен быть меньше $BASE_VERSION"
            assertTrue(less < BASE_VERSION, message)
            assertTrue(BASE_VERSION > less, message)
            assertTrue(less.compareTo(BASE_VERSION) < 0, message)
        }

        STAGE_ORDERING.forEach { (lower, higher) ->
            val message = "Стадия $lower должна идти раньше стадии $higher"
            assertTrue(lower < higher, message)
            assertTrue(higher > lower, message)
        }

        PRECEDENCE_CASES.forEach { (greater, less) ->
            val message = "Старшая часть не перевесила младшие: $greater должно быть больше $less"
            assertTrue(greater > less, message)
            assertTrue(less < greater, message)
        }
    }

    private fun assertNormalizeError(version: String) {
        val exception = assertFailsWith<IllegalStateException> { version.normalize() }
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

        val GREATER_CASES = listOf(
            BASE_VERSION.copy(major = 2),
            BASE_VERSION.copy(minor = 3),
            BASE_VERSION.copy(patch = 4),
            BASE_VERSION.copy(stage = AppVersion.Stage.Release),
            BASE_VERSION.copy(build = 5),
        )

        val LESS_CASES = listOf(
            BASE_VERSION.copy(major = 0),
            BASE_VERSION.copy(minor = 1),
            BASE_VERSION.copy(patch = 2),
            BASE_VERSION.copy(stage = AppVersion.Stage.Beta),
            BASE_VERSION.copy(build = 3),
        )

        val STAGE_ORDERING = listOf(
            AppVersion.Stage.Alpha to AppVersion.Stage.Beta,
            AppVersion.Stage.Beta to AppVersion.Stage.RC,
            AppVersion.Stage.RC to AppVersion.Stage.Release,
        )

        /**
         * Старшая часть должна перевешивать все младшие: `major` решает сравнение,
         * даже если младшие части у старшей версии меньше, а у младшей — больше.
         * */
        val PRECEDENCE_CASES = listOf(
            AppVersion(2, 0, 0, AppVersion.Stage.Alpha, 0) to BASE_VERSION,
            BASE_VERSION to AppVersion(0, 9, 9, AppVersion.Stage.Release, 999),
        )
    }
}
