package com.lu4p.fokuslauncher.ui.settings

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.theme.BuiltInThemes
import com.lu4p.fokuslauncher.theme.StyleFilter
import com.lu4p.fokuslauncher.theme.matches
import com.lu4p.fokuslauncher.theme.themedSettingsScrim

@Composable
fun StyleGalleryScreen(
        viewModel: StyleGalleryViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = themedSettingsScrim,
) {
    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .testTag("style_gallery_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.style_gallery_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )
        StyleGalleryContent(viewModel = viewModel, modifier = Modifier.weight(1f))
    }
}

/** Filter chips plus a swipeable pager of [HomePreviewCard]s; shared by the screen and the first-run sheet. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StyleGalleryContent(
        viewModel: StyleGalleryViewModel,
        modifier: Modifier = Modifier,
        onApplied: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(StyleFilter.ALL) }
    val custom = state.customThemes
    val classicName = stringResource(R.string.theme_studio_classic_name)
    val items =
            remember(filter, custom) {
                val filtered = (BuiltInThemes.gallery + custom).filter { it.matches(filter) }
                // The classic look is offered on the unfiltered and dark views.
                if (filter == StyleFilter.ALL || filter == StyleFilter.DARK) filtered + BuiltInThemes.ClassicPreview
                else filtered
            }
    val appliedId = if (state.isClassic) BuiltInThemes.ID_CLASSIC else state.applied?.id
    val pager = rememberPagerState { items.size.coerceAtLeast(1) }
    LaunchedEffect(filter) { pager.scrollToPage(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
                stringResource(R.string.style_gallery_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
        ) {
            items(StyleFilter.entries.toList(), key = { it.name }) { f ->
                FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(stringResource(filterLabel(f))) },
                        modifier = Modifier.testTag("style_filter_${f.name}"),
                )
            }
        }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                        stringResource(R.string.style_gallery_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                )
            }
        } else {
            HorizontalPager(
                    state = pager,
                    pageSize = PageSize.Fixed(250.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 16.dp,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
                    key = { items.getOrNull(it)?.id ?: it },
            ) { page ->
                val theme = items[page]
                val isClassic = theme.id == BuiltInThemes.ID_CLASSIC
                val selected = theme.id == appliedId
                val displayName = if (isClassic) classicName else theme.name
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .clickable {
                                            val done = {
                                                Toast.makeText(
                                                                context,
                                                                context.getString(R.string.style_gallery_applied_toast, displayName),
                                                                Toast.LENGTH_SHORT,
                                                        )
                                                        .show()
                                                onApplied()
                                            }
                                            if (isClassic) viewModel.applyClassic(done) else viewModel.apply(theme, done)
                                        }
                                        .testTag("style_card_${theme.id}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HomePreviewCard(
                            theme = theme,
                            selected = selected,
                            modifier = Modifier.width(250.dp).weight(1f),
                    )
                    Text(
                            displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                            if (selected) stringResource(R.string.style_gallery_applied)
                            else if (isClassic) stringResource(R.string.theme_studio_classic_subtitle) else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.height(18.dp),
                    )
                }
            }
        }
    }
}

private fun filterLabel(f: StyleFilter): Int =
        when (f) {
            StyleFilter.ALL -> R.string.style_filter_all
            StyleFilter.DARK -> R.string.style_filter_dark
            StyleFilter.LIGHT -> R.string.style_filter_light
            StyleFilter.COLOURFUL -> R.string.style_filter_colourful
            StyleFilter.MINIMAL -> R.string.style_filter_minimal
        }
