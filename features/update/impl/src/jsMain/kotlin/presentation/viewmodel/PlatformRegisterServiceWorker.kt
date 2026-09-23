package presentation.viewmodel

import registerServiceWorker

actual suspend fun platformRegisterServiceWorker(): Boolean =
    registerServiceWorker()
