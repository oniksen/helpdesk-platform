package platform

import java.io.File

/**
 * macOS-реализация [AppInstallDirResolver].
 *
 * Сначала пытается найти `.app` бандл, поднимаясь по дереву директорий
 * от `user.dir` или code source класса. Если бандл найден — возвращает
 * `{bundle}/Contents/` как installDir, а `getAppBundlePath` — сам бандл.
 *
 * Если бандл не найден — использует fallback-логику через JAR (как Windows).
 */
class MacosAppInstallDirResolver : AppInstallDirResolver {

    override fun getAppInstallDir(referenceClass: Class<*>): File {
        val bundleDir = resolveAppBundleDir(referenceClass)
        if (bundleDir != null) return File(bundleDir, CONTENTS_DIR)

        return fallbackInstallDir(referenceClass)
    }

    override fun getAppBundlePath(referenceClass: Class<*>): File? =
        resolveAppBundleDir(referenceClass)

    /**
     * Ищет `.app` бандл: сначала от `user.dir`, затем от code source класса.
     * Поднимается по родительским директориям, пока не найдёт папку с расширением .app.
     */
    private fun resolveAppBundleDir(referenceClass: Class<*>): File? {
        val userDir = System.getProperty("user.dir")
        if (userDir != null) {
            val result = findBundleAncestor(File(userDir))
            if (result != null) return result
        }

        try {
            val location = referenceClass.protectionDomain.codeSource.location.toURI()
            val locationFile = File(location)
            val result = findBundleAncestor(
                if (locationFile.isFile) locationFile.parentFile else locationFile,
            )
            if (result != null) return result
        } catch (_: Exception) {
            // code source недоступен — бандл не найден
        }

        return null
    }

    /** Поднимается по дереву директорий в поисках `.app` бандла с папкой Contents. */
    private fun findBundleAncestor(start: File?): File? {
        var current = start

        while (current != null) {
            if (current.name.endsWith(APP_EXTENSION) && File(current, CONTENTS_DIR).isDirectory) {
                return current
            }
            current = current.parentFile
        }

        return null
    }

    /** Fallback: определение директории через JAR (аналогично Windows-реализации). */
    private fun fallbackInstallDir(referenceClass: Class<*>): File {
        try {
            val location = referenceClass.protectionDomain.codeSource.location.toURI()
            val file = File(location)
            if (file.name.endsWith(JAR_EXTENSION)) {
                val dir = file.parentFile
                return if (dir != null && dir.name == LIB_DIR) dir.parentFile else dir ?: File(".")
            }
        } catch (_: Exception) {
            // code source недоступен — используем fallback ниже
        }

        val jarFile = findMainJar()
        if (jarFile != null) {
            val jar = File(jarFile)
            val parent = jar.parentFile
            return if (parent.name == LIB_DIR) parent.parentFile else parent
        }

        return File(System.getProperty("user.dir"))
    }

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
        const val APP_EXTENSION = ".app"
        const val CONTENTS_DIR = "Contents"
        const val JAR_EXTENSION = ".jar"
        const val LIB_DIR = "lib"
    }
}
