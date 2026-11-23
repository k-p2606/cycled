package com.example.cycled.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cycled.ui.theme.CycledTheme
import com.example.cycled.ui.theme.cycledColors

private const val DISABLED_ALPHA = 0.4f

/**
 * The full-width, fully rounded 52 dp button (handoff "Primary button"), Figtree 17/600.
 * Defaults to cream with sky text (Home's "Log today"). Calendar's period button passes
 * its own colours and a coral [borderColor]; [icon] goes before the text (e.g. a check).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.cycledColors.cream,
    contentColor: Color = MaterialTheme.cycledColors.sky,
    borderColor: Color? = null,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = containerColor.alpha * DISABLED_ALPHA),
            disabledContentColor = contentColor.copy(alpha = DISABLED_ALPHA + 0.2f),
        ),
        border = borderColor?.let {
            BorderStroke(1.5.dp, if (enabled) it else it.copy(alpha = DISABLED_ALPHA))
        },
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
    }
}

@Preview(widthDp = 390)
@Composable
private fun PrimaryButtonPreview() {
    CycledTheme {
        val colors = MaterialTheme.cycledColors
        Column(
            Modifier
                .background(colors.water)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(text = "Log today", onClick = {})
            // Calendar's period button, off / on / disabled (future day).
            PrimaryButton(
                text = "Log period today",
                onClick = {},
                containerColor = colors.period,
                contentColor = colors.sky,
                borderColor = colors.period,
            )
            PrimaryButton(
                text = "Period logged today",
                onClick = {},
                containerColor = Color.Transparent,
                contentColor = colors.periodLight,
                borderColor = colors.period,
                icon = CycledIcons.Check,
            )
            PrimaryButton(
                text = "Log period on 12 Nov",
                onClick = {},
                enabled = false,
                containerColor = colors.period,
                contentColor = colors.sky,
                borderColor = colors.period,
            )
        }
    }
}

@Preview(widthDp = 360, fontScale = 1.5f)
@Composable
private fun PrimaryButtonLargeFontPreview() = PrimaryButtonPreview()
