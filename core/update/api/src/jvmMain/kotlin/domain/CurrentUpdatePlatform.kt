package domain

/**
 * Платформа текущего процесса по системному свойству `os.name`.
 *
 * @return [UpdatePlatform.MACOS] или [UpdatePlatform.WINDOWS], либо `null`,
 * если ОС не поддерживается — тогда пайплайн обновления не запускается.
 * */
fun currentUpdatePlatform(): UpdatePlatform? {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        osName.contains("mac") -> UpdatePlatform.MACOS
        osName.contains("win") -> UpdatePlatform.WINDOWS
        else -> null
    }
}
