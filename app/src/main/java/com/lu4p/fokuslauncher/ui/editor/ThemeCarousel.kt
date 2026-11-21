package com.lu4p.fokuslauncher.ui.editor

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.theme.BuiltInThemes
import com.lu4p.fokuslauncher.theme.ThemeColorMath
import com.lu4p.fokuslauncher.theme.ThemeDefinition
import com.lu4p.fokuslauncher.ui.settings.HomePreviewCard
import com.lu4p.fokuslauncher.ui.settings.StyleGalleryViewModel
import kotlin.math.abs

/**
 * Full-screen swipeable carousel of whole-phone theme previews (theme + backdrop + clock face +
 * layout, drawn by [HomePreviewCard]). The screen background and the Back/Apply buttons take the
 * accent of the page being shown. Apply goes through [StyleGalleryViewModel.apply].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThemeCarousel(
        viewModel: StyleGalleryViewModel,
        onBack: () -> Unit,
        onCustomize: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val classicName = stringResource(R.string.theme_studio_classic_name)
    val items = remember(state.customThemes) { BuiltInThemes.gallery + state.customThemes + BuiltInThemes.ClassicPreview }
    val appliedId = if (state.isClassic) BuiltInThemes.ID_CLASSIC else state.applied?.id
    val initial = remember { items.indexOfFirst { it.id == appliedId }.coerceAtLeast(0) }
    val pager = rememberPagerState(initialPage = initial) { items.size }
    val page = pager.currentPage.coerceIn(0, items.lastIndex)
    val theme = items[page]
    val isClassic = theme.id == BuiltInThemes.ID_CLASSIC
    val name = if (isClassic) classicName else theme.name
    val isApplied = theme.id == appliedId

    val bg by animateColorAsState(Color(theme.representativeBackground), label = "carousel-bg")
    val accent by animateColorAsState(Color(theme.colors.accent), label = "carousel-accent")
    val onAccent = if (ThemeColorMath.luminance(accent.toArgb()) > 0.4) Color(0xFF111111) else Color.White
    val text by animateColorAsState(Color(theme.colors.primaryText), label = "carousel-text")
    val secondary by animateColorAsState(Color(theme.colors.secondaryText), label = "carousel-secondary")

    Column(modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding().testTag("theme_carousel")) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val pageWidth = minOf(maxWidth * 0.72f, maxHeight * 0.5f - 8.dp)
            val side = (maxWidth - pageWidth) / 2
            HorizontalPager(
                    state = pager,
                    pageSize = PageSize.Fixed(pageWidth),
                    contentPadding = PaddingValues(horizontal = side),
                    pageSpacing = 14.dp,
                    beyondViewportPageCount = 1,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                    key = { items[it].id },
            ) { index ->
                val t = items[index]
                val distance = abs((pager.currentPage - index) + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
                HomePreviewCard(
                        theme = t,
                        selected = t.id == appliedId,
                        cornerRadius = 28.dp,
                        modifier =
                                Modifier.width(pageWidth).height(pageWidth * 2f).graphicsLayer {
                                    val scale = 1f - 0.1f * distance
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - 0.45f * distance
                                }.testTag("carousel_page_${t.id}"),
                )
            }
        }
        Text(name, style = MaterialTheme.typography.titleMedium, color = text, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Text(stringResource(R.string.carousel_position, page + 1, items.size), style = MaterialTheme.typography.labelSmall,
                color = secondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, border = BorderStroke(1.dp, accent), modifier = Modifier.weight(1f).testTag("carousel_back")) {
                Text(stringResource(R.string.carousel_back), color = accent)
            }
            TextButton(onClick = onCustomize, modifier = Modifier.testTag("carousel_customize")) {
                Text(stringResource(R.string.carousel_customize), color = accent)
            }
            Button(
                    onClick = {
                        val done = {
                            Toast.makeText(context, context.getString(R.string.style_gallery_applied_toast, name), Toast.LENGTH_SHORT).show()
                        }
                        if (isClassic) viewModel.applyClassic(done) else viewModel.apply(theme, done)
                    },
                    enabled = !isApplied,
                    colors = ButtonDefaults.buttonColors(
                            containerColor = accent, contentColor = onAccent,
                            disabledContainerColor = accent.copy(alpha = 0.35f), disabledContentColor = onAccent.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1.4f).testTag("carousel_apply"),
            ) {
                Text(stringResource(if (isApplied) R.string.carousel_applied else R.string.carousel_apply))
            }
        }
    }
}
