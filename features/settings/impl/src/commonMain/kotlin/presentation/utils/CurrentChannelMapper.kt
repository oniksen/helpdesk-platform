package presentation.utils

import androidx.compose.runtime.Composable
import domain.UpdateChannel
import helpdesk_platform.features.settings.impl.generated.resources.Res
import helpdesk_platform.features.settings.impl.generated.resources.settings_channel_canary
import helpdesk_platform.features.settings.impl.generated.resources.settings_channel_prod
import helpdesk_platform.features.settings.impl.generated.resources.settings_channel_rc
import helpdesk_platform.features.settings.impl.generated.resources.settings_undefined
import org.jetbrains.compose.resources.stringResource

/**
 * Человекочитаемое имя канала вида «Canary (dev.canary)».
 *
 * Отсутствующий канал — ошибка чтения, неизвестный код показывается как есть.
 */
@Composable
internal fun channelDisplayName(code: String?): String = when (code) {
    null -> stringResource(resource = Res.string.settings_undefined)
    UpdateChannel.DEV_CANARY -> stringResource(resource = Res.string.settings_channel_canary)
    UpdateChannel.DEV_RC -> stringResource(resource = Res.string.settings_channel_rc)
    UpdateChannel.PROD -> stringResource(resource = Res.string.settings_channel_prod)
    else -> code
}
