package org.lpmti.helpdeskplatform

import DiProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    // Гарантируем отсутствие лишних рекомпозиций.
    remember { DiProvider() }.MainKoinApplication()
}