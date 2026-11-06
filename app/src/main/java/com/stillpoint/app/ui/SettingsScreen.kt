package com.stillpoint.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillpoint.app.core.Config
import com.stillpoint.app.core.LimitEngine
import com.stillpoint.app.core.formatResetTime
import com.stillpoint.app.data.Store

data class SetupState(
    val usageAccess: Boolean,
    val canShowStopScreen: Boolean,
    val grayscaleAllowed: Boolean,
    val developerOptionsOn: Boolean,
)

private const val RESET_STEP = 15
private const val MINUTES_PER_DAY = 24 * 60
private val ResetPresets = listOf(0, 4 * 60, 6 * 60, 8 * 60)

const val GRANT_COMMAND =
    "adb shell pm grant com.stillpoint.app android.permission.WRITE_SECURE_SETTINGS"

@Composable
fun SettingsScreen(
    setup: SetupState,
    config: Config,
    usage: Map<String, Long>,
    versionName: String,
    onOpenUsageAccess: () -> Unit,
    onOpenDisplayOver: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onCopyCommand: () -> Unit,
) {
    val anyLocked = LimitEngine.anyLocked(config, usage)
    var pickingReset by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize()) {
        item { ScreenTitle("Settings") }

        item { SectionLabel("Setup") }
        item {
            SetupStep(
                number = 1,
                title = "Usage access",
                done = setup.usageAccess,
                body = "Needed for limits and grayscale, so Stillpoint knows which app is open. Find Stillpoint in the list and allow it.",
            ) { TextAction("Open usage access settings", onOpenUsageAccess) }
        }
        item {
            SetupStep(
                number = 2,
                title = "Stop screen",
                done = setup.canShowStopScreen,
                body = "Lets the stop screen open when a limit runs out. Allow Display over other apps for Stillpoint.",
            ) { TextAction("Open display over other apps", onOpenDisplayOver) }
        }
        item {
            SetupStep(
                number = 3,
                title = "Grayscale permission (optional)",
                done = setup.grayscaleAllowed,
                body = "Only for grayscale. Android only gives this over USB, once. On OnePlus, Oppo and Realme, first turn on Disable permission monitoring in Developer options and restart. Then run this on a computer:",
            ) {
                Text(
                    GRANT_COMMAND,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextAction("Copy command", onCopyCommand)
            }
        }
        item {
            val finished = setup.usageAccess && setup.canShowStopScreen && setup.grayscaleAllowed
            SetupStep(
                number = 4,
                title = "Turn Developer options off",
                done = !setup.developerOptionsOn,
                body = if (finished) {
                    "The steps above are done, so you can turn this off now. Doing so also closes the USB debugging connection."
                } else {
                    "Do this last. It also closes the USB debugging connection."
                },
            ) {
                if (finished) TextAction("Open developer options", onOpenDeveloperOptions)
            }
        }

        item { SectionLabel("Limits") }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !anyLocked) { pickingReset = true }
                    .padding(horizontal = PageGutter, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Limits reset at", style = MaterialTheme.typography.bodyLarge)
                    Muted(
                        if (anyLocked) {
                            "A limit is locked, so this can't change until then."
                        } else {
                            "Used up apps stay blocked until this time, even past midnight."
                        },
                    )
                }
                Text(
                    formatResetTime(config.resetMinute).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (anyLocked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                )
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !anyLocked || !config.lockExhausted) {
                        Store.updateConfig { it.copy(lockExhausted = !it.lockExhausted) }
                    }
                    .padding(horizontal = PageGutter, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Lock until reset", style = MaterialTheme.typography.bodyLarge)
                    Muted(
                        if (config.lockExhausted && anyLocked) {
                            "A limit has run out, so this stays on until ${formatResetTime(config.resetMinute)}."
                        } else {
                            "Once a limit runs out, you can't change or remove it until limits reset."
                        },
                    )
                }
                Switch(
                    checked = config.lockExhausted,
                    onCheckedChange = null,
                    enabled = !anyLocked || !config.lockExhausted,
                )
            }
        }

        item { SectionLabel("Privacy and security") }
        item {
            Column(Modifier.padding(horizontal = PageGutter)) {
                Fact("No internet", "Stillpoint doesn't have the internet permission, so Android stops it from sending or receiving anything.")
                Fact("Stays on this phone", "Your limits and usage are stored only in this app, and are left out of backups.")
                Fact("No accessibility access", "Stillpoint sees which apps you open and when, using Usage Access. It can't read what's on your screen, what you type, or your messages.")
                Fact("One system setting", "It uses the grayscale permission only to switch the colour filter on and off.")
                Fact("Easy to remove", "Uninstalling Stillpoint removes its permissions and data.")
                Muted("Version $versionName", Modifier.padding(top = 20.dp, bottom = 28.dp))
            }
        }
    }

    if (pickingReset) {
        ResetTimeSheet(
            current = config.resetMinute,
            onDismiss = { pickingReset = false },
            onSave = {
                Store.setResetMinute(it)
                pickingReset = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResetTimeSheet(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var minute by remember { mutableIntStateOf(current) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        scrimColor = colors.onBackground.copy(alpha = 0.4f),
        dragHandle = null,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(PageGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Limits reset at",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Muted(
                "For all apps and groups. Time already used today is kept.",
                Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatResetTime(minute).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f),
                )
                // The slider gets close in 15 minute jumps; these reach any exact minute.
                TextAction("-1m", {
                    minute = (minute - 1 + MINUTES_PER_DAY) % MINUTES_PER_DAY
                }, Modifier.padding(end = 20.dp))
                TextAction("+1m", { minute = (minute + 1) % MINUTES_PER_DAY })
            }
            Slider(
                value = minute.toFloat(),
                onValueChange = { minute = it.toInt() },
                valueRange = 0f..(MINUTES_PER_DAY - RESET_STEP).toFloat(),
                steps = MINUTES_PER_DAY / RESET_STEP - 2,
                colors = SliderDefaults.colors(
                    thumbColor = colors.onBackground,
                    activeTrackColor = colors.onBackground,
                    inactiveTrackColor = colors.outlineVariant,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
            Row(
                Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResetPresets.forEach { preset ->
                    val active = preset == minute
                    Box(
                        Modifier
                            .weight(1f)
                            .background(if (active) colors.onBackground else colors.outlineVariant)
                            .clickable { minute = preset }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            formatResetTime(preset).replace(":00", "").replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) colors.background else colors.onBackground,
                        )
                    }
                }
            }
            InkButton("Save", { onSave(minute) })
            TextAction("Cancel", onDismiss, Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    done: Boolean,
    body: String,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = PageGutter, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$number  $title", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                if (done) "Done" else "To do",
                style = MaterialTheme.typography.labelLarge,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
            )
        }
        if (!done) {
            Muted(body, Modifier.padding(top = 4.dp))
            content()
        }
    }
    Hairline()
}

@Composable
private fun Fact(title: String, body: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Muted(body)
    }
}
