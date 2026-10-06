package domain

import domain.models.manifest.v2.Channels
import domain.models.manifest.v2.Dev
import domain.models.manifest.v2.LastRejection
import domain.models.manifest.v2.Release
import domain.models.manifest.v2.TargetData
import domain.models.manifest.v2.UpdateManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ManifestTargetForChannelTest {

    @Test
    fun `resolves web target for known channels`() {
        val manifest = manifest(
            prod = release("1.0.0"),
            canary = release("2.0.0"),
            rc = release("3.0.0"),
        )

        val cases = mapOf(
            UpdateChannel.PROD to "1.0.0",
            UpdateChannel.DEV_CANARY to "2.0.0",
            UpdateChannel.DEV_RC to "3.0.0",
        )

        cases.forEach { (channel, expectedVersion) ->
            val target = manifest.targetFor(channel)
            assertEquals(
                expectedVersion,
                target?.version,
                "Неверная версия таргета для канала '$channel'",
            )
        }
    }

    @Test
    fun `returns null for unknown channel`() {
        val manifest = manifest(prod = release("1.0.0"))

        assertNull(
            manifest.targetFor("qa"),
            "Неизвестный канал не должен иметь таргет",
        )
    }

    @Test
    fun `returns null for unpublished web target`() {
        val manifest = manifest(prod = Release(macos = null, web = null, windows = null))

        assertNull(
            manifest.targetFor(UpdateChannel.PROD),
            "Неопубликованный web-таргет не должен возвращаться",
        )
    }

    private fun release(version: String) = Release(
        macos = null,
        web = target(version),
        windows = null,
    )

    private fun target(version: String) = TargetData(
        version = version,
        patchNote = emptyList(),
        tags = emptyList(),
        date = "2026-10-06T00:00:00",
        link = "https://example.com/$version",
        hash = "hash-$version",
        size = 1L,
    )

    private fun manifest(
        prod: Release = Release(macos = null, web = null, windows = null),
        canary: Release = Release(macos = null, web = null, windows = null),
        rc: Release = Release(macos = null, web = null, windows = null),
    ) = UpdateManifest(
        channels = Channels(
            dev = Dev(canary = canary, rc = rc, lastRejection = LastRejection(date = null, lastVersion = "0.0.0", reason = null)),
            prod = prod,
        ),
        schemaVersion = 4,
    )
}
