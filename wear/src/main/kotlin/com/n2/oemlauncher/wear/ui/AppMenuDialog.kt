package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.wear.R
import androidx.compose.ui.res.stringResource

@Composable
fun AppMenuDialog(state: LauncherUiState, viewModel: LauncherViewModel, key: String) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f))) {
        Column(Modifier.align(androidx.compose.ui.Alignment.Center).width(160.dp).background(Color(0xFF122033), RoundedCornerShape(20.dp)).padding(12.dp)) {
            Text(state.entries.firstOrNull { it.key == key }?.label.orEmpty(), color = Color.White, fontSize = 14.sp)
            MenuAction(stringResource(R.string.favorite_toggle)) { viewModel.toggleFavorite(key); viewModel.showMenu(null) }
            MenuAction(stringResource(R.string.hide_app)) { viewModel.toggleHidden(key); viewModel.showMenu(null) }
            MenuAction(stringResource(R.string.app_info)) { viewModel.openAppInfo(key) }
            MenuAction(stringResource(R.string.cancel)) { viewModel.showMenu(null) }
        }
    }
}

@Composable
private fun MenuAction(label: String, onClick: () -> Unit) {
    Text(label, Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 4.dp), color = Color.White, fontSize = 12.sp)
}
