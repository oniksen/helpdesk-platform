package presentation.screen.compact

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import domain.model.TaskModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import presentation.effect.TasksPageEffect
import presentation.state.TasksPageState

internal val previewState = TasksPageState(
    currentList = mutableListOf<TaskModel>().apply {
        repeat(10) { index ->
            add(
                TaskModel(
                    id = index.toLong(),
                    title = "Task $index",
                )
            )
        }
    }
)
internal val previewEffect: SharedFlow<TasksPageEffect>
    field = MutableSharedFlow<TasksPageEffect>(
        replay = 0,
        extraBufferCapacity = 1
    ).apply {
        tryEmit(
            TasksPageEffect.ShowSnackBar(
                message = "Ошибка загрузки"
            )
        )
    }

@Composable
private fun PreviewState() {
    TasksPageContentCompact(
        state = previewState,
        effect = previewEffect
    )
}

@Preview
@Composable
private fun TasksPageCompatPreviewLight() {
    PreviewWrapper {
        PreviewState()
    }
}

@Preview
@Composable
private fun TasksPageCompatPreviewDark() {
    PreviewWrapper(true) {
        PreviewState()
    }
}