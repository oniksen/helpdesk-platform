package domain

import domain.models.manifest.v2.Channels
import domain.models.manifest.v2.Dev
import domain.models.manifest.v2.LastRejection
import domain.models.manifest.v2.Release
import domain.models.manifest.v2.TargetData
import domain.models.manifest.v2.UpdateManifest

/**
 * Билдеры доменных моделей манифеста v4 для тестов.
 *
 * Отсутствующая сборка платформы моделируется `null` — именно так маппер
 * отдаёт неопубликованный таргет.
 */
object ManifestFixtures {

    /** Релиз без опубликованных сборок. */
    val EMPTY_RELEASE = Release(macos = null, web = null, windows = null)

    fun target(version: String) = TargetData(
        version = version,
        patchNote = emptyList(),
        tags = emptyList(),
        date = DATE,
        link = "https://example.com/$version",
        hash = "hash-$version",
        size = 1L,
    )

    fun release(
        macos: String? = null,
        windows: String? = null,
        web: String? = null,
    ) = releaseWith(
        macos = macos?.let(::target),
        web = web?.let(::target),
        windows = windows?.let(::target),
    )

    /** Релиз с готовыми таргетами (нужен для проверки hash/size в пайплайне). */
    fun releaseWith(
        macos: TargetData? = null,
        windows: TargetData? = null,
        web: TargetData? = null,
    ) = Release(
        macos = macos,
        web = web,
        windows = windows,
    )

    fun manifest(
        prod: Release = EMPTY_RELEASE,
        canary: Release = EMPTY_RELEASE,
        rc: Release = EMPTY_RELEASE,
    ) = UpdateManifest(
        channels = Channels(
            dev = Dev(
                canary = canary,
                rc = rc,
                lastRejection = LastRejection(date = null, lastVersion = "0.0.0", reason = null),
            ),
            prod = prod,
        ),
        schemaVersion = 4,
    )

    private const val DATE = "2026-10-06T00:00:00"
}
