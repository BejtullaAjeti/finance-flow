package com.example.financeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.financeflow.ui.theme.Radius

/**
 * Full-bleed, fully opaque dialog shell — the design system bans transparency everywhere,
 * including modals (spec §5), so this replaces the platform's default translucent dim scrim
 * (which let the screen behind visibly show through) with a solid `background`-colored backdrop,
 * and zeroes out the window's own dim via [DialogWindowProvider] so the two don't stack. Tapping
 * the backdrop dismisses; the card itself swallows the click so it doesn't propagate to it.
 */
@Composable
fun OpaqueDialogSurface(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val lightBackdrop = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            // The host Activity draws edge-to-edge (enableEdgeToEdge()); without this the dialog
            // window defaults to shrinking around the IME instead, so the opaque backdrop below
            // only covers the shrunk window and the screen behind shows through under the
            // keyboard. imePadding() on the card (not the backdrop) below restores the "card
            // stays above the keyboard" behavior this used to get for free.
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setDimAmount(0f)
            window?.let {
                // The dialog is its own window (own status/nav bar icon appearance, not
                // inherited from the host Activity) — best-effort match so its icons don't
                // default to light-on-light against the solid backdrop.
                val controller = WindowCompat.getInsetsController(it, view)
                controller.isAppearanceLightStatusBars = lightBackdrop
                controller.isAppearanceLightNavigationBars = lightBackdrop
            }
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * Flat drop-in for M3's AlertDialog — Surface background, no border, no shadow, per the
 * minimalist design system's dialog policy (spec §6).
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OpaqueDialogSurface(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .clip(RoundedCornerShape(Radius.large))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(20.dp)
        ) {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleLarge) {
                title()
            }

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                    text()
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}
