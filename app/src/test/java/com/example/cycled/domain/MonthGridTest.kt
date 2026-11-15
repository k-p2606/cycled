package com.example.cycled.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthGridTest {

    private fun ym(year: Int, month: Int) = YearMonth.of(year, month)

    private fun leadingNulls(grid: List<List<LocalDate?>>) =
        grid.first().takeWhile { it == null }.size

    private fun trailingNulls(grid: List<List<LocalDate?>>) =
        grid.last().takeLastWhile { it == null }.size

    @Test
    fun nov2026_sundayStart_fiveRowsNoLeadingPadding() {
        val grid = monthGrid(ym(2026, 11), DayOfWeek.SUNDAY)

        assertEquals(5, grid.size)
        assertEquals(LocalDate.of(2026, 11, 1), grid[0][0])
        assertEquals(0, leadingNulls(grid))
        assertEquals(5, trailingNulls(grid))
        assertEquals(LocalDate.of(2026, 11, 30), grid[4][1])
    }

    @Test
    fun feb2026_sundayStart_exactlyFourRowsNoNulls() {
        val grid = monthGrid(ym(2026, 2), DayOfWeek.SUNDAY)

        assertEquals(4, grid.size)
        assertTrue(grid.flatten().none { it == null })
        assertEquals(LocalDate.of(2026, 2, 1), grid[0][0])
        assertEquals(LocalDate.of(2026, 2, 28), grid[3][6])
    }

    @Test
    fun aug2026_sundayStart_sixRowsAndFirstIsLastCellOfRowOne() {
        val grid = monthGrid(ym(2026, 8), DayOfWeek.SUNDAY)

        assertEquals(6, grid.size)
        assertEquals(6, leadingNulls(grid))
        assertEquals(LocalDate.of(2026, 8, 1), grid[0][6])
    }

    @Test
    fun nov2026_mondayStart_sixLeadingNullsAndSixRows() {
        val grid = monthGrid(ym(2026, 11), DayOfWeek.MONDAY)

        assertEquals(6, grid.size)
        assertEquals(6, leadingNulls(grid))
        assertEquals(LocalDate.of(2026, 11, 1), grid[0][6])
    }

    @Test
    fun feb2028_leapYear_has29Dates() {
        val grid = monthGrid(ym(2028, 2), DayOfWeek.SUNDAY)

        val dates = grid.flatten().filterNotNull()
        assertEquals(29, dates.size)
        assertEquals(LocalDate.of(2028, 2, 29), dates.last())
    }

    @Test
    fun everyMonthOf2026_sundayAndMondayStarts_areWellFormed() {
        for (firstDay in listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)) {
            for (m in 1..12) {
                val month = ym(2026, m)
                val grid = monthGrid(month, firstDay)
                val label = "$month starting $firstDay"

                assertTrue("$label: ${grid.size} rows", grid.size in 4..6)
                grid.forEach { row -> assertEquals("$label: row size", 7, row.size) }

                val cells = grid.flatten()
                val expected = (1..month.lengthOfMonth()).map { month.atDay(it) }
                assertEquals("$label: dates", expected, cells.filterNotNull())

                // Nulls only at the start of row 1 and the end of the last row.
                val lead = leadingNulls(grid)
                val trail = trailingNulls(grid)
                assertTrue("$label: leading nulls fit in row 1", lead < 7)
                assertTrue("$label: trailing nulls fit in last row", trail < 7)
                assertEquals("$label: nulls only at the edges", cells.size - lead - trail, expected.size)

                // First real date sits on its weekday column.
                assertEquals("$label: row 1 starts on $firstDay", firstDay, month.atDay(1).dayOfWeek.minus(lead.toLong()))
            }
        }
    }

    @Test
    fun paddingIsNullNotNeighbouringMonthDates() {
        val grid = monthGrid(ym(2026, 8), DayOfWeek.SUNDAY)

        assertNull(grid[0][0])
        assertNull(grid.last().last())
        assertTrue(grid.flatten().filterNotNull().all { it.monthValue == 8 })
    }
}
