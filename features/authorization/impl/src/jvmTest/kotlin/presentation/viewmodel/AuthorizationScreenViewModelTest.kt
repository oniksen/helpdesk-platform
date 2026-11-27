package presentation.viewmodel

import AppNavigator
import AuthResponse
import Authorization
import androidx.navigation3.runtime.NavKey
import auth.AuthData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import login.LoginCredentials
import login.LoginMode
import login.LoginStrategy
import navigation.TasksPageRoute
import user.AccessLevel
import user.UserData
import user.UserRole
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthorizationScreenViewModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = UnconfinedTestDispatcher(scheduler)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `keeps login form hidden until navigation after restore`() = TestScope(scheduler).runTest {
        try {
            val navigator = FakeNavigator()
            val viewModel = AuthorizationScreenViewModel(
                navigator = navigator,
                authorizer = FakeAuthorization(restoreResult = true),
                loginStrategy = FakeLoginStrategy(),
            )

            advanceTimeBy(999)
            runCurrent()

            assertEquals("Сессия восстановлена", viewModel.state.value.authResultMessage)
            assertTrue(
                viewModel.state.value.inProgress,
                "Форма входа не должна появляться до перехода на задачи",
            )
            assertEquals(0, navigator.routes.size, "Переход на задачи ещё не должен быть выполнен")

            advanceTimeBy(1)
            runCurrent()

            assertEquals(1, navigator.routes.size, "Экран задач должен открыться после задержки")
            assertEquals(TasksPageRoute::class, navigator.routes.single()::class)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `shows login form when restore does not finish`() = TestScope(scheduler).runTest {
        try {
            val navigator = FakeNavigator()
            val viewModel = AuthorizationScreenViewModel(
                navigator = navigator,
                authorizer = FakeAuthorization(hangOnRestore = true),
                loginStrategy = FakeLoginStrategy(),
            )

            assertTrue(viewModel.state.value.inProgress, "Пока идёт восстановление, должна быть загрузка")

            advanceTimeBy(10_000)
            runCurrent()

            assertFalse(viewModel.state.value.inProgress, "После таймаута должна появиться форма входа")
            assertEquals(0, navigator.routes.size, "При неудачном восстановлении переход не нужен")
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeAuthorization(
        private val restoreResult: Boolean = true,
        private val hangOnRestore: Boolean = false,
    ) : Authorization {
        override suspend fun helpdeskAuth(maxInitData: String): AuthResponse = error("Не используется в тестах")

        override suspend fun helpdeskAuth(email: String, password: String): AuthResponse = error("Не используется в тестах")

        override suspend fun fetchUserData(email: String): UserData = userData

        override fun saveUser(userData: UserData) = Unit

        override fun saveAuthData(authData: AuthData) = Unit

        override fun saveAutoLogin(value: Boolean) = Unit

        override fun getToken(): String = "token-1"

        override fun getUserData(): UserData = userData

        override fun getAutoLogin(): Boolean = true

        override fun persistAuthState() = Unit

        override suspend fun restoreAuthState(): Boolean {
            if (hangOnRestore) delay(Long.MAX_VALUE)
            return restoreResult
        }

        override fun clearAuthState() = Unit
    }

    private class FakeLoginStrategy : LoginStrategy {
        override val mode: LoginMode = LoginMode.Credentials

        override suspend fun authorize(credentials: LoginCredentials?): AuthResponse = error("Не используется в тестах")
    }

    private class FakeNavigator : AppNavigator {
        val routes = mutableListOf<NavKey>()

        override fun navigate(route: NavKey) = Unit

        override fun popBackStack() = Unit

        override fun navigateAndClearBackStack(route: NavKey) {
            routes += route
        }
    }

    private companion object {
        val userData = UserData(
            id = 42,
            name = "Иван",
            lastName = null,
            patronymic = null,
            email = "user@example.com",
            accessLevel = AccessLevel.EMPLOYEE,
            role = UserRole.NONE,
            buildings = emptyList(),
            services = emptyList(),
        )
    }
}
