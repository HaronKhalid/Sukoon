package com.lu4p.fokuslauncher.lockscreen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.notes.BackgroundSwatches
import com.lu4p.fokuslauncher.notes.ChipRow
import com.lu4p.fokuslauncher.notes.Label
import com.lu4p.fokuslauncher.notes.NoteAlignment
import com.lu4p.fokuslauncher.notes.NoteEntity
import com.lu4p.fokuslauncher.notes.NoteRepository
import com.lu4p.fokuslauncher.notes.NoteStyles
import com.lu4p.fokuslauncher.notes.SwatchRow
import com.lu4p.fokuslauncher.typography.FontRegistry
import com.lu4p.fokuslauncher.ui.settings.FokusSettingsTopBar
import com.lu4p.fokuslauncher.wallpaper.BuiltInWallpaper
import com.lu4p.fokuslauncher.wallpaper.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LockMessage { APPLIED, NO_NOTE, NOT_ALLOWED, FAILED, NOTIF_DENIED }

@OptIn(FlowPreview::class)
@HiltViewModel
class LockNoteViewModel
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val controller: LockNoteController,
        private val prefs: LockNotePreferences,
        notesRepo: NoteRepository,
        private val wallpapers: WallpaperRepository,
        customFonts: com.lu4p.fokuslauncher.data.font.CustomFontStore,
) : ViewModel() {
    val config: StateFlow<LockNoteConfig> =
            prefs.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockNoteConfig.DEFAULT)
    val notes: StateFlow<List<NoteEntity>> =
            notesRepo.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preview = MutableStateFlow<Bitmap?>(null)
    val busy = MutableStateFlow(false)
    val messages = MutableSharedFlow<LockMessage>(extraBufferCapacity = 4)
    val importedFont: Pair<String, String>? =
            customFonts.readStoredFontDisplayLabel()?.let { label ->
                customFonts.activeFileName()?.let {
                    (com.lu4p.fokuslauncher.data.model.LauncherFontPreferences.CUSTOM_FONT_PREFIX + it) to label
                }
            }

    /** Phone aspect ratio (width / height) of the real screen. */
    val aspect: Float =
            context.resources.displayMetrics.let { it.widthPixels.toFloat() / it.heightPixels.coerceAtLeast(1) }

    init {
        viewModelScope.launch {
            combine(config, notes) { c, _ -> c }.debounce(120).collectLatest { c ->
                val w = PREVIEW_WIDTH
                val h = (w / aspect).toInt()
                preview.value = controller.render(c, w, h)
            }
        }
    }

    fun update(c: LockNoteConfig) {
        viewModelScope.launch { prefs.set(c) }
    }

    fun photoPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            wallpapers.saveReference(uri)?.let {
                prefs.set(config.value.copy(background = LockBackgroundKind.PHOTO, photoFile = it.name))
            }
        }
    }

    fun apply() {
        viewModelScope.launch {
            busy.value = true
            val r = controller.applyToLockScreen(config.value)
            busy.value = false
            messages.emit(
                    when (r) {
                        LockApplyResult.APPLIED -> LockMessage.APPLIED
                        LockApplyResult.NO_NOTE -> LockMessage.NO_NOTE
                        LockApplyResult.NOT_ALLOWED -> LockMessage.NOT_ALLOWED
                        LockApplyResult.FAILED -> LockMessage.FAILED
                    }
            )
        }
    }

    fun setNotification(on: Boolean) {
        viewModelScope.launch {
            var c = config.value
            if (on) {
                c = c.copy(notificationOn = true)
                prefs.set(c)
                if (!controller.postNotification(c)) {
                    prefs.set(c.copy(notificationOn = false, notificationOnLockScreen = false))
                    messages.emit(LockMessage.NOTIF_DENIED)
                }
            } else {
                controller.cancelNotification()
                prefs.set(c.copy(notificationOn = false, notificationOnLockScreen = false))
            }
        }
    }

    fun setNotificationOnLockScreen(on: Boolean) {
        viewModelScope.launch {
            val c = config.value.copy(notificationOnLockScreen = on)
            prefs.set(c)
            if (c.notificationOn) controller.postNotification(c)
        }
    }

    companion object {
        const val PREVIEW_WIDTH = 540
    }
}

@Composable
fun LockNoteScreen(
        onNavigateBack: () -> Unit,
        onOpenWallpaper: () -> Unit,
        modifier: Modifier = Modifier,
        backgroundScrim: Color = Color.Black,
        viewModel: LockNoteViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var ackDialog by remember { mutableStateOf(false) }

    val texts =
            mapOf(
                    LockMessage.APPLIED to stringResource(R.string.lock_result_applied),
                    LockMessage.NO_NOTE to stringResource(R.string.lock_result_no_note),
                    LockMessage.NOT_ALLOWED to stringResource(R.string.lock_result_not_allowed),
                    LockMessage.FAILED to stringResource(R.string.lock_result_failed),
                    LockMessage.NOTIF_DENIED to stringResource(R.string.lock_notif_denied),
            )
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, texts[it], Toast.LENGTH_LONG).show() }
    }

    val photoPicker =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { viewModel.photoPicked(it) }
    val permission =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                viewModel.setNotification(granted)
            }

    Column(modifier.fillMaxSize().background(backgroundScrim).navigationBarsPadding().testTag("lock_note_screen")) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.lock_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )
        Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                    stringResource(R.string.lock_honesty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
            )
            Label(stringResource(R.string.lock_preview_label))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val bmp = preview
                Box(
                        Modifier.widthIn(max = 220.dp)
                                .fillMaxWidth()
                                .aspectRatioOf(viewModel.aspect)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF111111))
                                .testTag("lock_preview")
                ) {
                    if (bmp != null) {
                        Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = stringResource(R.string.lock_preview_label),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                                stringResource(R.string.lock_no_notes),
                                color = Color.White,
                                modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            // Note
            Label(stringResource(R.string.lock_pick_note))
            val noteLabels =
                    listOf(stringResource(R.string.lock_note_auto)) +
                            notes.map { n ->
                                n.title.ifBlank { n.body.lineSequence().firstOrNull().orEmpty() }
                                        .ifBlank { stringResource(R.string.notes_untitled) }
                                        .take(24)
                            }
            val noteIdx = notes.indexOfFirst { it.id == config.noteId }.let { if (it < 0) 0 else it + 1 }
            ChipRow(noteLabels, noteIdx) { i ->
                viewModel.update(config.copy(noteId = if (i == 0) LockNoteConfig.NO_NOTE else notes[i - 1].id))
            }

            // Style
            Label(stringResource(R.string.lock_style_label))
            val styleLabels =
                    listOf(stringResource(R.string.lock_style_own)) +
                            NoteStyles.all.map { stringResource(it.labelRes) }
            val styleIdx = NoteStyles.all.indexOfFirst { it.id == config.styleId }.let { if (it < 0) 0 else it + 1 }
            ChipRow(styleLabels, styleIdx) { i ->
                viewModel.update(config.copy(styleId = if (i == 0) "" else NoteStyles.all[i - 1].id))
            }

            // Font
            Label(stringResource(R.string.lock_font_label))
            val fontOptions =
                    buildList {
                        add("" to stringResource(R.string.lock_font_from_style))
                        FontRegistry.bundledFonts.forEach { add(FontRegistry.storageValueFor(it) to it.label) }
                        add("sans-serif" to stringResource(R.string.typography_system_sans))
                        add("serif" to stringResource(R.string.typography_system_serif))
                        add("monospace" to stringResource(R.string.typography_system_monospace))
                        viewModel.importedFont?.let { add(it.first to it.second) }
                    }
            ChipRow(fontOptions.map { it.second }, fontOptions.indexOfFirst { it.first == config.fontFamily }) {
                viewModel.update(config.copy(fontFamily = fontOptions[it].first))
            }

            Label(stringResource(R.string.lock_alignment))
            ChipRow(
                    listOf(
                            stringResource(R.string.notes_align_start),
                            stringResource(R.string.notes_align_center),
                            stringResource(R.string.notes_align_end),
                    ),
                    config.alignment.ordinal,
            ) {
                viewModel.update(config.copy(alignment = NoteAlignment.entries[it]))
            }

            Label(stringResource(R.string.lock_position))
            ChipRow(
                    listOf(
                            stringResource(R.string.lock_pos_top),
                            stringResource(R.string.lock_pos_center),
                            stringResource(R.string.lock_pos_bottom),
                    ),
                    config.position.ordinal,
            ) {
                viewModel.update(config.copy(position = LockPosition.entries[it]))
            }

            Label(stringResource(R.string.lock_text_size, (config.scale * 100).toInt()))
            Slider(
                    value = config.scale,
                    onValueChange = { viewModel.update(config.copy(scale = it)) },
                    valueRange = 0.5f..2f,
            )

            // Background
            Label(stringResource(R.string.lock_background))
            ChipRow(
                    listOf(
                            stringResource(R.string.lock_bg_solid),
                            stringResource(R.string.lock_bg_gradient),
                            stringResource(R.string.lock_bg_backdrop),
                            stringResource(R.string.lock_bg_photo),
                    ),
                    config.background.ordinal,
            ) {
                viewModel.update(config.copy(background = LockBackgroundKind.entries[it]))
            }
            val swatches = BackgroundSwatches.drop(2)
            when (config.background) {
                LockBackgroundKind.SOLID -> {
                    Label(stringResource(R.string.lock_color_top))
                    SwatchRow(swatches, config.color1) { viewModel.update(config.copy(color1 = it)) }
                }
                LockBackgroundKind.GRADIENT -> {
                    Label(stringResource(R.string.lock_color_top))
                    SwatchRow(swatches, config.color1) { viewModel.update(config.copy(color1 = it)) }
                    Label(stringResource(R.string.lock_color_bottom))
                    SwatchRow(swatches, config.color2) { viewModel.update(config.copy(color2 = it)) }
                }
                LockBackgroundKind.BACKDROP -> {
                    val all = BuiltInWallpaper.entries
                    ChipRow(all.map { it.id.replaceFirstChar { c -> c.uppercase() } }, all.indexOfFirst { it.id == config.backdropId }) {
                        viewModel.update(config.copy(backdropId = all[it].id))
                    }
                }
                LockBackgroundKind.PHOTO -> {
                    OutlinedButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    ) {
                        Text(stringResource(R.string.lock_pick_photo))
                    }
                    if (config.photoFile.isBlank()) {
                        Text(
                                stringResource(R.string.lock_photo_none),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            ToggleRow(
                    stringResource(R.string.lock_clock_line),
                    stringResource(R.string.lock_clock_line_hint),
                    config.showClockLine,
            ) {
                viewModel.update(config.copy(showClockLine = it))
            }

            // Apply
            Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = viewModel::apply, enabled = !busy, modifier = Modifier.testTag("lock_set")) {
                    Text(stringResource(R.string.lock_set))
                }
                OutlinedButton(onClick = viewModel::apply, enabled = !busy, modifier = Modifier.testTag("lock_update")) {
                    Text(stringResource(R.string.lock_update))
                }
            }
            ToggleRow(
                    stringResource(R.string.lock_auto_update),
                    stringResource(R.string.lock_auto_update_hint),
                    config.autoUpdate,
            ) {
                viewModel.update(config.copy(autoUpdate = it))
            }
            TextButton(onClick = onOpenWallpaper) { Text(stringResource(R.string.lock_choose_other)) }
            Text(
                    stringResource(R.string.lock_choose_other_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
            )

            // Notification
            Label(stringResource(R.string.lock_notif_section))
            ToggleRow(
                    stringResource(R.string.lock_notif_switch),
                    stringResource(R.string.lock_notif_desc),
                    config.notificationOn,
            ) { on ->
                if (on && !hasNotificationPermission(context)) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setNotification(on)
                }
            }
            if (config.notificationOn) {
                ToggleRow(stringResource(R.string.lock_notif_lock_switch), null, config.notificationOnLockScreen) { on ->
                    if (on) ackDialog = true else viewModel.setNotificationOnLockScreen(false)
                }
            }
        }
    }
    if (ackDialog) {
        AlertDialog(
                onDismissRequest = { ackDialog = false },
                title = { Text(stringResource(R.string.lock_notif_ack_title)) },
                text = { Text(stringResource(R.string.lock_notif_ack_body)) },
                confirmButton = {
                    TextButton(
                            onClick = {
                                ackDialog = false
                                viewModel.setNotificationOnLockScreen(true)
                            }
                    ) {
                        Text(stringResource(R.string.lock_notif_ack_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { ackDialog = false }) { Text(stringResource(R.string.lock_notif_cancel)) }
                },
        )
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED

@Composable
private fun ToggleRow(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun Modifier.aspectRatioOf(ratio: Float): Modifier =
        this.then(Modifier.aspectRatio(ratio.coerceIn(0.3f, 1f)))
