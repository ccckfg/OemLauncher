package com.n2.oemlauncher.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.config.UiDimensions

private val helpTopics = listOf(
    R.string.help_install to R.string.help_install_body,
    R.string.help_home to R.string.help_home_body,
    R.string.help_pair to R.string.help_pair_body,
    R.string.help_offline to R.string.help_offline_body,
    R.string.help_conflict to R.string.help_conflict_body,
    R.string.help_icons to R.string.help_icons_body,
    R.string.help_privacy to R.string.help_privacy_body,
)

@Composable
fun HelpScreen() {
    LazyColumn(
        Modifier.padding(horizontal = UiDimensions.PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(UiDimensions.PAGE_PADDING.dp),
    ) {
        helpTopics.forEach { (title, body) -> item(key = title) {
            Column(verticalArrangement = Arrangement.spacedBy(UiDimensions.SMALL_GAP.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        } }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(UiDimensions.GAP.dp)) }
    }
}
