package presentation.viewmodel

internal actual suspend fun platformPushBuild(url: String, files: Map<String, ByteArray>): Boolean = true
