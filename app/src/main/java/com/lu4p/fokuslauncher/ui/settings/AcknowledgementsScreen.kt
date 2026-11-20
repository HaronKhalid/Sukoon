package com.lu4p.fokuslauncher.ui.settings

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.settings.components.SectionHeader
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDivider
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow

private data class AckEntry(
        @param:StringRes val titleRes: Int,
        @param:StringRes val bodyRes: Int,
        val url: String,
)

private val upstreamEntries =
        listOf(
                AckEntry(
                        R.string.about_ack_fokus_title,
                        R.string.about_ack_fokus_body,
                        "https://github.com/luantak/FokusLauncher",
                )
        )

private val referenceEntries =
        listOf(
                AckEntry(
                        R.string.about_ack_victoria_title,
                        R.string.about_ack_victoria_body,
                        "https://github.com/adelmonte/victoria-launcher",
                )
        )

private val libraryEntries =
        listOf(
                AckEntry(
                        R.string.about_ack_androidx_title,
                        R.string.about_ack_androidx_body,
                        "https://developer.android.com/jetpack",
                ),
                AckEntry(
                        R.string.about_ack_material_title,
                        R.string.about_ack_material_body,
                        "https://github.com/material-components/material-components-android",
                ),
                AckEntry(
                        R.string.about_ack_symbols_title,
                        R.string.about_ack_symbols_body,
                        "https://fonts.google.com/icons",
                ),
                AckEntry(
                        R.string.about_ack_hilt_title,
                        R.string.about_ack_hilt_body,
                        "https://dagger.dev/hilt/",
                ),
                AckEntry(
                        R.string.about_ack_kotlin_title,
                        R.string.about_ack_kotlin_body,
                        "https://kotlinlang.org/",
                ),
                AckEntry(
                        R.string.about_ack_openmeteo_title,
                        R.string.about_ack_openmeteo_body,
                        "https://open-meteo.com/",
                ),
                AckEntry(
                        R.string.about_ack_arcticons_title,
                        R.string.about_ack_arcticons_body,
                        "https://arcticons.com/",
                ),
        )

private const val SOURCE_URL = "https://github.com/HaronKhalid/Sukoon"
private const val GPL_ASSET_PATH = "licenses/GPL-3.0.txt"

/** Full-screen acknowledgements dialog. Shows an inline GPL-3.0 viewer on request. */
@Composable
fun AcknowledgementsDialog(onDismiss: () -> Unit) {
    var showLicense by rememberSaveable { mutableStateOf(false) }
    Dialog(
            onDismissRequest = { if (showLicense) showLicense = false else onDismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .statusBarsPadding()
                                .navigationBarsPadding()
        ) {
            if (showLicense) {
                LicenseTextPane(onBack = { showLicense = false })
            } else {
                AcknowledgementsPane(onClose = onDismiss, onViewLicense = { showLicense = true })
            }
        }
    }
}

@Composable
private fun AcknowledgementsPane(onClose: () -> Unit, onViewLicense: () -> Unit) {
    val context = LocalContext.current
    FokusSettingsTopBar(
            titleText = stringResource(R.string.settings_acknowledgements_title),
            onNavigateBack = onClose,
            containerColor = MaterialTheme.colorScheme.background,
    )
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
                text = stringResource(R.string.about_ack_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        AckSection(R.string.about_ack_section_upstream, upstreamEntries, context)
        AckSection(R.string.about_ack_section_reference, referenceEntries, context)
        AckSection(R.string.about_ack_section_libraries, libraryEntries, context)
        SettingsDivider()
        SectionHeader(stringResource(R.string.about_ack_section_source))
        SettingsRow(
                label = stringResource(R.string.about_ack_source_title),
                subtitle = stringResource(R.string.about_ack_source_body),
                verticalPadding = 12.dp,
                onClick = { openUrl(context, SOURCE_URL) },
        )
        SettingsRow(
                label = stringResource(R.string.about_ack_view_gpl),
                verticalPadding = 12.dp,
                onClick = onViewLicense,
        )
        Text(
                text = stringResource(R.string.about_ack_warranty),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun AckSection(@StringRes headerRes: Int, entries: List<AckEntry>, context: Context) {
    SectionHeader(stringResource(headerRes))
    entries.forEach { entry ->
        SettingsRow(
                label = stringResource(entry.titleRes),
                subtitle = stringResource(entry.bodyRes),
                verticalPadding = 12.dp,
                onClick = { openUrl(context, entry.url) },
        )
    }
}

@Composable
private fun LicenseTextPane(onBack: () -> Unit) {
    val context = LocalContext.current
    val failed = stringResource(R.string.about_ack_gpl_load_failed)
    val text = remember { loadAsset(context, GPL_ASSET_PATH) ?: failed }
    FokusSettingsTopBar(
            titleText = stringResource(R.string.about_ack_gpl_title),
            onNavigateBack = onBack,
            containerColor = MaterialTheme.colorScheme.background,
    )
    Text(
            text = text,
            style =
                    MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                    ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                    Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private fun loadAsset(context: Context, path: String): String? =
        runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrNull()

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        )
    }
}
