package com.music.bitchord.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            item { Text("Listen Now", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.1).sp) }
            item {
                SectionTitle("Recents")
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    mockTracks.chunked(4).forEach { column ->
                        Column(Modifier.weight(1f)) {
                            column.forEach { track ->
                                DesktopTrackRow(track, state, mockTracks, "Listen Now", compact = true)
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle("Listen again", "YOUR MUSIC")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    CoverCard("Liked Music", "Auto playlist", null, Modifier.weight(1f)) {
                        state.navigate(Destination.LIKED_MUSIC)
                    }
                    mockTracks.take(cardsAcross - 1).forEach { track ->
                        CoverCard(track.title, track.artist, track, Modifier.weight(1f)) {
                            state.selectTrack(track, mockTracks, "Listen again")
                        }
                    }
                }
            }
            item {
                SectionTitle("Quick picks")
                Spacer(Modifier.height(12.dp))
                CoverShelf(mockTracks.drop(2), state, cardsAcross, "Quick picks")
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
