package com.stillpoint.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stillpoint.app.data.Apps
import com.stillpoint.app.data.InstalledApp
import com.stillpoint.app.data.Store
import com.stillpoint.app.service.GrayscaleController
import com.stillpoint.app.service.StillpointService
import com.stillpoint.app.ui.GRANT_COMMAND
import com.stillpoint.app.ui.GrayscaleScreen
import com.stillpoint.app.ui.GroupsScreen
import com.stillpoint.app.ui.PageGutter
import com.stillpoint.app.ui.SettingsScreen
import com.stillpoint.app.ui.SetupState
import com.stillpoint.app.ui.StillpointTheme
import com.stillpoint.app.ui.TodayScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Tab(val title: String) {
    Today("Today"),
    Groups("Groups"),
    Gray("Grayscale"),
    Settings("Settings"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        enableEdgeToEdge()
        setContent { StillpointTheme { Root() } }
    }
}

private fun readSetup(context: Context) = SetupState(
    serviceOn = StillpointService.isEnabled(context),
    grayscaleAllowed = GrayscaleController.isPermitted(context),
    developerOptionsOn = Settings.Global.getInt(
        context.contentResolver,
        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
        0,
    ) == 1,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Root() {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(Tab.Today) }
    var editingGroup by remember { mutableStateOf(false) }
    var setup by remember { mutableStateOf(readSetup(context)) }
    LifecycleResumeEffect(Unit) {
        setup = readSetup(context)
        onPauseOrDispose { }
    }

    val config by Store.config.collectAsStateWithLifecycle()
    val ledger by Store.ledger.collectAsStateWithLifecycle()
    val usage = ledger.forDay(Store.today()).usageMs
    val apps by produceState<List<InstalledApp>?>(initialValue = null) {
        value = withContext(Dispatchers.Default) { Apps.loadLaunchable(context) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        val nudge = when {
            tab == Tab.Settings -> null
            !setup.serviceOn -> "Limits and grayscale are off. Finish setup"
            tab == Tab.Gray && !setup.grayscaleAllowed -> "Grayscale needs one more step"
            else -> null
        }
        if (nudge != null) SetupNudge(nudge, onClick = { tab = Tab.Settings })

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                Tab.Today -> TodayScreen(apps, config, usage)
                Tab.Groups -> GroupsScreen(apps, config, usage, onEditingChange = { editingGroup = it })
                Tab.Gray -> GrayscaleScreen(apps, config)
                Tab.Settings -> SettingsScreen(
                    setup = setup,
                    config = config,
                    usage = usage,
                    versionName = context.packageManager
                        .getPackageInfo(context.packageName, 0).versionName.orEmpty(),
                    onOpenAccessibility = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenDeveloperOptions = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    },
                    onCopyCommand = { copyToClipboard(context, GRANT_COMMAND) },
                )
            }
        }

        // Tabs give way to the keyboard, and to the group editor so unsaved edits aren't lost.
        if (!editingGroup && !WindowInsets.isImeVisible) TabBar(tab, onSelect = { tab = it })
    }
}

@Composable
private fun SetupNudge(text: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.onBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = PageGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = colors.background,
        )
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    HorizontalDivider(color = colors.outlineVariant)
    Row(Modifier.fillMaxWidth()) {
        Tab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                Modifier.weight(1f).clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (active) colors.onBackground else colors.background),
                )
                Text(
                    tab.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) colors.onBackground else colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText("ADB command", text))
}
