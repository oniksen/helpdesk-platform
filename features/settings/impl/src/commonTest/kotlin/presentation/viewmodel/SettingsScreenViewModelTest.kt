package presentation.viewmodel

import AppVersion
import AppVersionProvider
import CurrentChannelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fills current channel from provider`() = runTest {
        val viewModel = createViewModel(channel = "dev.canary")

        assertEquals(
            "dev.canary",
            viewModel.uiState.value.currentChannel,
            "Код канала должен попасть в состояние как есть",
        )
    }

    @Test
    fun `keeps channel null when provider returns nothing`() = runTest {
        val viewModel = createViewModel(channel = null)

        assertNull(
            viewModel.uiState.value.currentChannel,
            "Без сохранённого канала состояние должно остаться пустым",
        )
    }

    private fun createViewModel(channel: String?) = SettingsScreenViewModel(
        appVersionProvider = FakeAppVersionProvider(),
        currentChannelProvider = FakeCurrentChannelProvider(channel),
    )

    private class FakeAppVersionProvider : AppVersionProvider {
        override fun provide(): AppVersion = AppVersion(0, 3, 0, AppVersion.Stage.Alpha, 4)
    }

    private class FakeCurrentChannelProvider(private val channel: String?) : CurrentChannelProvider {
        override fun provide(): String? = channel
    }
}
