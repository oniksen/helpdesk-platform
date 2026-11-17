package data.impl

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Тесты распаковки архива обновления: проверка целостности, нормализация путей,
 * отсечение служебных файлов macOS и защита от Zip Slip.
 */
class ZipExtractorImplTest {

    private val extractor = ZipExtractorImpl()
    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        workDir = Files.createTempDirectory("zip-extractor-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    @Test
    fun `reports readability of archives`() = runBlocking {
        val intact = createZip("app/main.jar" to CONTENT)
        val truncated = createZip("app/main.jar" to CONTENT).apply {
            writeBytes(readBytes().copyOf(length().toInt() / 2))
        }
        val notAnArchive = File(workDir, "broken.zip").apply { writeText("not a zip") }

        val cases = listOf(
            Triple("целый архив", intact, true),
            Triple("обрезанный архив", truncated, false),
            Triple("файл не является архивом", notAnArchive, false),
        )

        cases.forEach { (label, archive, expected) ->
            assertEquals(expected, extractor.verify(archive), "Неверный результат проверки для случая: $label")
        }
    }

    @Test
    fun `normalizes entry paths relative to the install directory`() = runBlocking {
        NORMALIZATION_CASES.forEach { (entryName, expectedPath) ->
            val destination = File(workDir, "dest-${entryName.hashCode()}")

            assertTrue(
                extractor.extract(createZip(entryName to CONTENT), destination),
                "Архив с записью '$entryName' не распакован",
            )
            assertTrue(
                File(destination, expectedPath).isFile,
                "После распаковки '$entryName' ожидался файл '$expectedPath'",
            )
        }
    }

    @Test
    fun `ignores macOS metadata entries`() = runBlocking {
        val destination = File(workDir, "dest-metadata")
        val archive = createZip(
            "Contents/app/main.jar" to CONTENT,
            "__MACOSX/Contents/._main.jar" to CONTENT,
            "Contents/__MACOSX/._main.jar" to CONTENT,
        )

        assertTrue(extractor.extract(archive, destination), "Архив со служебными файлами macOS не распакован")
        assertTrue(File(destination, "app/main.jar").isFile, "Полезный файл архива не распакован")
        assertFalse(File(destination, "__MACOSX").exists(), "Служебная папка __MACOSX попала в установку")
    }

    @Test
    fun `rejects entries that escape the destination directory`() = runBlocking {
        val destination = File(workDir, "dest-slip")

        assertFalse(
            extractor.extract(createZip("../escaped.txt" to CONTENT), destination),
            "Архив с выходом за пределы каталога установки распакован",
        )
        assertFalse(File(workDir, "escaped.txt").exists(), "Файл записан за пределы каталога установки")
    }

    /** Создаёт zip-архив с перечисленными записями. */
    private fun createZip(vararg entries: Pair<String, String>): File {
        val archive = Files.createTempFile(workDir.toPath(), "archive", ".zip").toFile()

        ZipOutputStream(archive.outputStream()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }

        return archive
    }

    private companion object {
        const val CONTENT = "payload"

        /** Имя записи в архиве → ожидаемый путь после нормализации. */
        val NORMALIZATION_CASES = listOf(
            "app/main.jar" to "app/main.jar",
            "Contents/app/main.jar" to "app/main.jar",
            "Helpdesk.app/Contents/MacOS/launcher" to "MacOS/launcher",
        )
    }
}
