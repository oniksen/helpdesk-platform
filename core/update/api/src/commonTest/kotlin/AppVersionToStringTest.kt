import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AppVersionToStringTest {

    @Test
    fun `formats version as major minor patch stage build`() {
        FORMATTING_CASES.forEach { (version, expected) ->
            assertEquals(expected, version.toString(), "Неверный формат для $version")
        }
    }

    @Test
    fun `formatted version is parsed back without changes`() {
        FORMATTING_CASES.forEach { (version, _) ->
            assertEquals(version, version.toString().normalize(), "Round-trip теряет данные для $version")
        }
    }

    @Test
    fun `formats version canonically after parsing`() {
        CANONICAL_CASES.forEach { (raw, expected) ->
            assertEquals(expected, raw.normalize().toString(), "Неверная канонизация для '$raw'")
        }
    }

    @Test
    fun `every version part changes formatted string`() {
        val base = AppVersion(1, 2, 3, AppVersion.Stage.RC, 4)
        val variants = listOf(
            base.copy(major = 9),
            base.copy(minor = 9),
            base.copy(patch = 9),
            base.copy(stage = AppVersion.Stage.Release),
            base.copy(build = 9),
        )
        val formatted = variants.map { it.toString() }
        val baseString = base.toString()
        assertEquals(variants.size, formatted.toSet().size, "Части версии не попали в формат: $formatted")
        formatted.forEach { assertNotEquals(baseString, it, "Вариант не отличился от базовой версии") }
    }

    private companion object {
        val FORMATTING_CASES = listOf(
            AppVersion(0, 0, 0, AppVersion.Stage.Release, 0) to "0.0.0-Release.0",
            AppVersion(0, 1, 1, AppVersion.Stage.Alpha, 1) to "0.1.1-Alpha.1",
            AppVersion(1, 2, 3, AppVersion.Stage.Beta, 2) to "1.2.3-Beta.2",
            AppVersion(1, 2, 3, AppVersion.Stage.RC, 10) to "1.2.3-RC.10",
            AppVersion(1, 2, 3, AppVersion.Stage.Release, 10) to "1.2.3-Release.10",
            AppVersion(10, 20, 30, AppVersion.Stage.Release, 999) to "10.20.30-Release.999",
            AppVersion(Int.MAX_VALUE, 0, 0, AppVersion.Stage.Alpha, 0) to "2147483647.0.0-Alpha.0",
        )

        val CANONICAL_CASES = listOf(
            "1.2.3-rc.1" to "1.2.3-RC.1",
            "1.2.3-alpha.1" to "1.2.3-Alpha.1",
            "01.02.03-Alpha.007" to "1.2.3-Alpha.7",
            "0.0.0-Release.0" to "0.0.0-Release.0",
        )
    }
}
