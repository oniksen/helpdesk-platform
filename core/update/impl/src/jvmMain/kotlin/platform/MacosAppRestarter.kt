package platform

import domain.AppRestarter
import java.io.File
import java.lang.management.ManagementFactory

/**
 * macOS-реализация [AppRestarter].
 *
 * Сначала пытается перезапустить приложение через `open -n {bundle}.app`
 * с задержкой 3 секунды (даёт время старому процессу завершиться).
 * Если бандл не найден или `open` не сработал — использует [launchWithJava]:
 * запускает JVM-процесс с теми же аргументами, фильтруя IDE-флаги и
 * устанавливая `VFORK` для обхода бага macOS `posix_spawn`.
 *
 * После запуска нового процесса завершает текущий через [System.exit].
 */
class MacosAppRestarter : AppRestarter {

    override fun restart() {
        val appBundlePath = findAppBundlePath()

        if (appBundlePath != null) {
            try {
                val script = "sleep $RESTART_DELAY_SECONDS && open -n '${appBundlePath.absolutePath}'"
                ProcessBuilder("sh", "-c", script).start()
                println("[MacosAppRestarter] Запланирован перезапуск бандла: $appBundlePath")
            } catch (error: Exception) {
                println("[MacosAppRestarter] 'open' не сработал (${error.message}), запуск через java")
                launchWithJava()
            }
        } else {
            launchWithJava()
        }

        System.exit(0)
    }

    /** Ищет `.app` бандл через `user.dir` или code source класса. */
    private fun findAppBundlePath(): File? {
        val userDir = System.getProperty("user.dir")
        if (userDir != null) {
            val appBundle = findBundleAncestor(File(userDir))
            if (appBundle != null) return appBundle
        }

        try {
            val location = this::class.java.protectionDomain.codeSource.location.toURI()
            val locationFile = File(location)
            val appBundle = findBundleAncestor(
                if (locationFile.isFile) locationFile.parentFile else locationFile,
            )
            if (appBundle != null) return appBundle
        } catch (_: Exception) {
            // code source недоступен — бандл не найден
        }

        return null
    }

    /** Поднимается по дереву директорий в поисках `.app` бандла с папкой MacOS. */
    private fun findBundleAncestor(start: File?): File? {
        var current = start

        while (current != null) {
            if (current.name.endsWith(APP_EXTENSION) &&
                File(File(current, CONTENTS_DIR), MACOS_DIR).isDirectory
            ) {
                return current
            }
            current = current.parentFile
        }

        return null
    }

    /** Запускает JVM-процесс с теми же аргументами (fallback, если `open` недоступен). */
    private fun launchWithJava() {
        val command = buildJavaCommand()
        println("[MacosAppRestarter] Запуск: ${command.joinToString(" ")}")
        ProcessBuilder(command).start()
    }

    private fun buildJavaCommand(): List<String> = mutableListOf<String>().apply {
        add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java")

        // VFORK обходит баг macOS posix_spawn при старте из обновлённого JRE
        add("-Djdk.lang.Process.launchMechanism=VFORK")
        addAll(filteredJvmArguments())

        val jarFile = findMainJar()
        if (jarFile != null) {
            add("-jar")
            add(jarFile)
        } else {
            addMainClassFromCommand()
        }

        addAll(applicationArguments())
    }

    /** Фильтрует IDE-флаги и дебаг-агенты — в production они не нужны. */
    private fun filteredJvmArguments(): List<String> =
        ManagementFactory.getRuntimeMXBean().inputArguments.filterNot { argument ->
            argument.startsWith("-javaagent:") ||
                argument.startsWith("-Xdebug") ||
                argument.startsWith("-Xrunjdwp:") ||
                argument.startsWith("-agentlib:") ||
                argument.startsWith("-Didea.") ||
                argument.startsWith("-Dgradle.") ||
                argument.contains("idea_rt.jar") ||
                argument.contains("idea-boot.jar")
        }

    private fun MutableList<String>.addMainClassFromCommand() {
        val mainClass = System.getProperty("sun.java.command")?.substringBefore(" ").orEmpty()
        if (mainClass.isNotEmpty()) {
            add("-cp")
            add(System.getProperty("java.class.path"))
            add(mainClass)
        } else {
            error("Не удалось определить main-класс или JAR приложения")
        }
    }

    private fun applicationArguments(): List<String> {
        val rawCommand = System.getProperty("sun.java.command").orEmpty()
        val appArguments = rawCommand.substringAfter(" ", "")
        return if (appArguments.isNotEmpty()) appArguments.split(" ") else emptyList()
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
        const val RESTART_DELAY_SECONDS = 3
        const val APP_EXTENSION = ".app"
        const val CONTENTS_DIR = "Contents"
        const val MACOS_DIR = "MacOS"
        const val JAR_EXTENSION = ".jar"
    }
}
