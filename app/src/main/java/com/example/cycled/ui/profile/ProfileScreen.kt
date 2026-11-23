package com.example.cycled.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cycled.ui.components.PlaceholderTitleScreen
import com.example.cycled.ui.theme.CycledTheme

/** Profile tab. A placeholder for now; settings come in a later step (D9). */
@Composable
fun ProfileRoute(modifier: Modifier = Modifier) {
    ProfileScreen(modifier = modifier)
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    PlaceholderTitleScreen(title = "Profile", modifier = modifier)
}

@Preview(widthDp = 390, heightDp = 768)
@Composable
private fun ProfileScreenPreview() {
    CycledTheme { ProfileScreen() }
}
