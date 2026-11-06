package com.stillpoint.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stillpoint.app.core.formatMinutes
import com.stillpoint.app.data.InstalledApp

const val MIN_LIMIT = 5
const val MAX_LIMIT = 240
private const val LIMIT_STEP = 5
private const val WARNING_FRACTION = 0.8f
private val Presets = listOf(15, 30, 60, 120)

val PageGutter = 20.dp

private val GrayIcon = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.padding(start = PageGutter, end = PageGutter, top = 28.dp, bottom = 12.dp),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = PageGutter, end = PageGutter, top = 28.dp, bottom = 8.dp),
    )
}

/** Whether this much of a limit is enough to bring out the signal colour. */
fun isRunningOut(usedMs: Long, limitMinutes: Int): Boolean =
    usedMs >= limitMinutes * 60_000f * WARNING_FRACTION

/** How much of a limit is used. The signal colour appears only when time is nearly gone. */
@Composable
fun UsageBar(usedMs: Long, limitMinutes: Int, modifier: Modifier = Modifier) {
    val fraction = (usedMs.toFloat() / (limitMinutes * 60_000f)).coerceIn(0f, 1f)
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().height(3.dp).background(colors.outlineVariant)) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .background(if (isRunningOut(usedMs, limitMinutes)) colors.error else colors.onBackground),
        )
    }
}

@Composable
fun AppIcon(icon: ImageBitmap, modifier: Modifier = Modifier, colorful: Boolean = false) {
    Image(
        icon,
        contentDescription = null,
        modifier = modifier.size(40.dp),
        colorFilter = if (colorful) null else GrayIcon,
    )
}

@Composable
fun AppRow(
    app: InstalledApp,
    subtitle: String?,
    modifier: Modifier = Modifier,
    colorfulIcon: Boolean = false,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = PageGutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppIcon(app.icon, colorful = colorfulIcon)
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) Muted(subtitle)
        }
        trailing()
    }
}

@Composable
fun Hairline() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = PageGutter),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** An underlined text field that matches the app's flat rows. */
@Composable
fun LineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(colors.onBackground),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Column {
                Box(Modifier.padding(vertical = 12.dp)) {
                    if (value.isEmpty()) Muted(placeholder)
                    inner()
                }
                HorizontalDivider(color = colors.onBackground)
            }
        },
    )
}

/** The main action on a page: solid ink, full width. */
@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .background(if (enabled) colors.onBackground else colors.outlineVariant)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) colors.background else colors.onSurfaceVariant,
        )
    }
}

/** A plain text action, for everything that isn't the main action on the page. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}

@Composable
fun LimitPicker(minutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        Text(formatMinutes(minutes), style = MaterialTheme.typography.displayLarge)
        Slider(
            value = minutes.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = MIN_LIMIT.toFloat()..MAX_LIMIT.toFloat(),
            steps = (MAX_LIMIT - MIN_LIMIT) / LIMIT_STEP - 1,
            colors = SliderDefaults.colors(
                thumbColor = colors.onBackground,
                activeTrackColor = colors.onBackground,
                inactiveTrackColor = colors.outlineVariant,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Presets.forEach { preset ->
                val active = preset == minutes
                Box(
                    Modifier
                        .weight(1f)
                        .background(if (active) colors.onBackground else colors.outlineVariant)
                        .clickable { onChange(preset) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        formatMinutes(preset),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) colors.background else colors.onBackground,
                    )
                }
            }
        }
    }
}

fun List<InstalledApp>.matching(query: String): List<InstalledApp> =
    if (query.isBlank()) this else filter { it.label.contains(query.trim(), ignoreCase = true) }
