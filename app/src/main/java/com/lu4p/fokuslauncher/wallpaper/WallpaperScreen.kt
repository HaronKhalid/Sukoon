package com.lu4p.fokuslauncher.wallpaper

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.theme.themedSettingsScrim
import com.lu4p.fokuslauncher.ui.settings.FokusSettingsTopBar
import com.lu4p.fokuslauncher.ui.settings.components.SectionHeader
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow

@Composable
fun WallpaperScreen(
    viewModel: WallpaperViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    backgroundScrim: Color = themedSettingsScrim,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { viewModel.picked(it) }

    val messageText =
        state.message?.let {
            stringResource(
                when (it) {
                    WallpaperMessage.APPLIED_HOME -> R.string.wallpaper_applied_home
                    WallpaperMessage.APPLIED_HOME_LOCK -> R.string.wallpaper_applied_home_lock
                    WallpaperMessage.LOCK_FAILED -> R.string.wallpaper_lock_failed
                    WallpaperMessage.FAILED -> R.string.wallpaper_failed
                    WallpaperMessage.LOAD_FAILED -> R.string.wallpaper_load_failed
                }
            )
        }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            Toast.makeText(context, messageText, Toast.LENGTH_LONG).show()
            viewModel.consumeMessage()
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(backgroundScrim)
            .navigationBarsPadding()
            .testTag("wallpaper_screen")
    ) {
        FokusSettingsTopBar(
            titleText = stringResource(R.string.wallpaper_title),
            onNavigateBack = onNavigateBack,
            containerColor = MaterialTheme.colorScheme.background,
        )
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                SettingsRow(
                    label = stringResource(R.string.wallpaper_pick),
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                )
            }
            state.preview?.let { bmp ->
                item {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        val m = context.resources.displayMetrics
                        Box(Modifier.height(280.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            // Crop scale + screen aspect == what apply() writes.
                            Image(
                                bitmap = remember(bmp) { bmp.asImageBitmap() },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier.height(280.dp)
                                        .aspectRatio(m.widthPixels.toFloat() / m.heightPixels)
                                        .clip(RoundedCornerShape(16.dp)),
                            )
                        }
                        Text(
                            stringResource(R.string.wallpaper_preview_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        state.averageColor?.let { c ->
                            Text(
                                stringResource(
                                    R.string.wallpaper_average_color,
                                    "#%06X".format(c and 0xFFFFFF),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                item {
                    SettingsRow(
                        label = stringResource(R.string.wallpaper_also_lock),
                        subtitle = stringResource(R.string.wallpaper_also_lock_hint),
                        onClick = { viewModel.setApplyLock(!state.applyLock) },
                        trailing = {
                            Checkbox(
                                checked = state.applyLock,
                                onCheckedChange = { viewModel.setApplyLock(it) },
                            )
                        },
                    )
                }
                item {
                    SettingsRow(
                        label = stringResource(R.string.wallpaper_set_home),
                        onClick = if (state.busy) null else ({ viewModel.apply() }),
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.wallpaper_section_builtin)) }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    items(BuiltInWallpaper.entries) { w ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(width = 64.dp, height = 112.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.verticalGradient(listOf(Color(w.top), Color(w.bottom))))
                                    .clickable { viewModel.selectBuiltIn(w) }
                            )
                            Text(
                                stringResource(w.labelRes()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.wallpaper_section_recent)) }
            if (state.recents.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.wallpaper_recent_empty),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            }
            items(state.recents, key = { it.name }) { f ->
                SettingsRow(
                    label =
                        java.text.DateFormat.getDateTimeInstance().format(java.util.Date(f.lastModified())),
                    onClick = { viewModel.selectRecent(f) },
                    trailing = {
                        Text(
                            stringResource(R.string.wallpaper_remove),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { viewModel.deleteRecent(f) }.padding(8.dp),
                        )
                    },
                )
            }
        }
    }
}

private fun BuiltInWallpaper.labelRes(): Int =
    when (this) {
        BuiltInWallpaper.PURE_BLACK -> R.string.wallpaper_builtin_black
        BuiltInWallpaper.MIDNIGHT -> R.string.wallpaper_builtin_midnight
        BuiltInWallpaper.EMBER -> R.string.wallpaper_builtin_ember
        BuiltInWallpaper.FOREST -> R.string.wallpaper_builtin_forest
        BuiltInWallpaper.DUSK -> R.string.wallpaper_builtin_dusk
    }
