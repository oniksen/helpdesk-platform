package domain

/**
 * Каналы обновлений, которые умеет текущий манифест.
 *
 * Имена канонические: они же используются в [targetFor],
 * в файле каналов (channels.json) и в путях деплоя на сервере.
 */
object UpdateChannel {
    const val PROD = "prod"
    const val DEV_CANARY = "dev.canary"
    const val DEV_RC = "dev.rc"
}
