package data.impl

import domain.port.ZipExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Реализация [ZipExtractor] для распаковки zip-архивов с защитой от Zip Slip-атаки.
 *
 * ## Процесс распаковки
 * 1. Проверяет и создаёт целевую директорию, если она не существует.
 * 2. Вычисляет канонический путь целевой директории для проверки безопасности.
 * 3. Итерируется по всем записям (entry) zip-архива:
 *    - пропускает служебную папку macOS `__MACOSX` и пустые имена;
 *    - проверяет, что канонический путь записи остаётся внутри целевой директории
 *      (защита от Zip Slip);
 *    - создаёт директории и записывает файлы с буферизацией 8 КБ;
 *    - сохраняет бит исполнимости для `MacOS/` и `bin/`.
 * 4. При любой ошибке возвращает `false`.
 */
class ZipExtractorImpl : ZipExtractor {

    override suspend fun verify(zipFile: File): Boolean = try {
        withContext(Dispatchers.IO) {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory) {
                        zip.getInputStream(entry).use { input ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            while (input.read(buffer) != -1) {
                                // Чтение до конца — проверка CRC и структуры архива
                            }
                        }
                    }
                }
            }
            true
        }
    } catch (error: Exception) {
        println("[ZipExtractor] Архив повреждён: ${error.message}")
        false
    }

    override suspend fun extract(zipFile: File, destinationDir: File): Boolean = try {
        withContext(Dispatchers.IO) {
            if (!destinationDir.exists() && !destinationDir.mkdirs()) {
                return@withContext false
            }

            val canonicalDest = destinationDir.canonicalPath
                .let { if (it.endsWith(File.separator)) it else it + File.separator }

            FileInputStream(zipFile).buffered().use { inputStream ->
                ZipInputStream(inputStream).use { zipInputStream ->
                    extractEntries(zipInputStream, destinationDir, canonicalDest)
                }
            }
            true
        }
    } catch (error: Exception) {
        println("[ZipExtractor] Распаковка не удалась: ${error.message}")
        false
    }

    private fun extractEntries(
        zipInputStream: ZipInputStream,
        destinationDir: File,
        canonicalDest: String,
    ) {
        var entry = zipInputStream.nextEntry

        while (entry != null) {
            val normalizedName = normalizeEntryName(entry.name)

            if (normalizedName == null || normalizedName.startsWith(MACOS_METADATA_DIR)) {
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
                continue
            }

            val outputFile = File(destinationDir, normalizedName)

            // Запись не должна выходить за пределы целевой директории
            if (!outputFile.canonicalPath.startsWith(canonicalDest)) {
                throw SecurityException("Zip entry tries to escape destination directory: $normalizedName")
            }

            if (entry.isDirectory) {
                outputFile.mkdirs()
            } else {
                writeEntry(zipInputStream, outputFile, normalizedName)
            }

            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
    }

    private fun writeEntry(zipInputStream: ZipInputStream, outputFile: File, normalizedName: String) {
        outputFile.parentFile?.mkdirs()

        FileOutputStream(outputFile).buffered().use { outputStream ->
            val buffer = ByteArray(BUFFER_SIZE)
            var length: Int
            while (zipInputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
        }

        // Сохраняем бит исполнимости (macOS: лаунчер приложения, JRE)
        if (normalizedName.contains(MACOS_EXECUTABLE_DIR) || normalizedName.contains(BIN_DIR)) {
            outputFile.setExecutable(true, false)
        }
    }

    /**
     * Нормализует имя entry из zip-архива:
     * 1. Если entry содержит обёртку `.app` — срезает префикс до `Contents/`.
     * 2. Если entry начинается с `Contents/` — срезает этот префикс,
     *    т.к. директория установки уже указывает на `Contents/`.
     *
     * @return нормализованное имя или `null`, если имя стало пустым.
     */
    private fun normalizeEntryName(entryName: String): String? {
        val afterApp = if (APP_WRAPPER in entryName) {
            entryName.substring(entryName.indexOf(APP_WRAPPER) + APP_WRAPPER.length)
        } else {
            entryName
        }

        val result = afterApp.removePrefix(CONTENTS_PREFIX)
        return result.ifEmpty { null }
    }

    private companion object {
        const val BUFFER_SIZE = 8192
        const val APP_WRAPPER = ".app/"
        const val CONTENTS_PREFIX = "Contents/"
        const val MACOS_METADATA_DIR = "__MACOSX"
        const val MACOS_EXECUTABLE_DIR = "MacOS/"
        const val BIN_DIR = "/bin/"
    }
}
