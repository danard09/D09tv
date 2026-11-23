package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RoyalPurple

@Composable
fun tvFocusBorderColor(isFocused: Boolean, unfocusedColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)): Color {
    val color by animateColorAsState(
        targetValue = if (isFocused) ElectricBlue else unfocusedColor,
        animationSpec = tween(160),
        label = "tv_focus_border",
    )
    return color
}

@Composable
fun TvFocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    focusedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedBorderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    focusScale: Float = 1.035f,
    borderWidth: Dp = 2.5.dp,
    enabled: Boolean = true,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val highlighted = isFocused || isHovered

    val scale by animateFloatAsState(
        targetValue = if (highlighted) focusScale else 1f,
        animationSpec = tween(150),
        label = "tv_focus_scale",
    )

    val bg by animateColorAsState(
        targetValue = if (highlighted) focusedContainerColor else containerColor,
        animationSpec = tween(150),
        label = "tv_focus_bg",
    )

    val borderBrush = if (highlighted) {
        Brush.linearGradient(listOf(ElectricBlue, RoyalPurple))
    } else {
        Brush.linearGradient(listOf(unfocusedBorderColor, unfocusedBorderColor))
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .scale(scale)
            .clip(shape)
            .background(bg, shape)
            .border(
                border = BorderStroke(
                    width = if (highlighted) borderWidth else 1.dp,
                    brush = borderBrush,
                ),
                shape = shape,
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .onPreviewKeyEvent { keyEvent ->
                if (enabled && keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.DirectionCenter ||
                        keyEvent.key == Key.Enter ||
                        keyEvent.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        content(highlighted)
    }
}
