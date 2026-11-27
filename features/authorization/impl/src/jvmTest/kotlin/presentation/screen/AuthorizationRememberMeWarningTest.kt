package presentation.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.flow.MutableSharedFlow
import login.LoginMode
import presentation.actions.AuthorizationScreenActions
import presentation.screen.compact.AuthorizationScreenCompact
import presentation.screen.compact.REMEMBER_ME_TEST_TAG
import presentation.state.AuthorizationState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AuthorizationRememberMeWarningTest {

    @Test
    fun `warns about plain text token when remember me is enabled`() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(
                LocalAuthorizationActions provides AuthorizationScreenActions(
                    authorize = { _, _ -> },
                ),
            ) {
                AuthorizationScreenCompact(
                    state = AuthorizationState(
                        loginMode = LoginMode.Credentials,
                        inProgress = false,
                    ),
                    effect = MutableSharedFlow(),
                )
            }
        }

        onNodeWithTag(REMEMBER_ME_TEST_TAG).performClick()

        onNodeWithText(REMEMBER_ME_WARNING_TEXT).assertIsDisplayed()
        onNodeWithText("Отмена").assertIsDisplayed()
    }

    private companion object {
        const val REMEMBER_ME_WARNING_TEXT =
            "В текущей версии токен авторизации хранится в открытом виде на устройстве."
    }
}
