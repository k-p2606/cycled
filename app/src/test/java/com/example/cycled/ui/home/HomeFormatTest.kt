package com.example.cycled.ui.home

import com.example.cycled.ui.ComingUpKind
import com.example.cycled.ui.ComingUpRow
import com.example.cycled.ui.DateSpan
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFormatTest {

    private val uk = Locale.UK
    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)
    private fun row(kind: ComingUpKind, start: LocalDate, end: LocalDate = start, until: String = "in 2 days") =
        ComingUpRow(kind, DateSpan(start, end), until)

    @Test
    fun dateLine() = assertEquals("Sunday, 8 Nov", HomeFormat.dateLine(d(11, 8), uk))

    @Test
    fun rowDates() {
        assertEquals("Sat 28 Nov", HomeFormat.rowDate(row(ComingUpKind.NEXT_PERIOD, d(11, 28)), uk))
        assertEquals("9–14 Nov", HomeFormat.rowDate(row(ComingUpKind.FERTILE_WINDOW, d(11, 9), d(11, 14)), uk))
        assertEquals("29 Nov – 4 Dec", HomeFormat.rowDate(row(ComingUpKind.FERTILE_WINDOW, d(11, 29), d(12, 4)), uk))
    }

    @Test
    fun rangeLine() = assertEquals(
        "Likely 26 Nov – 2 Dec. Estimates from your logged dates, not contraception or medical advice.",
        HomeFormat.rangeLine(DateSpan(d(11, 26), d(12, 2)), uk),
    )

    @Test
    fun spokenText() {
        assertEquals(
            "Next period, Saturday 28 November, in 20 days",
            HomeFormat.rowSpoken(row(ComingUpKind.NEXT_PERIOD, d(11, 28), until = "in 20 days"), uk),
        )
        assertEquals(
            "Fertile window, 9 to 14 November, tomorrow",
            HomeFormat.rowSpoken(row(ComingUpKind.FERTILE_WINDOW, d(11, 9), d(11, 14), "tomorrow"), uk),
        )
        assertEquals(
            "Cycle day 9, Flood tide, follicular phase",
            HomeFormat.heroSpoken(9, "Flood tide", "Follicular phase", uk),
        )
    }
}
