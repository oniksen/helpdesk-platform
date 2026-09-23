package presentation.viewmodel

import syncBuildToServiceWorker

internal actual suspend fun platformPushBuild(url: String, files: Map<String, ByteArray>): Boolean =
    syncBuildToServiceWorker(url, files)
