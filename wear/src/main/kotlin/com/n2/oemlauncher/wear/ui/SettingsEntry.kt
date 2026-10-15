package com.n2.oemlauncher.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.n2.oemlauncher.wear.R

/**
 * Settings entry for the launcher root. A labelled capsule instead of a bare gear icon, so it
 * can never be mistaken for an app; kept in the bottom band of the round screen.
 */
@Composable
fun SettingsEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.settings)
    Row(
        modifier.heightIn(min = 48.dp).background(Color(0xFF22344F), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick).semantics { contentDescription = label }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⚙", fontSize = 14.sp, color = Color(0xFF64D8FF))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = Color.White, maxLines = 1)
    }
}

/**
 * Settings entry as the last row of a scrolling layout (grid and list). Being part of the
 * content, it can never cover an icon or a label at any scroll position.
 */
@Composable
fun SettingsRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        SettingsEntry(onClick = onClick)
    }
}
