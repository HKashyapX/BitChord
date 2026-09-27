package com.music.bitchord.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HomeScreen(state: DesktopState) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cardsAcross = if (maxWidth < 650.dp) 3 else 5
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item { Text("Home", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.1).sp) }
            if (state.homeLoading && state.homeShelves.isEmpty()) {
                item {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = Color.White) }
                }
            } else if (state.homeError != null && state.homeShelves.isEmpty()) {
                item {
                    Text("Home could not be loaded", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(state.homeError.orEmpty(), color = Muted, fontSize = 13.sp)
                    TextButton(onClick = state::refreshHome) { Text("Try again") }
                }
            } else {
                state.homeShelves.forEachIndexed { index, shelf ->
                    item(key = "$index-${shelf.title}") {
                        SectionTitle(shelf.title)
                        Spacer(Modifier.height(12.dp))
                        CoverShelf(shelf.tracks, state, cardsAcross, shelf.title)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
