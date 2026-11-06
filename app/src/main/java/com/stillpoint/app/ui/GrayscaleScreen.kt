package com.stillpoint.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillpoint.app.core.Config
import com.stillpoint.app.data.InstalledApp
import com.stillpoint.app.data.Store

@Composable
fun GrayscaleScreen(apps: List<InstalledApp>?, config: Config) {
    var query by remember { mutableStateOf("") }
    val loaded = apps.orEmpty()
    val ordered = remember(loaded) { loaded.sortedBy { it.pkg !in config.grayscaleApps } }

    LazyColumn(Modifier.fillMaxSize()) {
        item { ScreenTitle("Grayscale") }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { Store.updateConfig { it.copy(grayscaleEnabled = !it.grayscaleEnabled) } }
                    .padding(horizontal = PageGutter, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Use grayscale", style = MaterialTheme.typography.bodyLarge)
                    Muted(
                        if (config.grayscaleEnabled) {
                            val count = config.grayscaleApps.size
                            "$count ${if (count == 1) "app opens" else "apps open"} without colour"
                        } else {
                            "Paused. Every app shows normal colours"
                        },
                    )
                }
                Switch(checked = config.grayscaleEnabled, onCheckedChange = null)
            }
        }
        item {
            LineField(
                query,
                { query = it },
                "Search apps",
                Modifier.padding(start = PageGutter, end = PageGutter, top = 8.dp),
            )
        }
        if (apps == null) {
            item { Muted("Loading your apps", Modifier.padding(PageGutter)) }
        }
        items(ordered.matching(query), key = { it.pkg }) { app ->
            val gray = app.pkg in config.grayscaleApps
            // The icon previews the effect: it loses its colour when the app is switched on.
            AppRow(
                app = app,
                subtitle = null,
                colorfulIcon = !gray,
                modifier = Modifier.clickable {
                    Store.updateConfig {
                        it.copy(
                            grayscaleApps = if (gray) {
                                it.grayscaleApps - app.pkg
                            } else {
                                it.grayscaleApps + app.pkg
                            },
                        )
                    }
                },
            ) {
                Switch(checked = gray, onCheckedChange = null)
            }
            Hairline()
        }
    }
}
