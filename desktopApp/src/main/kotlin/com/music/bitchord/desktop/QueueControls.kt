package com.music.bitchord.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun QueueModeControls(state: DesktopState) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = state::toggleShuffle, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Shuffle, if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                tint = if (state.shuffleEnabled) Red else Color.White, modifier = Modifier.size(19.dp))
        }
        IconButton(onClick = state::cycleRepeat, modifier = Modifier.size(34.dp)) {
            Icon(if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                when (state.repeatMode) {
                    RepeatMode.OFF -> "Repeat off; click for repeat all"
                    RepeatMode.ALL -> "Repeat all; click for repeat one"
                    RepeatMode.ONE -> "Repeat one; click to turn off"
                }, tint = if (state.repeatMode == RepeatMode.OFF) Color.White else Red,
                modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
internal fun QueueMoveButtons(state: DesktopState, index: Int) {
    IconButton(onClick = { state.moveUpcoming(index, index - 1) },
        enabled = index > state.currentIndex + 1, modifier = Modifier.size(25.dp)) {
        Icon(Icons.Default.ArrowUpward, "Move up", modifier = Modifier.size(17.dp))
    }
    IconButton(onClick = { state.moveUpcoming(index, index + 1) },
        enabled = index < state.queue.lastIndex, modifier = Modifier.size(25.dp)) {
        Icon(Icons.Default.ArrowDownward, "Move down", modifier = Modifier.size(17.dp))
    }
}
