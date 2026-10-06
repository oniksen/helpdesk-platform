package domain

import domain.models.manifest.v2.TargetData
import domain.models.manifest.v2.UpdateManifest

/**
 * Разрешение канала обновлений в web-таргет манифеста.
 *
 * @return таргет канала или null, если канал неизвестен манифесту
 * или для него не опубликована web-сборка.
 */
fun UpdateManifest.targetFor(channel: String): TargetData? {
    val release = when (channel) {
        UpdateChannel.PROD -> channels.prod
        UpdateChannel.DEV_CANARY -> channels.dev.canary
        UpdateChannel.DEV_RC -> channels.dev.rc
        else -> return null
    }

    return release.web
}
