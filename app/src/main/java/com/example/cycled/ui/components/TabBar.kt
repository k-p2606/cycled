package com.example.cycled.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors

/** Height of the bar itself, not counting the system navigation bar under it. */
val TabBarHeight = 76.dp

@Immutable
data class TabBarItem(val label: String, val icon: ImageVector)

/**
 * The fixed bottom tab bar (handoff "Tab bar"): equal columns, a 24 dp icon over a
 * 12 sp label. The active tab is cream with a SemiBold label; the rest are text-dim.
 * Each tab fills its column and the bar's full 76 dp height, so taps are well over 44 dp.
 *
 * The bar pads itself above the system navigation bar and paints sky behind it.
 * [showTopDivider] draws the 1 dp divider-sky line used on Profile, where the
 * screen and the bar are both sky.
 */
@Composable
fun TabBar(
    items: List<TabBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showTopDivider: Boolean = false,
) {
    val colors = MaterialTheme.cycledColors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.sky)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        if (showTopDivider) HorizontalDivider(thickness = 1.dp, color = colors.dividerSky)
        Row(
            Modifier
                .fillMaxWidth()
                .height(TabBarHeight)
                .selectableGroup(),
        ) {
            items.forEachIndexed { index, item ->
                Tab(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun Tab(item: TabBarItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.cycledColors
    val color = if (selected) colors.cream else colors.textDim
    Column(
        // Role.Tab + selected: TalkBack reads "Selected, Home, Tab, 1 of 4".
        modifier = modifier
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(top = 8.dp, start = 2.dp, end = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val PreviewItems = listOf(
    TabBarItem("Home", CycledIcons.Home),
    TabBarItem("Calendar", CycledIcons.Calendar),
    TabBarItem("Logs", CycledIcons.Logs),
    TabBarItem("Profile", CycledIcons.Profile),
)

@Preview(widthDp = 390)
@Composable
private fun TabBarPreview() {
    CycledTheme {
        var selected by remember { mutableIntStateOf(0) }
        TabBar(items = PreviewItems, selectedIndex = selected, onSelect = { selected = it })
    }
}

@Preview(widthDp = 360, fontScale = 1.5f)
@Composable
private fun TabBarProfileLargeFontPreview() {
    CycledTheme {
        TabBar(items = PreviewItems, selectedIndex = 3, onSelect = {}, showTopDivider = true)
    }
}
