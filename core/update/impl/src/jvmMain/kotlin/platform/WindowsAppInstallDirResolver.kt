package platform

import java.io.File

/**
 * Windows-реализация [AppInstallDirResolver].
 *
 * Определяет директорию установки по расположению JAR-файлов:
 * ищет main JAR в classpath (исключая кеши Gradle/Maven),
 * и поднимается на уровень выше, если JAR находится в `lib/` или `app/`.
 * Если JAR не найден — использует `user.dir`.
 */
class WindowsAppInstallDirResolver : AppInstallDirResolver {

    override fun getAppInstallDir(referenceClass: Class<*>): File {
        // Пытаемся определить через code source загруженного класса
        try {
            val location = referenceClass.protectionDomain.codeSource.location.toURI()
            val file = File(location)
            if (file.name.endsWith(JAR_EXTENSION)) {
                val dir = file.parentFile
                return if (dir != null && (dir.name == LIB_DIR || dir.name == APP_DIR)) {
                    dir.parentFile
                } else {
                    dir ?: File(".")
                }
            }
        } catch (_: Exception) {
            // code source недоступен — используем fallback ниже
        }

        // Fallback: поиск main JAR в classpath
        val jarFile = findMainJar()
        if (jarFile != null) {
            val jar = File(jarFile)
            val parent = jar.parentFile
            return if (parent.name == LIB_DIR || parent.name == APP_DIR) parent.parentFile else parent
        }

        return File(System.getProperty("user.dir"))
    }

    override fun getAppBundlePath(referenceClass: Class<*>): File? = null

    /** Ищет первый JAR из classpath, не находящийся в кешах Gradle или Maven. */
    private fun findMainJar(): String? {
        val classPath = System.getProperty("java.class.path")
        return classPath.split(File.pathSeparatorChar)
            .filter { it.endsWith(JAR_EXTENSION) }
            .firstOrNull { jarPath ->
                val path = File(jarPath).canonicalPath
                !path.contains(".gradle") &&
                    !path.contains(".m2") &&
                    !path.contains(File.separator + "cache" + File.separator)
            }
    }

    private companion object {
        const val JAR_EXTENSION = ".jar"
        const val LIB_DIR = "lib"
        const val APP_DIR = "app"
    }
}
