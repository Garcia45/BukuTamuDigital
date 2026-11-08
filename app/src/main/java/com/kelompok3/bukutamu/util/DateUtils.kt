package com.kelompok3.bukutamu.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utilitas pemformatan tanggal tampilan dan perhitungan rentang awal/akhir hari sesuai PRD 8.3 & 8.4.
 */
object DateUtils {

    private const val DISPLAY_PATTERN = "dd MMM yyyy, HH:mm"
    private const val DATE_ONLY_PATTERN = "dd MMM yyyy"
    private const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000

    /**
     * Memformat timestamp (milidetik) menjadi string tampilan, contoh: "08 Nov 2026, 14:30" (FR-15).
     */
    fun formatTimestamp(
        timestamp: Long,
        locale: Locale = Locale.forLanguageTag("id-ID"),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val formatter = SimpleDateFormat(DISPLAY_PATTERN, locale).apply {
            this.timeZone = timeZone
        }
        return formatter.format(Date(timestamp))
    }

    /**
     * Memformat timestamp (milidetik) menjadi string tanggal saja, contoh: "08 Nov 2026" (FR-26).
     */
    fun formatDateOnly(
        timestamp: Long,
        locale: Locale = Locale.forLanguageTag("id-ID"),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val formatter = SimpleDateFormat(DATE_ONLY_PATTERN, locale).apply {
            this.timeZone = timeZone
        }
        return formatter.format(Date(timestamp))
    }

    /**
     * Menghitung rentang awal hari (00:00:00.000) dan akhir hari (23:59:59.999)
     * sesuai rumus pada PRD bagian 8.4 (FR-10, FR-24).
     *
     * @param referenceTimeMillis waktu acuan (default: waktu perangkat saat ini)
     * @param timeZone zona waktu perangkat
     * @return Pair(startOfDay, endOfDay) dalam milidetik
     */
    fun getDayRange(
        referenceTimeMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = referenceTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + ONE_DAY_MILLIS - 1
        return Pair(startOfDay, endOfDay)
    }

    /**
     * Mengembalikan batas awal hari (00:00:00.000) untuk waktu acuan.
     */
    fun getTodayStartMillis(
        referenceTimeMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long = getDayRange(referenceTimeMillis, timeZone).first

    /**
     * Mengembalikan batas akhir hari (23:59:59.999) untuk waktu acuan.
     */
    fun getTodayEndMillis(
        referenceTimeMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long = getDayRange(referenceTimeMillis, timeZone).second
}
