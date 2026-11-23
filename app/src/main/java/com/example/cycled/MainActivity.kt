package com.example.cycled

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cycled.ui.CycleViewModel
import com.example.cycled.ui.CycledApp
import com.example.cycled.ui.theme.CycledTheme

/**
 * An "Activity" is one screen of your app. When you tap the app icon,
 * Android creates MainActivity and calls onCreate() — that's the starting point.
 * (AndroidManifest.xml is where MainActivity is registered as the launch screen.)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is dark-only, so the system bars always get light icons,
        // whatever the phone's own light/dark setting is.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        // setContent { ... } says "draw this UI on the screen".
        // Everything inside is built from @Composable functions.
        setContent {
            CycledTheme {
                // The ViewModel survives rotation; its Factory pulls the
                // repository from CycledApplication.
                val cycleViewModel: CycleViewModel = viewModel(factory = CycleViewModel.Factory)
                // Opens on Home, with the bottom tab bar on every screen.
                CycledApp(cycleViewModel)
            }
        }
    }
}

/**
 * The whole screen: a scrollable vertical list (Column) of small demos.
 */
@Composable
fun LearningScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // lets you scroll if it's taller than the screen
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // 16dp gap between each child
    ) {
        Text("My First Android App", style = MaterialTheme.typography.headlineMedium)

        CounterDemo()
        GreetingDemo()
        ToggleDemo()
        ChecklistDemo()
        ToastDemo()
    }
}

/**
 * DEMO 1: A button that counts taps.
 *
 * The key idea in Compose is STATE. `count` is a value Compose watches.
 * When it changes, Compose automatically redraws (re-"composes") any UI that reads it.
 */
@Composable
fun CounterDemo() {
    // remember { } keeps the value alive between redraws.
    // mutableIntStateOf(0) makes it "observable" so changes trigger a redraw.
    var count by remember { mutableIntStateOf(0) }

    DemoCard(title = "1. Button + Counter") {
        Text("You tapped the button $count times")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // onClick is the code that runs when the button is tapped.
            Button(onClick = { count++ }) {
                Text("Tap me!")
            }
            OutlinedButton(onClick = { count = 0 }) {
                Text("Reset")
            }
        }
    }
}

/**
 * DEMO 2: A text box. Whatever you type shows up in the greeting below it.
 */
@Composable
fun GreetingDemo() {
    var name by remember { mutableStateOf("") }

    DemoCard(title = "2. Text Input") {
        OutlinedTextField(
            value = name,                       // what the box displays
            onValueChange = { name = it },      // runs on every keystroke; `it` is the new text
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // An if-statement decides what to show — UI is just Kotlin code!
        if (name.isBlank()) {
            Text("Type your name above 👆")
        } else {
            Text("Hello, $name! 👋", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * DEMO 3: A switch that changes the color of a piece of text.
 */
@Composable
fun ToggleDemo() {
    var isOn by remember { mutableStateOf(false) }

    DemoCard(title = "3. Switch") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = isOn, onCheckedChange = { isOn = it })
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isOn) "The light is ON" else "The light is OFF",
                color = if (isOn) Color(0xFF2E7D32) else Color.Gray
            )
        }
    }
}

/**
 * DEMO 4: A small checklist built from a list of strings.
 */
@Composable
fun ChecklistDemo() {
    val tasks = listOf("Open Android Studio", "Run the app", "Change some code")
    // One true/false value per task, remembered across redraws.
    var checked by remember { mutableStateOf(List(tasks.size) { false }) }

    DemoCard(title = "4. Checklist") {
        // forEachIndexed loops through the list and creates one Row per task.
        tasks.forEachIndexed { index, task ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked[index],
                    onCheckedChange = { isChecked ->
                        // Make a copy of the list with this one item changed.
                        checked = checked.toMutableList().also { it[index] = isChecked }
                    }
                )
                Text(task)
            }
        }
        HorizontalDivider()
        Text("Done: ${checked.count { it }} / ${tasks.size}")
    }
}

/**
 * DEMO 5: A Toast — the little pop-up message at the bottom of the screen.
 */
@Composable
fun ToastDemo() {
    // Toasts need a "Context" (a handle to the Android system).
    val context = LocalContext.current

    DemoCard(title = "5. Toast Message") {
        Button(onClick = {
            Toast.makeText(context, "Hi from Kotlin! 🎉", Toast.LENGTH_SHORT).show()
        }) {
            Text("Show a Toast")
        }
    }
}

/**
 * A reusable helper: a Card with a title and whatever content you pass in.
 * `content` is itself a composable — this is how you build your own UI "components".
 */
@Composable
fun DemoCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/**
 * @Preview lets you see the UI in Android Studio WITHOUT running the app.
 * Open this file, then click "Split" or "Design" at the top-right of the editor.
 */
@Preview(showBackground = true)
@Composable
fun LearningScreenPreview() {
    CycledTheme {
        LearningScreen()
    }
}
