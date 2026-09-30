import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Тесты технического формата версии вида `major.minor.(patch * 10_000 + stageOffset + iteration)`,
 * который собирается в `build.gradle.kts` и попадает в `BuildKonfig.PROJECT_VERSION`.
 * */
class AppVersionTechnicalNormalizeTest {

    @Test
    fun `parses technical version strings into typed objects`() {
        PARSING_CASES.forEach { (raw, expected) ->
            assertEquals(expected, raw.normalizeFromTechnical(), "Неверный разбор технической версии '$raw'")
        }
    }

    @Test
    fun `maps stage codes outside the stage list to release`() {
        UNEXPECTED_STAGE_CASES.forEach { (raw, expected) ->
            assertEquals(expected, raw.normalizeFromTechnical(), "Неверная стадия для '$raw'")
        }
    }

    @Test
    fun `rejects technical strings that do not match the pattern`() {
        PATTERN_MISMATCH_CASES.forEach { raw ->
            assertNormalizeError(raw, AppVersion.NORMALIZE_ERROR)
        }
    }

    @Test
    fun `rejects technical strings whose parts do not fit into int`() {
        OVERFLOW_CASES.forEach { raw ->
            assertNormalizeError(raw, AppVersion.NORMALIZE_ERROR)
        }
    }

    @Test
    fun `decodes to the same version as the ui format`() {
        UI_TWIN_CASES.forEach { (technical, ui) ->
            val message = "Техническая '$technical' и ui-версия '$ui' разошлись"
            assertEquals(ui.normalizeFromUi(), technical.normalizeFromTechnical(), message)
        }
    }

    @Test
    fun `rejects ui formatted version strings`() {
        UI_FORMATTED_CASES.forEach { raw ->
            assertNormalizeError(raw, AppVersion.NORMALIZE_ERROR)
        }
    }

    @Test
    fun `accepts plain three part versions that the ui format rejects`() {
        assertEquals(
            AppVersion(1, 2, 0, AppVersion.Stage.Release, 3),
            "1.2.3".normalizeFromTechnical(),
            "Трёхзначный хвост разобран как patch, стадия и build"
        )
        assertFailsWith<IllegalStateException> { "1.2.3".normalizeFromUi() }
    }

    @Test
    fun `round trips versions encoded by the gradle script`() {
        ROUND_TRIP_CASES.forEach { version ->
            val encoded = encode(version)
            assertEquals(version, encoded.normalizeFromTechnical(), "Round-trip теряет данные для $version")
        }
    }

    @Test
    fun `loses build iteration beyond the thousand`() {
        val encoded = encode(AppVersion(0, 2, 0, AppVersion.Stage.Alpha, 1_000))
        assertEquals("0.2.2000", encoded, "Итерация 1000 кодируется неверно")
        assertEquals(
            AppVersion(0, 2, 0, AppVersion.Stage.Beta, 0),
            encoded.normalizeFromTechnical(),
            "Итерация от 1000 переполняет разряд стадии"
        )
    }

    /**
     * Повторяет кодирование из `build.gradle.kts`: разряд patch, разряд стадии, разряд итерации.
     * */
    private fun encode(version: AppVersion): String {
        val stageOffset = when (version.stage) {
            AppVersion.Stage.Alpha -> 1_000
            AppVersion.Stage.Beta -> 2_000
            AppVersion.Stage.RC -> 3_000
            AppVersion.Stage.Release -> 4_000
        }
        val collapsed = version.patch * 10_000 + stageOffset + version.build
        return "${version.major}.${version.minor}.$collapsed"
    }

    private fun assertNormalizeError(version: String, expectedMessage: String) {
        val exception = assertFailsWith<IllegalStateException> { version.normalizeFromTechnical() }
        assertEquals(expectedMessage, exception.message, "Неожиданное сообщение для '$version'")
    }

    private companion object {
        val PARSING_CASES = listOf(
            "0.2.1002" to AppVersion(0, 2, 0, AppVersion.Stage.Alpha, 2),
            "0.2.2005" to AppVersion(0, 2, 0, AppVersion.Stage.Beta, 5),
            "0.2.3010" to AppVersion(0, 2, 0, AppVersion.Stage.RC, 10),
            "0.2.4007" to AppVersion(0, 2, 0, AppVersion.Stage.Release, 7),
            "0.2.11002" to AppVersion(0, 2, 1, AppVersion.Stage.Alpha, 2),
            "1.2.31005" to AppVersion(1, 2, 3, AppVersion.Stage.Alpha, 5),
            "1.20.39999" to AppVersion(1, 20, 3, AppVersion.Stage.Release, 999),
            "01.02.12005" to AppVersion(1, 2, 1, AppVersion.Stage.Beta, 5),
            "10.20.24000" to AppVersion(10, 20, 2, AppVersion.Stage.Release, 0),
        )

        /**
         * Разряды стадии за пределами 1..4 (включая 0) схлопываются в Release — `when` без
         * ветки по умолчанию, отличной от Release, не отличает их от настоящей release.
         * */
        val UNEXPECTED_STAGE_CASES = listOf(
            "0.2.0" to AppVersion(0, 2, 0, AppVersion.Stage.Release, 0),
            "0.2.5007" to AppVersion(0, 2, 0, AppVersion.Stage.Release, 7),
            "0.2.9000" to AppVersion(0, 2, 0, AppVersion.Stage.Release, 0),
        )

        val PATTERN_MISMATCH_CASES = listOf(
            "", "not-a-version", "1.2", "1.2.-3", "1.2.3.4", "v1.2.3",
            " 1.2.3", "1.2.3 ", "1.2.", ".1.2", "1..3",
        )

        val OVERFLOW_CASES = listOf(
            "99999999999.0.0", "1.99999999999.0", "1.2.99999999999",
        )

        val UI_TWIN_CASES = listOf(
            "0.2.1002" to "0.2.0-Alpha.2",
            "0.2.11002" to "0.2.1-Alpha.2",
            "1.20.39999" to "1.20.3-Release.999",
            "2.3.72999" to "2.3.7-Beta.999",
        )

        val UI_FORMATTED_CASES = listOf(
            "0.2.0-Alpha.2", "1.2.3-Release.1", "0.2.11002-Alpha.2",
        )

        val ROUND_TRIP_CASES = listOf(
            AppVersion(0, 0, 0, AppVersion.Stage.Release, 0),
            AppVersion(0, 0, 0, AppVersion.Stage.RC, 0),
            AppVersion(0, 2, 0, AppVersion.Stage.Alpha, 2),
            AppVersion(0, 2, 1, AppVersion.Stage.Alpha, 2),
            AppVersion(1, 0, 0, AppVersion.Stage.Release, 0),
            AppVersion(2, 3, 7, AppVersion.Stage.Beta, 999),
            AppVersion(10, 20, 30, AppVersion.Stage.Release, 999),
        )
    }
}
