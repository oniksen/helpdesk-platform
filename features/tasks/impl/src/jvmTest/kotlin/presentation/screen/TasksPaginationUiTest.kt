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
 * Сквозной тест загрузки следующей страницы: реальный экран, реальная [TasksPageViewModel]
 * и замоканный [TasksRepository]. Проверяет, что после достижения порога подгружается
 * следующая страница и её задачи появляются в списке. На маленьких экранах для этого
 * нужен явный скролл; на больших стартовая композиция вправе загрузить сразу несколько
 * страниц, поэтому количество запрошенных страниц жёстко не фиксируется.
 *
 * Тест лежит в jvmTest: на JavaScript-таргете Compose UI-тесты требуют
 * инициализации WASM-рантайма Skiko, который в браузерном тестовом окружении
 * не поднимается. Проверяется при этом общий код из commonMain.
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
        mockRepository = mock {
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
    fun `loads the next page on a small phone screen`(): TestResult =
        runPaginationScenario(SMALL_PHONE)

    @Test
    fun `loads the next page on a compact phone screen`(): TestResult =
        runPaginationScenario(COMPACT_PHONE)

    @Test
    fun `loads the next page on a tall phone screen`(): TestResult =
        runPaginationScenario(TALL_PHONE)

    @Test
    fun `loads the next page on a tablet screen`(): TestResult =
        runPaginationScenario(TABLET_PORTRAIT)

    @OptIn(ExperimentalTestApi::class)
    private fun runPaginationScenario(screenSize: DpSize): TestResult = runComposeUiTest {
        setContent {
            Box(Modifier.size(screenSize)) {
                TasksPage(tasksPageViewModel = viewModel)
            }
        }

        // 1. Первая страница отрисовалась. На большом экране к этому моменту уже
        //    могло загрузиться несколько страниц — это допустимо и ниже не отвергается.
        waitUntilAtLeastOneExists(hasText(title(0)), timeoutMillis = 3_000L)
        assertEquals(
            1,
            requestedPages.firstOrNull(),
            "Первой должна загружаться страница 1 при разрешении $screenSize",
        )

        // 2. Явный скролл до «последний элемент первой страницы минус порог».
        //    На маленьких экранах это необходимое предусловие подгрузки; на больших
        //    скролл может быть избыточным, потому что порог уже пройден при старте.
        onNodeWithTag(TASKS_LIST_TEST_TAG)
            .performScrollToNode(hasText(title(SCROLL_TARGET_INDEX)))

        // 3. Следующая страница загружена. Страницы идут подряд от 1 без пропусков
        //    и повторов; сколько их будет на большом экране — заранее не фиксируем.
        waitUntil(
            conditionDescription = "Следующая страница не загрузилась при $screenSize: " +
                "запрошено $requestedPages",
            timeoutMillis = 3_000L,
        ) { requestedPages.contains(2) }
        assertEquals(
            (1..requestedPages.max()).toList(),
            requestedPages,
            "Страницы должны запрашиваться подряд от 1 без пропусков и повторов при разрешении $screenSize",
        )

        // 4. Данные второй страницы доехали до UI и отрисовались.
        waitUntil(
            conditionDescription = "Задачи второй страницы не попали в состояние при $screenSize",
            timeoutMillis = 3_000L,
        ) { viewModel.uiState.value.currentList.size >= 20 }
        waitForIdle()
        onNodeWithTag(TASKS_LIST_TEST_TAG)
            .performScrollToNode(hasText(title(SECOND_PAGE_ITEM_INDEX)))
        onNodeWithText(title(SECOND_PAGE_ITEM_INDEX)).assertIsDisplayed()
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
        override fun navigateAndClearBackStack(route: NavKey) = Unit
    }

    private companion object {
        const val PAGE_SIZE = TasksRepository.DEFAULT_PAGE_SIZE
        const val LAST_PAGE = 2

        /** Порог из TasksPageViewModel: подгрузка начинается за 3 элемента до конца страницы. */
        const val PAGE_THRESHOLD = 3

        /** Последний элемент первой страницы минус порог: 9 - 3 = 6. */
        const val SCROLL_TARGET_INDEX = PAGE_SIZE - 1 - PAGE_THRESHOLD

        /** Элемент второй страницы, по которому проверяем, что данные доехали до UI. */
        const val SECOND_PAGE_ITEM_INDEX = PAGE_SIZE + 5

        fun title(id: Int): String = "Задача #$id"

        /** Каждое разрешение — отдельный тест: на JS один @Test должен вернуть ровно один TestResult. */
        val SMALL_PHONE = DpSize(320.dp, 420.dp)
        val COMPACT_PHONE = DpSize(360.dp, 640.dp)
        val TALL_PHONE = DpSize(411.dp, 731.dp)
        val TABLET_PORTRAIT = DpSize(768.dp, 720.dp)
    }
}
