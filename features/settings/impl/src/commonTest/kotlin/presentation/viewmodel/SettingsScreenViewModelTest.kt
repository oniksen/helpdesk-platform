package presentation.viewmodel

import AppNavigator
import AppVersion
import AppVersionProvider
import AuthResponse
import AuthSession
import AuthorizationScreenRoute
import CurrentChannelProvider
import androidx.navigation3.runtime.NavKey
import auth.AuthData
import domain.intent.SettingsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import login.LoginCredentials
import login.LoginMode
import login.LoginStrategy
import user.UserData
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
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

    @Test
    fun `offers logout for credentials mode`() = runTest {
        val viewModel = createViewModel(mode = LoginMode.Credentials)

        assertEquals(
            true,
            viewModel.uiState.value.exitButtonAvailable,
            "При парольном входе кнопка выхода должна быть доступна",
        )
    }

    @Test
    fun `does not offer logout for silent mode`() = runTest {
        val viewModel = createViewModel(mode = LoginMode.Silent)

        assertEquals(
            false,
            viewModel.uiState.value.exitButtonAvailable,
            "При входе через MAX кнопка выхода не должна показываться",
        )
    }

    @Test
    fun `logout clears saved session and opens authorization screen`() = runTest {
        val navigator = FakeNavigator()
        val session = FakeAuthSession()
        val viewModel = createViewModel(
            mode = LoginMode.Credentials,
            navigator = navigator,
            session = session,
        )

        viewModel.sendIntent(SettingsIntent.Logout)

        assertEquals(1, session.clearCalls, "Сессия должна удаляться из хранилища")
        assertEquals(1, navigator.routes.size, "Должен быть выполнен переход на один экран")
        assertEquals(
            AuthorizationScreenRoute::class,
            navigator.routes.single()::class,
            "После выхода должен открываться экран авторизации",
        )
    }

    private fun createViewModel(
        channel: String? = null,
        mode: LoginMode = LoginMode.Credentials,
        navigator: AppNavigator = FakeNavigator(),
        session: AuthSession = FakeAuthSession(),
    ) = SettingsScreenViewModel(
        appVersionProvider = FakeAppVersionProvider(),
        currentChannelProvider = FakeCurrentChannelProvider(channel),
        navigator = navigator,
        loginStrategy = FakeLoginStrategy(mode),
        authSession = session,
    )

    private class FakeAppVersionProvider : AppVersionProvider {
        override fun provide(): AppVersion = AppVersion(0, 3, 0, AppVersion.Stage.Alpha, 4)
    }

    private class FakeCurrentChannelProvider(private val channel: String?) : CurrentChannelProvider {
        override fun provide(): String? = channel
    }

    private class FakeLoginStrategy(override val mode: LoginMode) : LoginStrategy {
        override suspend fun authorize(credentials: LoginCredentials?): AuthResponse = error("Не используется в тестах")
    }

    private class FakeAuthSession : AuthSession {
        var clearCalls = 0

        override fun saveUser(userData: UserData) = Unit

        override fun saveAuthData(authData: AuthData) = Unit

        override fun saveAutoLogin(value: Boolean) = Unit

        override fun getAutoLogin(): Boolean = false

        override fun persistAuthState() = Unit

        override suspend fun restoreAuthState(): Boolean = true

        override fun clearAuthState() {
            clearCalls++
        }
    }

    private class FakeNavigator : AppNavigator {
        val routes = mutableListOf<NavKey>()

        override fun navigate(route: NavKey) = Unit

        override fun popBackStack() = Unit

        override fun navigateAndClearBackStack(route: NavKey) {
            routes += route
        }
    }
}
