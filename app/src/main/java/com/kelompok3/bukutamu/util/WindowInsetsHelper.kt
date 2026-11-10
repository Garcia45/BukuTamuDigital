package com.kelompok3.bukutamu.util

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max

/**
 * Helper untuk menangani WindowInsets (Status Bar, Display Cutout, Navigation Bar, dan Keyboard IME)
 * secara konsisten pada Android 15 (SDK 35 Edge-to-Edge) maupun versi Android sebelumnya.
 */
object WindowInsetsHelper {

    /**
     * Mengaktifkan mode edge-to-edge dan menerapkan inset top pada [topBar] serta inset bottom/IME
     * pada [bottomView] (misalnya bottom action bar atau scrollable container).
     */
    fun applyEdgeToEdgeInsets(
        activity: ComponentActivity,
        rootView: View,
        topBar: View? = null,
        bottomView: View? = null,
        extraTopDp: Int = 8,
        extraBottomDp: Int = 12
    ) {
        activity.enableEdgeToEdge()

        val density = rootView.resources.displayMetrics.density
        val extraTopPx = (extraTopDp * density).toInt()
        val extraBottomPx = (extraBottomDp * density).toInt()

        val initialTopPadding = topBar?.paddingTop ?: rootView.paddingTop
        val initialBottomPadding = bottomView?.paddingBottom ?: rootView.paddingBottom
        val initialRootLeft = rootView.paddingLeft
        val initialRootRight = rootView.paddingRight

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = max(systemBars.bottom, ime.bottom)

            view.updatePadding(
                left = initialRootLeft + systemBars.left,
                right = initialRootRight + systemBars.right
            )

            if (topBar != null) {
                topBar.updatePadding(top = initialTopPadding + systemBars.top + extraTopPx)
            } else {
                view.updatePadding(top = initialTopPadding + systemBars.top + extraTopPx)
            }

            if (bottomView != null) {
                bottomView.updatePadding(bottom = initialBottomPadding + bottomInset + extraBottomPx)
            } else {
                view.updatePadding(bottom = initialBottomPadding + bottomInset + extraBottomPx)
            }

            windowInsets
        }
    }
}

