package presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import max_helpdesk.features.tasks.impl.generated.resources.Res
import max_helpdesk.features.tasks.impl.generated.resources.open_parking_page
import max_helpdesk.features.tasks.impl.generated.resources.page_title
import org.jetbrains.compose.resources.stringResource
import presentation.intent.TasksPageIntent
import presentation.viewmodel.TasksPageViewModel

@Composable
internal fun HomePage(
    tasksPageViewModel: TasksPageViewModel,
) {
    HomePageContent(
        onParkingPage = { tasksPageViewModel.sendIntent(TasksPageIntent.OpenParkingPage) },
    )
}

@Composable
internal fun HomePageContent(
    onParkingPage: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageTitle()
            Spacer(modifier = Modifier.height(16.dp))
            OpenParkingPageButton(onClick = onParkingPage)
        }
    }
}

@Composable
private fun PageTitle() {
    Text(
        text = stringResource(resource = Res.string.page_title),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun OpenParkingPageButton(
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
    ) {
        Text(
            text = stringResource(resource = Res.string.open_parking_page),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

