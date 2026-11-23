package com.example.cycled.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors

/**
 * A sky screen with just its title (Bricolage 34/600, top 60, sides 24), for tabs
 * that aren't built yet.
 */
@Composable
fun PlaceholderTitleScreen(title: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.cycledColors.sky)
            .statusBarsPadding()
            .padding(top = 60.dp, start = 24.dp, end = 24.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.cycledColors.text,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Preview(widthDp = 390, heightDp = 400)
@Composable
private fun PlaceholderTitleScreenPreview() {
    CycledTheme { PlaceholderTitleScreen(title = "Logs") }
}
