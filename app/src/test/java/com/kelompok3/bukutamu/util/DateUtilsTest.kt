package com.kelompok3.bukutamu.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Unit test untuk DateUtils (format tanggal tampilan & rentang awal/akhir hari) sesuai PRD 8.4 & 15.
 */
class DateUtilsTest {

    private val testTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")

    @Test
    fun getDayRange_returnsExactStartAndEndOfDayMillis() {
        val cal = Calendar.getInstance(testTimeZone).apply {
            set(2026, Calendar.NOVEMBER, 8, 14, 30, 45)
            set(Calendar.MILLISECOND, 500)
        }
        val referenceMillis = cal.timeInMillis

        val (startOfDay, endOfDay) = DateUtils.getDayRange(referenceMillis, testTimeZone)

        // Durasi 1 hari penuh dikurangi 1 milidetik (00:00:00.000 s.d. 23:59:59.999)
        assertEquals(24L * 60 * 60 * 1000 - 1, endOfDay - startOfDay)

        val startCal = Calendar.getInstance(testTimeZone).apply { timeInMillis = startOfDay }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, startCal.get(Calendar.MONTH))
        assertEquals(8, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))
        assertEquals(0, startCal.get(Calendar.MILLISECOND))

        val endCal = Calendar.getInstance(testTimeZone).apply { timeInMillis = endOfDay }
        assertEquals(2026, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, endCal.get(Calendar.MONTH))
        assertEquals(8, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun getDayRange_correctlyDistinguishesYesterdayTodayAndTomorrowAtMidnightBoundary() {
        val cal = Calendar.getInstance(testTimeZone).apply {
            set(2026, Calendar.NOVEMBER, 8, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val (startOfDay, endOfDay) = DateUtils.getDayRange(cal.timeInMillis, testTimeZone)

        val yesterdayLastMillisecond = startOfDay - 1
        val todayFirstMillisecond = startOfDay
        val todayLastMillisecond = endOfDay
        val tomorrowFirstMillisecond = endOfDay + 1

        assertFalse(yesterdayLastMillisecond in startOfDay..endOfDay)
        assertTrue(todayFirstMillisecond in startOfDay..endOfDay)
        assertTrue(todayLastMillisecond in startOfDay..endOfDay)
        assertFalse(tomorrowFirstMillisecond in startOfDay..endOfDay)
    }

    @Test
    fun formatTimestamp_formatsCorrectlyAccordingToPrdPattern() {
        val cal = Calendar.getInstance(testTimeZone).apply {
            set(2026, Calendar.NOVEMBER, 8, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val formatted = DateUtils.formatTimestamp(
            timestamp = cal.timeInMillis,
            locale = Locale.ENGLISH,
            timeZone = testTimeZone
        )
        assertEquals("08 Nov 2026, 14:30", formatted)
    }
}

