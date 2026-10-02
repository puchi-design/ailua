package com.example.ui.components

import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Dialogs have their own window, so they must hide the native bar as the activity does. */
@Composable
fun HideDialogStatusBar() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.dialogWindow()
        if (window == null) {
            onDispose { }
        } else {
            val hideStatusBar = Runnable {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.statusBars())
                }
            }
            // Apply after Dialog.show(), and again after a system picker returns focus.
            view.post(hideStatusBar)
            val observer = view.viewTreeObserver
            val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                if (hasFocus) hideStatusBar.run()
            }
            observer.addOnWindowFocusChangeListener(focusListener)
            onDispose {
                view.removeCallbacks(hideStatusBar)
                if (observer.isAlive) observer.removeOnWindowFocusChangeListener(focusListener)
                else view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            }
        }
    }
}

private fun View.dialogWindow(): Window? {
    var candidate: Any? = this
    while (candidate != null) {
        if (candidate is DialogWindowProvider) return candidate.window
        candidate = (candidate as? View)?.parent
    }
    return null
}
