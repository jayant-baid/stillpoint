package com.stillpoint.app.service

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillpoint.app.core.BlockReason
import com.stillpoint.app.core.formatMinutes
import com.stillpoint.app.ui.InkButton
import com.stillpoint.app.ui.Muted
import com.stillpoint.app.ui.PageGutter
import com.stillpoint.app.ui.StillpointTheme

/**
 * The full-screen stop page. It is an ordinary activity rather than an accessibility overlay,
 * because Android always draws those above the notification shade, which would hide the shade.
 */
class BlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this) { goHome() }
        setContent {
            StillpointTheme {
                val info by BlockGate.current.collectAsState()
                LaunchedEffect(info) { if (info == null) finish() }
                info?.let { StopPage(it, onLeave = ::goHome) }
            }
        }
    }

    override fun onDestroy() {
        BlockGate.screenAlive = false
        super.onDestroy()
    }

    /** The service sees the home screen open, clears the gate, and that closes this screen. */
    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
private fun StopPage(info: BlockGate.Info, onLeave: () -> Unit) {
    val (minutes, detail) = when (val reason = info.reason) {
        is BlockReason.AppLimit -> reason.minutes to "That was its daily limit."
        is BlockReason.GroupLimit ->
            reason.minutes to "That was the shared limit for ${reason.groupName}."
    }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(start = PageGutter, end = PageGutter, top = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            formatMinutes(minutes),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            "${info.appLabel} is done for now",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Muted(
            "$detail It opens again at ${info.resetLabel}.",
            Modifier.padding(top = 8.dp, bottom = 32.dp),
        )
        InkButton("Go to home screen", onLeave)
    }
}
