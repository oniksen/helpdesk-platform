package presentation.screen

import AppNavigator
import NetworkResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import data.repository.TasksRepository
import dev.mokkery.answering.calls
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import domain.model.Pagination
import domain.model.Statuses
import domain.model.TaskData
import domain.model.TasksPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import presentation.screen.compact.TASKS_LIST_TEST_TAG
import presentation.viewmodel.TasksPageViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Сквозной тест пагинации: реальный экран, реальная [TasksPageViewModel] и замоканный
 * [TasksRepository]. Проверяет, что скролл к концу первой страницы заставляет ViewModel
 * запросить вторую, а её данные доезжают до UI.
 */
class TasksPaginationUiTest : KoinTest {

    private lateinit var requested: MutableStateFlow<List<Int>>
    private lateinit var mockRepository: TasksRepository
    private lateinit var viewModel: TasksPageViewModel

    /** Номера запрошенных страниц в порядке вызовов. */
    private val requestedPages: List<Int>
        get() = requested.value

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        // BaseViewModel живёт на Dispatchers.Main; без подмены первая страница
        // в Compose UI-тесте не загрузится. Unconfined запускает init сразу,
        // а виртуальное время не даёт сработать повторному опросу раз в 5 секунд.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        requested = MutableStateFlow(emptyList())
        mockRepository = mock<TasksRepository> {
            everySuspend { loadPage(any(), any(), any(), any()) } calls { args ->
                val page = args.arg<Int>(0)
                requested.update { it + page }
                pageResult(page)
            }
        }
        startKoin {
            modules(
                module {
                    single<TasksRepository> { mockRepository }
                    single<AppNavigator> { NoopAppNavigator }
                    factory { TasksPageViewModel(get(), get()) }
                }
            )
        }
        viewModel = get()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `requests the second page when scrolling to the end on a small phone screen`(): TestResult =
        runPaginationScenario(SMALL_PHONE)

    @Test
    fun `requests the second page when scrolling to the end on a compact phone screen`(): TestResult =
        runPaginationScenario(COMPACT_PHONE)

    @Test
    fun `requests the second page when scrolling to the end on a tall phone screen`(): TestResult =
        runPaginationScenario(TALL_PHONE)

    @Test
    fun `requests the second page when scrolling to the end on a tablet screen`(): TestResult =
        runPaginationScenario(TABLET_PORTRAIT)

    @OptIn(ExperimentalTestApi::class)
    private fun runPaginationScenario(screenSize: DpSize): TestResult = runComposeUiTest {
        setContent {
            Box(Modifier.size(screenSize)) {
                TasksPage(tasksPageViewModel = viewModel)
            }
        }

        // 1. ViewModel загрузила первую страницу (задачи #0..#9).
        waitUntilAtLeastOneExists(hasText(title(0)), timeoutMillis = 3_000L)
        assertEquals(
            listOf(1),
            requestedPages,
            "Первая страница должна запроситься ровно один раз при разрешении $screenSize",
        )

        // 2. Скролл к концу первой страницы: UI отдаёт firstVisibleItemIndex,
        //    ViewModel обязана запросить страницу 2. performScrollToNode скроллит
        //    постепенно, как реальный пользователь, а не прыжком к индексу.
        onNodeWithTag(TASKS_LIST_TEST_TAG)
            .performScrollToNode(hasText(title(9)))

        // Фиксируем запросы до второго скролла: дальнейший проход по 20 элементам
        // способен спровоцировать запрос следующей страницы и зашуметь проверку.
        waitUntil(
            conditionDescription = "Страница 2 не запрошена при $screenSize: " +
                "запрошено $requestedPages. Скролл к концу первой страницы " +
                "не преодолел порог emitNewPage (firstVisibleItemIndex < 3)",
            timeoutMillis = 3_000L,
        ) { requestedPages.contains(2) }
        assertEquals(
            listOf(1, 2),
            requestedPages,
            "Пагинация должна запросить ровно страницы 1 и 2 при разрешении $screenSize",
        )

        // 3. Данные второй страницы доехали до UI и отрисовались.
        waitUntil(
            conditionDescription = "Вторая страница не попала в состояние при $screenSize",
            timeoutMillis = 3_000L,
        ) { viewModel.uiState.value.currentList.size >= 20 }
        waitForIdle()
        onNodeWithTag(TASKS_LIST_TEST_TAG)
            .performScrollToNode(hasText(title(15)))
        onNodeWithText(title(15)).assertIsDisplayed()
    }

    /** Эмуляция ответа сервера: pageSize совпадает с контрактом репозитория. */
    private fun pageResult(page: Int): NetworkResult<TasksPage> {
        val firstId = (page - 1) * PAGE_SIZE
        return NetworkResult.Success(
            TasksPage(
                data = List(PAGE_SIZE) { offset -> task(id = firstId + offset) },
                pagination = Pagination(
                    hasNext = page < LAST_PAGE,
                    hasPrevious = page > 1,
                    page = page,
                    pageSize = PAGE_SIZE,
                    totalItems = LAST_PAGE * PAGE_SIZE,
                    totalPages = LAST_PAGE,
                ),
            )
        )
    }

    private fun task(id: Int): TaskData = TaskData(
        building = null,
        category = "",
        commercial = false,
        contacts = emptyList(),
        dateCreate = null,
        description = "",
        id = id,
        initiator = "",
        reopenCount = 0,
        serviceId = "",
        status = null,
        statuses = Statuses(emptyList(), emptyList(), emptyList()),
        title = title(id),
    )

    private object NoopAppNavigator : AppNavigator {
        override fun navigate(route: NavKey) = Unit
        override fun popBackStack() = Unit
    }

    private companion object {
        const val PAGE_SIZE = 10
        const val LAST_PAGE = 2

        fun title(id: Int): String = "Задача #$id"

        /** Каждое разрешение — отдельный тест: на JS один @Test должен вернуть ровно один TestResult. */
        // Контроль: на коротком экране список прокручивается далеко, индекс >= 3 и триггер срабатывает.
        val SMALL_PHONE = DpSize(320.dp, 420.dp)
        val COMPACT_PHONE = DpSize(360.dp, 640.dp)
        val TALL_PHONE = DpSize(411.dp, 731.dp)
        val TABLET_PORTRAIT = DpSize(768.dp, 720.dp)
    }
}
