package domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Тесты выбора таргета манифеста по каналу и платформе.
 */
class ManifestTargetForChannelTest {

    @Test
    fun `resolves web target for known channels`() {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(web = "1.0.0"),
            canary = ManifestFixtures.release(web = "2.0.0"),
            rc = ManifestFixtures.release(web = "3.0.0"),
        )

        val cases = mapOf(
            UpdateChannel.PROD to "1.0.0",
            UpdateChannel.DEV_CANARY to "2.0.0",
            UpdateChannel.DEV_RC to "3.0.0",
        )

        cases.forEach { (channel, expectedVersion) ->
            assertEquals(
                expectedVersion,
                manifest.targetFor(channel)?.version,
                "Неверный web-таргет для канала '$channel'",
            )
        }
    }

    @Test
    fun `resolves desktop targets for every channel`() {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(macos = "1.1.1", windows = "1.1.2"),
            canary = ManifestFixtures.release(macos = "2.1.1", windows = "2.1.2"),
            rc = ManifestFixtures.release(macos = "3.1.1", windows = "3.1.2"),
        )

        val cases = listOf(
            Triple(UpdateChannel.PROD, UpdatePlatform.MACOS, "1.1.1"),
            Triple(UpdateChannel.PROD, UpdatePlatform.WINDOWS, "1.1.2"),
            Triple(UpdateChannel.DEV_CANARY, UpdatePlatform.MACOS, "2.1.1"),
            Triple(UpdateChannel.DEV_CANARY, UpdatePlatform.WINDOWS, "2.1.2"),
            Triple(UpdateChannel.DEV_RC, UpdatePlatform.MACOS, "3.1.1"),
            Triple(UpdateChannel.DEV_RC, UpdatePlatform.WINDOWS, "3.1.2"),
        )

        cases.forEach { (channel, platform, expectedVersion) ->
            assertEquals(
                expectedVersion,
                manifest.targetFor(channel, platform)?.version,
                "Неверный таргет для канала '$channel' и платформы $platform",
            )
        }
    }

    @Test
    fun `returns null for unknown channel`() {
        val manifest = ManifestFixtures.manifest(prod = ManifestFixtures.release(web = "1.0.0"))

        assertNull(
            manifest.targetFor("qa"),
            "Неизвестный канал не должен давать web-таргет",
        )
    }

    @Test
    fun `returns null for unknown channel in desktop lookup`() {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(macos = "1.1.1", windows = "1.1.2"),
        )

        listOf("qa", "", UpdateChannel.PROD.uppercase()).forEach { channel ->
            assertNull(
                manifest.targetFor(channel, UpdatePlatform.MACOS),
                "Канал '$channel' не должен давать desktop-таргет",
            )
        }
    }

    @Test
    fun `returns null for unpublished web target`() {
        val manifest = ManifestFixtures.manifest(prod = ManifestFixtures.EMPTY_RELEASE)

        assertNull(
            manifest.targetFor(UpdateChannel.PROD),
            "Неопубликованный web-таргет не должен выбираться",
        )
    }

    @Test
    fun `returns null for unpublished desktop target`() {
        val manifest = ManifestFixtures.manifest(
            prod = ManifestFixtures.release(windows = "1.1.2"),
        )

        val cases = listOf(
            UpdatePlatform.MACOS to null,
            UpdatePlatform.WINDOWS to "1.1.2",
        )

        cases.forEach { (platform, expectedVersion) ->
            assertEquals(
                expectedVersion,
                manifest.targetFor(UpdateChannel.PROD, platform)?.version,
                "Неверный таргет для платформы $platform при частично опубликованном релизе",
            )
        }
    }
}
