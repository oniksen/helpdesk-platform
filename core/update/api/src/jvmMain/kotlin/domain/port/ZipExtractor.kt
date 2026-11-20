package domain.port

import java.io.File

/**
 * Порт для распаковки zip-архивов.
 *
 * Определяет контракт для извлечения файлов из zip-архива и проверки
 * целостности архива без распаковки.
 */
interface ZipExtractor {

    /**
     * Распаковывает zip-архив в указанную директорию.
     *
     * @param zipFile Zip-архив с обновлением.
     * @param destinationDir Целевая директория для распакованных файлов.
     * @return `true` — распаковка выполнена успешно, `false` — произошла ошибка.
     */
    suspend fun extract(
        zipFile: File,
        destinationDir: File,
    ): Boolean

    /**
     * Проверяет целостность zip-архива без его распаковки.
     *
     * @param zipFile Zip-архив для проверки.
     * @return `true` — архив корректен, `false` — архив повреждён.
     */
    suspend fun verify(zipFile: File): Boolean
}
