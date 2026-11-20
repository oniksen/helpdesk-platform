package domain

/**
 * Платформа, под которую выбирается таргет манифеста обновлений.
 *
 * Имена совпадают с полями [domain.models.manifest.v2.Release]:
 * у каждой ветки обновлений свой набор платформенных сборок.
 * */
enum class UpdatePlatform {
    MACOS,
    WINDOWS,
    WEB,
}
