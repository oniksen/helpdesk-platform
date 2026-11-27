package data.storage

import data.storage.JvmAuthStateStorage.Companion.APP_NAME
import java.io.File

actual fun createAuthStateStorage(): AuthStateStorage = JvmAuthStateStorage()

internal class JvmAuthStateStorage(
    private val file: File = File(getAppDataDirectory(), AUTH_STORAGE_FILE_NAME),
) : AuthStateStorage {
    /**
     * Записать параметры в файл настроек.
     *
     * При повторном вызове новые параметры перепишут старые: файл целиком
     * заменяется переданным значением.
     * */
    override fun save(stateJson: String) = file.writeText(stateJson)

    override fun load(): String? = if (file.exists()) file.readText() else null

    override fun clear() { file.delete() }

    internal companion object {
        const val AUTH_STORAGE_FILE_NAME = "auth_storage.json"
        const val APP_NAME = "com.lpmti.helpdesk"
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

    val appDir =  File(baseUrl, appName)
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    return appDir
}
