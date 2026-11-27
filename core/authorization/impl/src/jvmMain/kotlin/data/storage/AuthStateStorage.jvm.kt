package data.storage

import data.storage.JvmAuthStateStorage.Companion.APP_NAME
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission

actual fun createAuthStateStorage(): AuthStateStorage = JvmAuthStateStorage()

internal class JvmAuthStateStorage(
    private val file: File = File(getAppDataDirectory(), AUTH_STORAGE_FILE_NAME),
) : AuthStateStorage {
    /**
     * Записать параметры в файл настроек.
     *
     * При повторном вызове новые параметры перепишут старые: файл целиком
     * заменяется переданным значением. Запись идёт через временный файл,
     * чтобы сбой не оставил повреждённый файл сессии.
     * */
    override fun save(stateJson: String) {
        val directory = file.parentFile.toPath()
        Files.createDirectories(directory)

        val temp = Files.createTempFile(directory, "auth-state-", ".tmp")
        try {
            Files.writeString(temp, stateJson, Charsets.UTF_8)
            restrictPermissions(temp)
            try {
                Files.move(
                    temp,
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temp, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    override fun load(): String? = try {
        if (file.exists()) Files.readString(file.toPath(), Charsets.UTF_8) else null
    } catch (_: IOException) {
        null
    }

    override fun clear() {
        if (file.exists() && !file.delete()) {
            error("Не удалось удалить файл сессии: ${file.absolutePath}")
        }
    }

    companion object {
        const val AUTH_STORAGE_FILE_NAME = "auth_storage.json"
        const val APP_NAME = "com.lpmti.helpdesk"
    }
}

/** Ограничить доступ к файлу только владельцем на системах с POSIX-правами. */
private fun restrictPermissions(path: Path) {
    runCatching {
        Files.setPosixFilePermissions(
            path,
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
        )
    }
}

/**
 * Получить корневую папку приложения.
 *
 * Для Windows вернёт *AppData/Local* с fallback на *Roaming*.
 * Для MacOS вернёт *~/Application Support*.
 * Для Linux и Unix-систем вернёт путь по *XDG_CONFIG_HOME* с fallback на *.config*.
 *
 * Если папки для приложения не будет при обращении, то она создастся автоматически.
 * */
private fun getAppDataDirectory(appName: String = APP_NAME): File {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    val baseUrl = when {
        os.contains("win") -> {
            // Вернём AppData\Local с fallback на AppData/Roaming
            System.getenv("LOCALAPPDATA")
                ?.let { path -> File(path) }
                ?: File(userHome, "AppData/Roaming")
        }
        os.contains("mac") -> {
            File(userHome, "Library/Application Support")
        }
        else -> {
            // Linux или Unix
            System.getenv("XDG_CONFIG_HOME")
                ?.let { path -> File(path) }
                ?: File(userHome, ".config")
        }
    }

    val appDir = File(baseUrl, appName)
    if (!appDir.exists() && !appDir.mkdirs()) {
        error("Не удалось создать каталог приложения: ${appDir.absolutePath}")
    }
    return appDir
}
