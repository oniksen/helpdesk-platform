package newvision

/**
 * Каналы обновлений, которые умеет текущий манифест.
 *
 * Имена канонические: они же используются в [targetFor],
 * в файле каналов (channels.json) и в путях деплоя на сервере.
 */
enum class UpdateChannel(val alias: String) {
    PROD("prod"),
    DEV_CANARY("dev.canary"),
    DEV_RC("dev.rc")
}