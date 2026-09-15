package presentation.screen.compact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.model.TaskModel
import kotlinx.coroutines.flow.SharedFlow
import presentation.effect.TasksPageEffect
import presentation.screen.LocalTasksPageActions
import presentation.state.TasksPageState

@Composable
internal fun TasksPageContentCompact(
    state: TasksPageState,
    effect: SharedFlow<TasksPageEffect>,
) {
    val actions = LocalTasksPageActions.current


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TasksTopBar()
                }
            )
        }
    ) { innerPadding ->
        TasksList(
            modifier = Modifier.padding(innerPadding),
            list = state.currentList,
        )
    }
}

@Composable
private fun TasksTopBar() {

    Text(
        text = "Tasks Top Bar",
    )
}

@Composable
private fun TasksList(
    modifier: Modifier = Modifier,
    list: List<TaskModel>
) {

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(items = list, key = { it.id }) { taskModel ->
            TaskItemCard(taskModel)
        }
    }
}

@Composable
private fun TaskItemCard(
    data: TaskModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
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