package com.stillpoint.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stillpoint.app.core.Config
import com.stillpoint.app.core.LimitEngine
import com.stillpoint.app.core.LimitGroup
import com.stillpoint.app.core.formatMinutes
import com.stillpoint.app.core.formatResetTime
import com.stillpoint.app.core.formatUsage
import com.stillpoint.app.data.InstalledApp
import com.stillpoint.app.data.Store
import java.util.UUID

private val NewGroup = LimitGroup(id = "", name = "", minutes = 60, packages = emptySet())
private const val MAX_MEMBER_ICONS = 7

@Composable
fun GroupsScreen(
    apps: List<InstalledApp>?,
    config: Config,
    usage: Map<String, Long>,
    onEditingChange: (Boolean) -> Unit,
) {
    var editing by remember { mutableStateOf<LimitGroup?>(null) }
    val loaded = apps.orEmpty()

    editing?.let { group ->
        DisposableEffect(Unit) {
            onEditingChange(true)
            onDispose { onEditingChange(false) }
        }
        GroupEditor(group, loaded, onDone = { editing = null })
        return
    }

    val byPackage = remember(loaded) { loaded.associateBy { it.pkg } }
    LazyColumn(Modifier.fillMaxSize()) {
        item { ScreenTitle("Groups") }
        item {
            Muted(
                "Apps in a group share one daily limit. When it runs out, all of them stop.",
                Modifier.padding(start = PageGutter, end = PageGutter, bottom = 16.dp),
            )
        }
        item {
            InkButton(
                "New group",
                { editing = NewGroup },
                Modifier.padding(start = PageGutter, end = PageGutter, bottom = 12.dp),
            )
        }
        if (config.groups.isEmpty()) {
            item {
                Muted(
                    "No groups yet. For example, put Instagram, YouTube and X in a group with one hour between them.",
                    Modifier.padding(horizontal = PageGutter, vertical = 16.dp),
                )
            }
        }
        items(config.groups, key = { it.id }) { group ->
            val used = LimitEngine.groupUsageMs(group, usage)
            val locked = LimitEngine.isGroupLocked(group, config, usage)
            val colors = MaterialTheme.colorScheme
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !locked) { editing = group }
                    .padding(horizontal = PageGutter, vertical = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        group.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${formatUsage(used)} / ${formatMinutes(group.minutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRunningOut(used, group.minutes)) colors.error else colors.onBackground,
                    )
                }
                Row(
                    Modifier.padding(top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    group.packages.mapNotNull { byPackage[it] }
                        .sortedBy { it.label.lowercase() }
                        .take(MAX_MEMBER_ICONS)
                        .forEach { AppIcon(it.icon, Modifier.size(28.dp)) }
                    val extra = group.packages.size - MAX_MEMBER_ICONS
                    if (extra > 0) Muted("+$extra")
                }
                UsageBar(used, group.minutes)
                if (locked) {
                    Text(
                        "Locked until ${formatResetTime(config.resetMinute)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupEditor(group: LimitGroup, apps: List<InstalledApp>, onDone: () -> Unit) {
    var name by remember { mutableStateOf(group.name) }
    var minutes by remember { mutableIntStateOf(group.minutes) }
    var selected by remember { mutableStateOf(group.packages) }
    var query by remember { mutableStateOf("") }
    val ordered = remember(apps) { apps.sortedBy { it.pkg !in group.packages } }
    val isNew = group.id.isEmpty()
    BackHandler(onBack = onDone)

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            item { ScreenTitle(if (isNew) "New group" else "Edit group") }
            item {
                Column(Modifier.padding(horizontal = PageGutter)) {
                    LineField(name, { name = it }, "Name, for example Social")
                    Muted("Shared daily limit", Modifier.padding(top = 24.dp))
                    LimitPicker(minutes, { minutes = it })
                }
            }
            item { SectionLabel("Apps in this group") }
            item {
                LineField(query, { query = it }, "Search apps", Modifier.padding(horizontal = PageGutter))
            }
            items(ordered.matching(query), key = { it.pkg }) { app ->
                val checked = app.pkg in selected
                AppRow(
                    app = app,
                    subtitle = null,
                    modifier = Modifier.clickable {
                        selected = if (checked) selected - app.pkg else selected + app.pkg
                    },
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                }
                Hairline()
            }
        }
        if (!WindowInsets.isImeVisible) Column(Modifier.padding(horizontal = PageGutter, vertical = 12.dp)) {
            InkButton(
                text = if (selected.isEmpty()) "Choose at least one app" else "Save, ${selected.size} ${if (selected.size == 1) "app" else "apps"}",
                enabled = name.isNotBlank() && selected.isNotEmpty(),
                onClick = {
                    val saved = group.copy(
                        id = group.id.ifEmpty { UUID.randomUUID().toString() },
                        name = name.trim(),
                        minutes = minutes,
                        packages = selected,
                    )
                    Store.updateConfig { c ->
                        c.copy(groups = c.groups.filterNot { it.id == saved.id } + saved)
                    }
                    onDone()
                },
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextAction("Cancel", onDone)
                if (!isNew) {
                    TextAction("Delete group", {
                        Store.updateConfig { c -> c.copy(groups = c.groups.filterNot { it.id == group.id }) }
                        onDone()
                    })
                }
            }
        }
    }
}
