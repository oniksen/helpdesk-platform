package presentation.viewmodel

import AppNavigator

/**
 * Завершение установки обновления с передачей управления навигатору.
 * JS: отмечает установку и перезагружает страницу (с guard на число попыток).
 * JVM: навигация в главное меню.
 * */
internal expect fun platformFinishInstall(navigator: AppNavigator): FinishInstallResult
