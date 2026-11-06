package com.stillpoint.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillpoint.app.core.Config
import com.stillpoint.app.core.LimitEngine
import com.stillpoint.app.core.formatMinutes
import com.stillpoint.app.core.formatResetTime
import com.stillpoint.app.core.formatUsage
import com.stillpoint.app.data.InstalledApp
import com.stillpoint.app.data.Store
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DateLine = DateTimeFormatter.ofPattern("EEEE d MMMM")

@Composable
fun TodayScreen(apps: List<InstalledApp>?, config: Config, usage: Map<String, Long>) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<InstalledApp?>(null) }
    val shown = apps.orEmpty().matching(query)
    val (limited, others) = shown.partition { it.pkg in config.appLimits }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(start = PageGutter, end = PageGutter, top = 28.dp, bottom = 20.dp)) {
                Text(
                    LocalDate.now().format(DateLine).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatUsage(usage.values.sum()), style = MaterialTheme.typography.displayLarge)
                Muted("in apps today")
            }
        }
        item {
            LineField(query, { query = it }, "Search apps", Modifier.padding(horizontal = PageGutter))
        }
        if (apps == null) {
            item { Muted("Loading your apps", Modifier.padding(PageGutter)) }
        }
        if (limited.isNotEmpty()) {
            item { SectionLabel("With limits") }
            items(limited, key = { it.pkg }) { app ->
                val limit = config.appLimits.getValue(app.pkg)
                val used = usage[app.pkg] ?: 0L
                AppRow(app, subtitle = null, Modifier.clickable { editing = app }) {
                    Text(
                        "${formatUsage(used)} / ${formatMinutes(limit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRunningOut(used, limit)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                    )
                }
                UsageBar(used, limit, Modifier.padding(horizontal = PageGutter))
            }
        }
        if (others.isNotEmpty()) {
            item { SectionLabel(if (limited.isEmpty()) "Tap an app to set a limit" else "No limit") }
            items(others, key = { it.pkg }) { app ->
                val used = usage[app.pkg] ?: 0L
                AppRow(
                    app,
                    subtitle = null,
                    Modifier.clickable { editing = app },
                ) {
                    if (used > 0) Muted(formatUsage(used))
                }
                Hairline()
            }
        }
    }

    editing?.let { app ->
        LimitSheet(
            app = app,
            current = config.appLimits[app.pkg],
            locked = LimitEngine.isAppLocked(app.pkg, config, usage),
            resetLabel = formatResetTime(config.resetMinute),
            onDismiss = { editing = null },
            onSave = { minutes ->
                Store.updateConfig { it.copy(appLimits = it.appLimits + (app.pkg to minutes)) }
                editing = null
            },
            onRemove = {
                Store.updateConfig { it.copy(appLimits = it.appLimits - app.pkg) }
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LimitSheet(
    app: InstalledApp,
    current: Int?,
    locked: Boolean,
    resetLabel: String,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    var minutes by remember { mutableIntStateOf(current ?: 30) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().padding(PageGutter)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium)
            if (locked) {
                Text(
                    "Locked until $resetLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Muted(
                    "This limit has run out, and the lock in Settings keeps it in place until limits reset.",
                    Modifier.padding(top = 4.dp, bottom = 20.dp),
                )
                InkButton("Close", onDismiss)
            } else {
                Muted("Daily limit", Modifier.padding(top = 2.dp, bottom = 8.dp))
                LimitPicker(minutes, { minutes = it })
                InkButton("Save", { onSave(minutes) }, Modifier.padding(top = 20.dp))
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (current != null) {
                        TextAction("Remove limit", onRemove)
                    } else {
                        TextAction("Cancel", onDismiss)
                    }
                }
            }
        }
    }
}
