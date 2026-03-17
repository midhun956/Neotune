package com.example.neotune

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun YouTubeMusicSeekBar(
        value: Float,
        onValueChange: (Float) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
        steps: Int = 0,
        onValueChangeFinished: (() -> Unit)? = null,
        bufferedProgress: Float = 0f,
        isBuffering: Boolean = false,
        duration: Long = 0L
) {
        var containerWidth by remember { mutableStateOf(0f) }

        val animatedValue by
                animateFloatAsState(
                        targetValue = value,
                        animationSpec = spring(), // A simple spring animation is fine
                        label = "seekBarValue"
                )

        val trackHeight = 4.dp
        val thumbSize = 16.dp

        Box(
                modifier =
                        modifier.height(32.dp)
                                .onSizeChanged { size -> containerWidth = size.width.toFloat() }
                                .pointerInput(Unit) {
                                        detectDragGestures(
                                                onDragStart = { offset ->
                                                        // This fires on first touch (for taps)
                                                        if (containerWidth > 0) {
                                                                val newValue =
                                                                        (offset.x / containerWidth)
                                                                                .coerceIn(0f, 1f)
                                                                onValueChange(newValue)
                                                        }
                                                },
                                                onDragEnd = {
                                                        // Fires when you lift your finger
                                                        onValueChangeFinished?.invoke()
                                                },
                                                onDrag = { change, dragAmount ->
                                                        // This fires as you move your finger
                                                        change.consume()
                                                        if (containerWidth > 0) {
                                                                // The key fix: Use the finger's
                                                                // absolute position
                                                                val newValue =
                                                                        (change.position.x /
                                                                                        containerWidth)
                                                                                .coerceIn(0f, 1f)
                                                                onValueChange(newValue)
                                                        }
                                                }
                                        )
                                }
                                .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                                if (containerWidth > 0) {
                                                        val newValue =
                                                                (offset.x / containerWidth)
                                                                        .coerceIn(0f, 1f)
                                                        onValueChange(newValue)
                                                        onValueChangeFinished?.invoke()
                                                }
                                        }
                                }
        ) {
                // Background track
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(trackHeight)
                                        .background(
                                                color = Color.Gray.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(trackHeight / 2)
                                        )
                                        .align(Alignment.CenterStart)
                )

                // Progress track
                Box(
                        modifier =
                                Modifier.fillMaxWidth(animatedValue)
                                        .height(trackHeight)
                                        .background(
                                                brush =
                                                        Brush.horizontalGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color(0xFF3B3BFF),
                                                                                Color(0xFF6B6BFF)
                                                                        )
                                                        ),
                                                shape = RoundedCornerShape(trackHeight / 2)
                                        )
                                        .align(Alignment.CenterStart)
                )

                // Thumb
                Box(
                        modifier =
                                Modifier.size(thumbSize)
                                        .offset {
                                                val thumbOffset =
                                                        (containerWidth * animatedValue -
                                                                        (thumbSize.toPx() / 2f))
                                                                .coerceIn(
                                                                        0f,
                                                                        containerWidth -
                                                                                thumbSize.toPx()
                                                                )
                                                IntOffset(thumbOffset.roundToInt(), 0)
                                        }
                                        .background(color = Color.White, shape = CircleShape)
                                        .shadow(
                                                elevation = 4.dp,
                                                shape = CircleShape,
                                                spotColor = Color(0xFF3B3BFF).copy(alpha = 0.3f)
                                        )
                                        .align(Alignment.CenterStart)
                ) {
                        // Inner progress indicator
                        Box(
                                modifier =
                                        Modifier.size(10.dp)
                                                .background(
                                                        color = Color(0xFF3B3BFF),
                                                        shape = CircleShape
                                                )
                                                .align(Alignment.Center)
                        )
                }
        }
}

@Composable
fun MiniPlayerSeekBar(value: Float, modifier: Modifier = Modifier) {
        val animatedValue by
                animateFloatAsState(
                        targetValue = value,
                        animationSpec = spring(), // A simple spring animation for smooth progress
                        label = "miniSeekBarValue"
                )

        val trackHeight = 2.dp

        Box(
                modifier =
                        modifier.height(trackHeight) // Removed the extra height for gestures
                                .fillMaxWidth() // The box should just fill the width
        ) {
                // Background track
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(trackHeight)
                                        .background(
                                                color = Color.Gray,
                                                shape = RoundedCornerShape(trackHeight / 2)
                                        )
                )

                // Progress track
                Box(
                        modifier =
                                Modifier.fillMaxWidth(animatedValue)
                                        .height(trackHeight)
                                        .background(
                                                color = Color(0xFF3B3BFF),
                                                shape = RoundedCornerShape(trackHeight / 2)
                                        )
                )
        }
}

@Composable
fun PlaylistCard(playlist: Playlist, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
        Box(
                modifier =
                        Modifier.size(160.dp)
                                .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(12.dp)
                                )
        ) {
                // The main clickable area of the card
                Box(
                        modifier = Modifier.fillMaxSize().clickable { onClick() }.padding(16.dp),
                        contentAlignment = Alignment.Center
                ) {
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                        ) {
                                if (playlist.icon != null) {
                                        Icon(
                                                imageVector = playlist.icon,
                                                contentDescription = playlist.name,
                                                tint =
                                                        if (playlist.isLiked) Color.Red
                                                        else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(48.dp)
                                        )
                                } else {
                                        Box(
                                                modifier =
                                                        Modifier.size(48.dp)
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .primary.copy(
                                                                                alpha = 0.2f
                                                                        ),
                                                                        RoundedCornerShape(8.dp)
                                                                ),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Text(
                                                        text = playlist.name.take(2).uppercase(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                        }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                )
                        }

                        if (playlist.icon == null && playlist.songCount > 0) {
                                Box(
                                        modifier =
                                                Modifier.align(Alignment.BottomEnd)
                                                        .background(
                                                                MaterialTheme.colorScheme.primary,
                                                                RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                        Text(
                                                text = "${playlist.songCount}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimary
                                        )
                                }
                        }
                }

                // Sibling Box for the Dropdown Menu, positioned on top of the clickable area
                if (onLongClick != null) {
                        var menuExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.padding(4.dp).size(24.dp)
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Playlist Options",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                        )
                                }

                                DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                ) {
                                        DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = {
                                                        menuExpanded = false
                                                        onLongClick()
                                                },
                                                leadingIcon = {
                                                        Icon(
                                                                Icons.Filled.Delete,
                                                                contentDescription = null
                                                        )
                                                }
                                        )
                                }
                        }
                }
        }
}
