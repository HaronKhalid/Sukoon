package com.lu4p.fokuslauncher.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One-time dialog after a crash: shows the saved report and lets the user copy it. */
@Composable
fun CrashReportPrompt() {
    val context = LocalContext.current
    var report by remember { mutableStateOf(CrashReporter.pending(context)) }
    val text = report ?: return
    AlertDialog(
            onDismissRequest = {},
            title = { Text("Sukoon stopped last time") },
            text = {
                Text(
                        text = text.take(2_500),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = 4.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { copy(context, text) }) { Text("Copy report") }
            },
            dismissButton = {
                TextButton(
                        onClick = {
                            CrashReporter.clear(context)
                            report = null
                        }
                ) { Text("Dismiss") }
            },
    )
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Sukoon crash report", text))
    Toast.makeText(context, "Copied. Paste it into your message.", Toast.LENGTH_SHORT).show()
}
