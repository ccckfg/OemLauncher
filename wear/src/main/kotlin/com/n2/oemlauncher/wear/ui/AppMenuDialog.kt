package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.wear.R

/**
 * Long-press menu. The actions sit in a 2x2 grid: a single column of five 48dp rows needs more
 * height than a 227dp round screen has at any usable width, so it would run under the bezel.
 * The 160dp width keeps every corner of the sheet inside the circle's safe area.
 */
@Composable
fun AppMenuDialog(state: LauncherUiState, viewModel: LauncherViewModel, key: String) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f))) {
        Column(
            Modifier.align(Alignment.Center).width(160.dp)
                .background(Color(0xFF122033), RoundedCornerShape(20.dp)).padding(10.dp),
        ) {
            Text(
                state.entries.firstOrNull { it.key == key }?.label.orEmpty(),
                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MenuAction(stringResource(R.string.favorite_toggle), Modifier.weight(1f)) {
                    viewModel.toggleFavorite(key)
                    viewModel.showMenu(null)
                }
                MenuAction(stringResource(R.string.hide_app), Modifier.weight(1f)) {
                    viewModel.toggleHidden(key)
                    viewModel.showMenu(null)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MenuAction(stringResource(R.string.app_info), Modifier.weight(1f)) { viewModel.openAppInfo(key) }
                MenuAction(stringResource(R.string.cancel), Modifier.weight(1f)) { viewModel.showMenu(null) }
            }
        }
    }
}

@Composable
private fun MenuAction(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp)
            .background(Color(0xFF22344F), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
