package com.example.financeflow.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Thin wrapper around [SnackbarHostState] for save/delete feedback, provided app-wide via
 * [LocalSnackbarController] so any screen can call `LocalSnackbarController.current.show(...)`
 * without threading a SnackbarHostState through every navigation route.
 */
class SnackbarController(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    fun show(message: String) {
        scope.launch { hostState.showSnackbar(message) }
    }
}

val LocalSnackbarController = compositionLocalOf<SnackbarController> {
    error("No SnackbarController provided — wrap content in FinanceFlowApp's CompositionLocalProvider")
}
