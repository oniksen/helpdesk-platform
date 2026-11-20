package domain.model

import domain.UpdatePlatform
import domain.models.manifest.v2.TargetData
import domain.models.manifest.v2.UpdateManifest
import domain.targetFor

/**
 * Типизированный резолв таргета манифеста по каналу и платформе.
 *
 * Делегирует строковому `targetFor`, сохраняя единую реализацию выбора
 * ветки манифеста.
 */
fun UpdateManifest.targetFor(
    channel: UpdateChannel,
    platform: UpdatePlatform,
): TargetData? = targetFor(channel.alias, platform)
