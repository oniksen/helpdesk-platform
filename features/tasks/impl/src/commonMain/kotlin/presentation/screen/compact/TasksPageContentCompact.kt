package presentation.screen.compact

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import domain.model.Tabs
import domain.model.TaskModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import presentation.effect.TasksPageEffect
import presentation.screen.LocalTasksPageActions
import presentation.screen.shared.AnimatedTabBar
import presentation.state.TasksPageState

private enum class ListPosition {
    First, Middle, Last
}

@Composable
internal fun TasksPageContentCompact(
    state: TasksPageState,
    effect: SharedFlow<TasksPageEffect>,
) {
    val actions = LocalTasksPageActions.current

    val pagerState = rememberPagerState(pageCount = { Tabs.entries.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { TasksTopBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Кастомный контейнер для вкладок
            AnimatedTabBar(
                modifier = Modifier
                    .padding(innerPadding),
                tabs = Tabs.entries,
                pagerState = pagerState,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            // Контент под вкладками, синхронизированный с Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when(Tabs.entries[page]) {
                        Tabs.Current -> TasksList(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            list = state.currentList,
                            hasNextPage = state.hasNextPage,
                            onFirstVisibleIndexChange = { index -> actions.changeFirstVisibleIndex(index) }
                        )
                        else -> {
                            Text(
                                text = "В разработке",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = TextUnit(24f, TextUnitType.Sp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Задачи",
            )
        }
    )
}

@Composable
private fun TasksList(
    modifier: Modifier = Modifier,
    list: List<TaskModel>,
    hasNextPage: Boolean,
    onFirstVisibleIndexChange: (Int) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        onFirstVisibleIndexChange(listState.firstVisibleItemIndex)
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        state = listState,
    ) {
        itemsIndexed(items = list) { index, taskModel  ->
            TaskItemCard(
                data = taskModel,
                listPosition = when (index) {
                    0 -> ListPosition.First
                    list.lastIndex -> ListPosition.Last
                    else -> ListPosition.Middle
                }
            )
        }
        if (hasNextPage) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = "Конец списка",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    data: TaskModel,
    listPosition: ListPosition,
) {
    val cardShape = when (listPosition) {
        ListPosition.First -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        ListPosition.Last -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        else -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = data.id.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}