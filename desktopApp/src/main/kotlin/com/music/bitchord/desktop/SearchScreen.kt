package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SearchScreen(state: DesktopState) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(state.searchQuery, state.audio) {
        if (state.audio != null && state.searchQuery.isNotBlank()) {
            state.searchLoading = true
            delay(350)
            state.audio?.search(state.searchQuery)
        } else {
            state.searchLoading = false
            state.liveResults = null
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)) {
        Text("Search", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.1).sp)
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().height(42.dp).background(Charcoal, RoundedCornerShape(10.dp))
                .focusOutline(RoundedCornerShape(10.dp)).padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, null, tint = Muted, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            BasicTextField(
                value = state.searchQuery,
                onValueChange = { state.searchQuery = it; state.liveResults = null },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.weight(1f).focusRequester(focusRequester)
                    .onKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) {
                            state.recordSearch()
                            true
                        } else false
                    },
                decorationBox = { inner ->
                    Box {
                        if (state.searchQuery.isBlank()) Text("Songs or artists", color = Muted, fontSize = 14.sp)
                        inner()
                    }
                },
            )
            if (state.searchQuery.isNotBlank()) {
                IconButton(onClick = { state.searchQuery = "" }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, "Clear search", tint = Muted, modifier = Modifier.size(17.dp))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        if (state.searchQuery.isBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent searches", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (state.recentSearches.isNotEmpty()) {
                    Text("Clear", color = Color.White, fontSize = 12.sp, modifier = Modifier.focusOutline().clickable { state.clearSearches() }.padding(8.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            if (state.recentSearches.isEmpty()) {
                Text("Search for a song or artist to begin.", color = Muted, fontSize = 13.sp)
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
                    items(state.recentSearches.toList()) { term ->
                        Row(
                            Modifier.fillMaxWidth().height(58.dp).focusOutline().clickable {
                                state.searchQuery = term
                                state.recordSearch(term)
                            }.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.History, null, tint = Muted, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(term, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { state.removeSearch(term) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Remove $term from recent searches", tint = Muted, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
            }
        } else {
            val results = state.searchResults
            Text(if (state.searchLoading) "Searching…" else if (results.isEmpty()) "No results" else "Songs · ${results.size} results", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (state.audioError != null) Text(state.audioError.orEmpty(), color = Red, fontSize = 13.sp)
            if (results.isEmpty() && !state.searchLoading && state.audioError == null) Text("No matching songs found.", color = Muted, fontSize = 13.sp)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(if (state.searchLoading && state.audio != null) emptyList() else results, key = { it.id }) { track ->
                    DesktopTrackRow(
                        track, state, results, "Search", showLike = true,
                        onSelected = {
                            state.recordSearch()
                            state.selectTrack(track, results, "Search")
                        },
                    )
                }
            }
        }
    }
}
