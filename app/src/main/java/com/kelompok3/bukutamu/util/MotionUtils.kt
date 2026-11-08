package com.kelompok3.bukutamu.util

import android.content.Context
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Utilitas motion terkalibrasi sesuai pedoman apple-inspired-design (motion.md & mobile.md).
 * - Token durasi semantik: FAST (180ms), BASE (280ms), SLOW (480ms).
 * - Menghormati preferensi aksesibilitas Reduce Motion / Remove Animations pada perangkat Android.
 * - Seluruh transisi bersifat interruptible dan tidak pernah memblokir input pengguna.
 */
object MotionUtils {

    const val DURATION_FAST_MS = 180L
    const val DURATION_BASE_MS = 280L
    const val DURATION_SLOW_MS = 480L

    private val enterInterpolator = DecelerateInterpolator(1.6f)

    /**
     * Memeriksa apakah pengguna mengaktifkan Reduce Motion / menonaktifkan animasi di pengaturan sistem.
     */
    fun isReducedMotionEnabled(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Menampilkan permukaan utama secara halus saat layar dibuka tanpa menunda interaksi.
     * Jika Reduce Motion aktif, permukaan langsung tampil pada posisi akhir tanpa travel.
     */
    fun animateSurfaceEntry(view: View) {
        view.animate().cancel()
        if (isReducedMotionEnabled(view.context)) {
            view.alpha = 1f
            view.translationY = 0f
            return
        }
        val offsetPx = 10f * view.resources.displayMetrics.density
        view.alpha = 0f
        view.translationY = offsetPx
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(DURATION_BASE_MS)
            .setInterpolator(enterInterpolator)
            .start()
    }

    /**
     * Menampilkan banner peringatan/status dengan transisi opacity singkat yang dapat diinterupsi.
     */
    fun revealStatusBanner(view: View) {
        view.animate().cancel()
        view.visibility = View.VISIBLE
        if (isReducedMotionEnabled(view.context)) {
            view.alpha = 1f
            return
        }
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(DURATION_FAST_MS)
            .setInterpolator(enterInterpolator)
            .start()
    }

    /**
     * Umpan balik visual ringan saat nilai metrik berubah secara reaktif (tanpa count-up yang menunda pembacaan).
     */
    fun animateMetricUpdate(view: View) {
        view.animate().cancel()
        if (isReducedMotionEnabled(view.context)) {
            view.alpha = 1f
            return
        }
        view.alpha = 0.65f
        view.animate()
            .alpha(1f)
            .setDuration(DURATION_FAST_MS)
            .setInterpolator(enterInterpolator)
            .start()
    }
}

