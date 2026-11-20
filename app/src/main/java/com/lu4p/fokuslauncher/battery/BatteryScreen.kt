package com.lu4p.fokuslauncher.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.settings.FokusSettingsTopBar
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import com.lu4p.fokuslauncher.usage.UsageStatsHelper
import com.lu4p.fokuslauncher.usage.formatScreenTimeDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BatteryViewModel @Inject constructor(private val repository: BatteryRepository) : ViewModel() {
    val history: StateFlow<List<LevelSample>> =
            repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val profile: StateFlow<PowerProfile> =
            repository.profile.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    PowerProfile.BALANCED,
            )

    fun record(reading: BatteryReading) {
        val level = reading.percent ?: return
        val charging = reading.chargeState == ChargeState.CHARGING
        viewModelScope.launch {
            repository.record(LevelSample(System.currentTimeMillis(), level, charging))
        }
    }

    fun clearHistory() = viewModelScope.launch { repository.clearHistory() }

    fun setProfile(p: PowerProfile) = viewModelScope.launch { repository.setProfile(p) }
}

@Composable
fun BatterySettingsScreen(
        onNavigateBack: () -> Unit,
        modifier: Modifier = Modifier,
        viewModel: BatteryViewModel = hiltViewModel(),
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val history by viewModel.history.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var reading by remember { mutableStateOf<BatteryReading?>(null) }
    var saverOn by remember { mutableStateOf<Boolean?>(null) }
    var usage by remember { mutableStateOf<List<AppScreenTime>?>(null) }
    var usageGranted by remember { mutableStateOf(false) }
    var showUsageDialog by remember { mutableStateOf(false) }

    // Receiver is registered only while the screen is visible (STARTED), never in a service.
    DisposableEffect(lifecycleOwner) {
        var receiver: BroadcastReceiver? = null
        fun refresh() {
            saverOn = BatteryReader.isBatterySaverOn(context)
            usageGranted = UsageStatsHelper.hasUsageAccess(context)
            usage = if (usageGranted) AppScreenTimeReader.query(context) else null
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                val r =
                        object : BroadcastReceiver() {
                            override fun onReceive(c: Context?, intent: Intent?) {
                                intent ?: return
                                val parsed = BatteryReader.parse(intent)
                                reading = parsed
                                viewModel.record(parsed)
                            }
                        }
                receiver = r
                ContextCompat.registerReceiver(
                        context,
                        r,
                        BatteryReader.filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                refresh()
            } else if (event == Lifecycle.Event.ON_STOP) {
                receiver?.let { context.unregisterReceiver(it) }
                receiver = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            receiver?.let { context.unregisterReceiver(it) }
        }
    }

    Column(
            modifier.fillMaxSize()
                    .background(backgroundScrim)
                    .navigationBarsPadding()
                    .testTag("battery_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.battery_settings_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )
        Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Note(stringResource(R.string.battery_honesty_note))
            val r = reading
            val na = stringResource(R.string.battery_not_available)
            ValueRow(
                    R.string.battery_label_level,
                    r?.percent?.let { stringResource(R.string.battery_value_percent, it) } ?: na,
            )
            ValueRow(
                    R.string.battery_label_state,
                    r?.chargeState?.let { stringResource(stateRes(it)) } ?: na,
            )
            ValueRow(R.string.battery_label_plug, r?.plug?.let { stringResource(plugRes(it)) } ?: na)
            ValueRow(
                    R.string.battery_label_temperature,
                    r?.temperatureC?.let { stringResource(R.string.battery_value_temperature, it) } ?: na,
            )
            ValueRow(
                    R.string.battery_label_voltage,
                    r?.voltageMv?.let { stringResource(R.string.battery_value_voltage, it) } ?: na,
            )
            ValueRow(R.string.battery_label_technology, r?.technology ?: na)
            ValueRow(
                    R.string.battery_label_health,
                    r?.health?.let { stringResource(healthRes(it)) } ?: na,
            )

            val recs = r?.let { BatteryRecommendations.evaluate(it, saverOn) }.orEmpty()
            if (recs.isNotEmpty()) {
                Section(R.string.battery_recs_title)
                recs.forEach { Text(stringResource(recRes(it)), style = MaterialTheme.typography.bodyMedium) }
            }

            Section(R.string.battery_history_title)
            Note(stringResource(R.string.battery_history_note, BatteryHistory.MAX_SAMPLES))
            if (history.size < 2) {
                Text(
                        stringResource(R.string.battery_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                TrendLine(history)
                Text(
                        stringResource(R.string.battery_history_count, history.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                )
            }
            TextButton(onClick = { viewModel.clearHistory() }, enabled = history.isNotEmpty()) {
                Text(stringResource(R.string.battery_history_clear))
            }

            Section(R.string.battery_usage_title)
            Note(stringResource(R.string.battery_usage_why))
            if (!usageGranted) {
                Text(
                        stringResource(R.string.battery_usage_disabled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                )
                TextButton(onClick = { showUsageDialog = true }) {
                    Text(stringResource(R.string.battery_usage_grant))
                }
            } else {
                Note(stringResource(R.string.battery_usage_last24))
                val list = usage.orEmpty()
                if (list.isEmpty()) {
                    Text(stringResource(R.string.battery_usage_none), style = MaterialTheme.typography.bodyMedium)
                }
                list.forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it.label, style = MaterialTheme.typography.bodyMedium)
                        Text(formatScreenTimeDuration(it.foregroundMs), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Section(R.string.battery_profile_title)
            Note(stringResource(R.string.battery_profile_note))
            ProfileOption(profile == PowerProfile.BALANCED, R.string.battery_profile_balanced, R.string.battery_profile_balanced_desc) {
                viewModel.setProfile(PowerProfile.BALANCED)
            }
            ProfileOption(profile == PowerProfile.POWER_SAVER, R.string.battery_profile_saver, R.string.battery_profile_saver_desc) {
                viewModel.setProfile(PowerProfile.POWER_SAVER)
            }
            ProfileOption(profile == PowerProfile.EXTREME_FOCUS, R.string.battery_profile_extreme, R.string.battery_profile_extreme_desc) {
                viewModel.setProfile(PowerProfile.EXTREME_FOCUS)
            }
            TextButton(
                    onClick = {
                        try {
                            context.startActivity(
                                    Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {}
                    }
            ) {
                Text(stringResource(R.string.battery_open_saver_settings))
            }
            Column(Modifier.height(24.dp)) {}
        }
    }

    if (showUsageDialog) {
        AlertDialog(
                onDismissRequest = { showUsageDialog = false },
                title = { Text(stringResource(R.string.battery_usage_dialog_title)) },
                text = { Text(stringResource(R.string.battery_usage_dialog_body)) },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showUsageDialog = false
                                UsageStatsHelper.openUsageAccessSettings(context)
                            }
                    ) { Text(stringResource(R.string.battery_usage_continue)) }
                },
                dismissButton = {
                    TextButton(onClick = { showUsageDialog = false }) {
                        Text(stringResource(R.string.battery_usage_cancel))
                    }
                },
        )
    }
}

@Composable
private fun Note(text: String) =
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)

@Composable
private fun Section(res: Int) =
        Text(
                stringResource(res),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 18.dp),
        )

@Composable
private fun ValueRow(label: Int, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ProfileOption(selected: Boolean, title: Int, desc: Int, onSelect: () -> Unit) {
    Row(
            Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onSelect),
            verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Note(stringResource(desc))
        }
    }
}

@Composable
private fun TrendLine(samples: List<LevelSample>) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val t0 = samples.first().timestampMs
        val span = (samples.last().timestampMs - t0).coerceAtLeast(1L).toFloat()
        listOf(0f, 0.5f, 1f).forEach {
            drawLine(grid.copy(alpha = 0.3f), Offset(0f, size.height * it), Offset(size.width, size.height * it))
        }
        val path = Path()
        samples.forEachIndexed { i, s ->
            val x = (s.timestampMs - t0) / span * size.width
            val y = size.height * (1f - s.level / 100f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, line, style = Stroke(width = 3.dp.toPx()))
    }
}

private fun stateRes(s: ChargeState) =
        when (s) {
            ChargeState.CHARGING -> R.string.battery_state_charging
            ChargeState.DISCHARGING -> R.string.battery_state_discharging
            ChargeState.FULL -> R.string.battery_state_full
            ChargeState.NOT_CHARGING -> R.string.battery_state_not_charging
        }

private fun plugRes(p: PlugType) =
        when (p) {
            PlugType.NONE -> R.string.battery_plug_none
            PlugType.AC -> R.string.battery_plug_ac
            PlugType.USB -> R.string.battery_plug_usb
            PlugType.WIRELESS -> R.string.battery_plug_wireless
            PlugType.DOCK -> R.string.battery_plug_dock
            PlugType.OTHER -> R.string.battery_plug_other
        }

private fun healthRes(h: HealthFlag) =
        when (h) {
            HealthFlag.GOOD -> R.string.battery_health_good
            HealthFlag.OVERHEAT -> R.string.battery_health_overheat
            HealthFlag.DEAD -> R.string.battery_health_dead
            HealthFlag.OVER_VOLTAGE -> R.string.battery_health_over_voltage
            HealthFlag.FAILURE -> R.string.battery_health_failure
            HealthFlag.COLD -> R.string.battery_health_cold
        }

private fun recRes(r: Recommendation) =
        when (r) {
            Recommendation.WARM -> R.string.battery_rec_warm
            Recommendation.HOT -> R.string.battery_rec_hot
            Recommendation.HEALTH_FLAG -> R.string.battery_rec_health
            Recommendation.LOW_SAVER_OFF -> R.string.battery_rec_low_saver
        }
