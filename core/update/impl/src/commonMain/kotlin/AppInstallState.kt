/**
 * Модуль хранения флага установленной сборки приложения.
 * */
expect fun isAppInstalled(): Boolean

expect fun markAppInstalled()

/**
 * Контролирует ли Service Worker текущую страницу.
 * */
expect fun isServiceWorkerControlled(): Boolean

/**
 * Увеличивает счётчик попыток перезагрузки после установки и возвращает новое значение.
 * */
expect fun incrementInstallReloadAttempts(): Int

/**
 * Сбрасывает счётчик попыток перезагрузки (успешная установка).
 * */
expect fun resetInstallReloadAttempts()
