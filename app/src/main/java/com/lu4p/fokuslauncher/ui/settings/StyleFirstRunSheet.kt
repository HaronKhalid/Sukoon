package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.FokusTextButton

/** One-time optional "Choose your style" sheet shown after onboarding. Applying or skipping dismisses it for good. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleFirstRunSheet(viewModel: StyleGalleryViewModel = hiltViewModel()) {
    val seen by viewModel.stylePromptSeen.collectAsStateWithLifecycle()
    if (seen != false) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
            onDismissRequest = { viewModel.markPromptSeen() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.testTag("style_first_run_sheet"),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                    stringResource(R.string.style_first_run_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                    stringResource(R.string.style_first_run_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            StyleGalleryContent(
                    viewModel = viewModel,
                    modifier = Modifier.height(500.dp),
                    onApplied = { viewModel.markPromptSeen() },
            )
            FokusTextButton(
                    onClick = { viewModel.markPromptSeen() },
                    modifier = Modifier.padding(horizontal = 16.dp).testTag("style_first_run_skip"),
            ) {
                Text(stringResource(R.string.style_first_run_skip), color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}
