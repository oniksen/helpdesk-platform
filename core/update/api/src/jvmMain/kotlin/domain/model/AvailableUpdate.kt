package domain.model

import AppVersion

/**
 * Данные, необходимые для установки и показа обновления.
 *
 * @property version Типизированная версия обновления.
 * @property channel Канал, из которого пришло обновление.
 * @property url Ссылка на zip-архив.
 * @property hash SHA-256 архива; обязателен для пайплайна.
 * @property size Размер архива в байтах; обязателен для пайплайна.
 * @property patchNote Список изменений для будущего UI.
 */
data class AvailableUpdate(
    val version: AppVersion,
    val channel: UpdateChannel,
    val url: String,
    val hash: String,
    val size: Long,
    val patchNote: List<String> = emptyList(),
)
