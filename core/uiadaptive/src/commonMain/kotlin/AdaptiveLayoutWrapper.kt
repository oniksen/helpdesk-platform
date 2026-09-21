import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

@Composable
fun<S, E> AdaptiveLayoutWrapper(
    state: S,
    effect: E,
    compact: @Composable (state: S, effect: E) -> Unit,
    medium: (@Composable (state: S, effect: E) -> Unit)? = null,
    expanded: @Composable (state: S, effect: E) -> Unit = medium ?: compact,
) {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) ->
            expanded(state, effect)
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
            (medium ?: compact)(state, effect)
        else ->
            compact(state, effect)
    }
}

@Composable
fun AdaptiveLayoutWrapper(
    compact: @Composable () -> Unit,
    medium: (@Composable () -> Unit)? = null,
    expanded: @Composable () -> Unit = medium ?: compact,
) {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) ->
            expanded()
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
            (medium ?: compact)()
        else ->
            compact()
    }
}