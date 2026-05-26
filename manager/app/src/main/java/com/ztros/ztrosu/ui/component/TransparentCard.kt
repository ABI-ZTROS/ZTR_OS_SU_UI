package com.ztros.ztrosu.ui.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ztros.ztrosu.ui.LocalCardTransparency

/**
 * ZTR_OS SU UI-Only Mode - Transparent Card Defaults
 * Provides card colors with transparency applied
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
