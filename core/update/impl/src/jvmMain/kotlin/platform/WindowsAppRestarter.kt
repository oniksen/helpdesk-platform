package platform

import domain.AppRestarter
import java.io.File
import kotlin.system.exitProcess

/**
 * Windows-реализация [AppRestarter].
 *
 * Запускает `.exe`-лаунчер приложения через [ProcessBuilder] и завершает
 * текущий процесс через [exitProcess].
 *
 * Путь к лаунчеру определяется через [ProcessHandle] (текущий процесс),
 * либо через ожидаемый путь `%LOCALAPPDATA%\com.lpmti.helpdesk\com.lpmti.helpdesk.exe`.
 */
class WindowsAppRestarter : AppRestarter {

    override fun restart() {
        val executable = findLauncherExecutable()

        println("[WindowsAppRestarter] Перезапуск через: $executable")

        ProcessBuilder(executable)
            .directory(File(executable).parentFile)
            .start()

        exitProcess(0)
    }

    /**
     * Определяет путь к исполняемому файлу лаунчера.
     * Приоритет: текущий процесс → стандартная директория установки.
     */
    private fun findLauncherExecutable(): String {
        val currentCommand = ProcessHandle.current()
            .info()
            .command()
            .orElse(null)

        if (currentCommand != null && currentCommand.endsWith(EXE_EXTENSION, ignoreCase = true)) {
            return currentCommand
        }

        val appDir = File(System.getProperty("user.home"))
            .resolve("AppData")
            .resolve("Local")
            .resolve(PACKAGE_NAME)
        val launcher = appDir.resolve("$PACKAGE_NAME$EXE_EXTENSION")

        if (launcher.exists()) {
            return launcher.absolutePath
        }

        error(
            """
            Cannot determine launcher executable.
            Process command: $currentCommand
            Expected launcher: ${launcher.absolutePath}
            """.trimIndent(),
        )
    }

    private companion object {
        const val PACKAGE_NAME = "com.lpmti.helpdesk"
        const val EXE_EXTENSION = ".exe"
    }
}
