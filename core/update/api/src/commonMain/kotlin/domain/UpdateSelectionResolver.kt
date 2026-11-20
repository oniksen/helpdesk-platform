package domain

import domain.models.manifest.v2.TargetData
import domain.models.manifest.v2.UpdateManifest

/**
 * Разрешение канала обновлений в таргет манифеста для конкретной платформы.
 *
 * Таргет считается опубликованным только при непустом [TargetData.hash]
 * и положительном [TargetData.size] — это проверяет маппер манифеста.
 *
 * @param channel канонический канал из [UpdateChannel].
 * @param platform платформа, под которую нужна сборка.
 * @return таргет канала или `null`, если канал неизвестен манифесту
 * или для платформы не опубликована сборка.
 * */
fun UpdateManifest.targetFor(
    channel: String,
    platform: UpdatePlatform,
): TargetData? {
    val release = when (channel) {
        UpdateChannel.PROD -> channels.prod
        UpdateChannel.DEV_CANARY -> channels.dev.canary
        UpdateChannel.DEV_RC -> channels.dev.rc
        else -> return null
    }

    return when (platform) {
        UpdatePlatform.MACOS -> release.macos
        UpdatePlatform.WINDOWS -> release.windows
        UpdatePlatform.WEB -> release.web
    }
}

/**
 * Таргет канала для web-сборки.
 *
 * Перегрузка для гейта (webShell) и прочих web-потребителей манифеста.
 * */
fun UpdateManifest.targetFor(channel: String): TargetData? =
    targetFor(channel, UpdatePlatform.WEB)
