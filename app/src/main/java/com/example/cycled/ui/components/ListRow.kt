package com.example.cycled.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors

/**
 * A settings / summary row (handoff "List row"): a 1 dp divider on top, at least
 * [minHeight] tall, an optional [leading] mark, the label, and a value or control on
 * the right. Use [dividerColor] divider-water on water panels, divider-sky on sky.
 *
 * With [onClick] the whole row is one button; otherwise any control in [trailing]
 * is its own tap target.
 */
@Composable
fun ListRow(
    label: String,
    modifier: Modifier = Modifier,
    dividerColor: Color = MaterialTheme.cycledColors.dividerSky,
    minHeight: Dp = 52.dp,
    labelColor: Color = MaterialTheme.cycledColors.text,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    ListRow(
        modifier = modifier,
        dividerColor = dividerColor,
        minHeight = minHeight,
        onClick = onClick,
        leading = leading,
        trailing = trailing,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
        )
    }
}

/**
 * [ListRow] with any [content] in place of the one-line label, e.g. several lines of
 * text. [content] takes the width left between [leading] and [trailing].
 */
@Composable
fun ListRow(
    modifier: Modifier = Modifier,
    dividerColor: Color = MaterialTheme.cycledColors.dividerSky,
    minHeight: Dp = 52.dp,
    verticalPadding: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = dividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            leading?.invoke()
            Box(Modifier.weight(1f)) { content() }
            trailing?.invoke(this)
        }
    }
}

@Preview(widthDp = 390)
@Composable
private fun ListRowPreview() {
    CycledTheme {
        val colors = MaterialTheme.cycledColors
        Column {
            Column(
                Modifier
                    .background(colors.water)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                ListRow(
                    label = "Next period",
                    dividerColor = colors.dividerWater,
                    minHeight = 50.dp,
                    leading = { Box(Modifier.size(9.dp).background(colors.period, CircleShape)) },
                ) {
                    Text("Sat 28 Nov", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                    Text(
                        "in 20 days",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.cream,
                    )
                }
                ListRow(
                    label = "High tide",
                    dividerColor = colors.dividerWater,
                    minHeight = 50.dp,
                    leading = { Box(Modifier.size(9.dp).background(colors.cream, CircleShape)) },
                ) {
                    Text("passed", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                }
            }
            Column(
                Modifier
                    .background(colors.sky)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                ListRow(label = "Period coming up") { Switch(checked = true, onCheckedChange = {}) }
                ListRow(label = "Export my data", labelColor = colors.textDim) {
                    Text("Coming soon", style = MaterialTheme.typography.bodyMedium, color = colors.textDim)
                }
                ListRow(label = "Delete all my data", labelColor = colors.period, onClick = {})
            }
        }
    }
}
