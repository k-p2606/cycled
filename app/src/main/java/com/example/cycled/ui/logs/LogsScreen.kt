package com.example.cycled.ui.logs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cycled.ui.components.PlaceholderTitleScreen
import com.example.cycled.ui.theme.CycledTheme

/** Logs tab. A placeholder for now; the cycle list comes in a later step (D10). */
@Composable
fun LogsRoute(modifier: Modifier = Modifier) {
    LogsScreen(modifier = modifier)
}

@Composable
fun LogsScreen(modifier: Modifier = Modifier) {
    PlaceholderTitleScreen(title = "Logs", modifier = modifier)
}

@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun LogsScreenPreview() {
    CycledTheme { LogsScreen() }
}
