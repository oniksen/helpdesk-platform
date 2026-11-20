package platform

import java.io.File

/**
 * Платформенный резолвер директории установки приложения.
 *
 * Определяет, где физически расположены файлы приложения, для безопасной замены
 * при обновлении. На macOS дополнительно определяет путь к `.app` бандлу
 * для переподписи кода.
 */
interface AppInstallDirResolver {

    /**
     * Возвращает директорию установки приложения (содержит файлы для замены).
     *
     * @param referenceClass Любой класс приложения для определения code source.
     */
    fun getAppInstallDir(referenceClass: Class<*>): File

    /**
     * Возвращает путь к `.app` бандлу (только macOS) или `null`.
     *
     * @param referenceClass Любой класс приложения для определения code source.
     */
    fun getAppBundlePath(referenceClass: Class<*>): File?
}
