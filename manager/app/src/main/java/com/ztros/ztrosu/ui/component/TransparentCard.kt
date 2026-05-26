package com.ztros.ztrosu.ui.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ztros.ztrosu.ui.LocalCardTransparency

/**
 * ZTR_OS SU UI-Only Mode - Transparent Card Component
 * Applies transparency setting to all cards using LocalCardTransparency
 */
@Composable
fun TransparentCard(
    modifier: Modifier = Modifier,
    shape: Shapes = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    content: @Composable BoxScope.() -> Unit
) {
    val transparency = LocalCardTransparency.current
    
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = colors.containerColor.copy(alpha = transparency)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation.defaultElevation,
            pressedElevation = elevation.pressedElevation,
            focusedElevation = elevation.focusedElevation,
            hoveredElevation = elevation.hoveredElevation,
            draggedElevation = elevation.draggedElevation,
            disabledElevation = elevation.disabledElevation
        ),
        content = content
    )
}

/**
 * Alternative: Use CardDefaults directly with transparency applied
 */
object TransparentCardDefaults {
    @Composable
    fun cardColors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ): CardColors {
        val transparency = LocalCardTransparency.current
        return CardDefaults.cardColors(
            containerColor = containerColor.copy(alpha = transparency),
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor.copy(alpha = transparency),
            disabledContentColor = disabledContentColor
        )
    }
}
