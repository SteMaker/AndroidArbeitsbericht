package com.stemaker.arbeitsbericht.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

/* Work time dates are stored in the database as dd.MM.yyyy strings, so these two functions
 * sit on the persistence path for every report. The report ID also slices the same format
 * by index, which is why the zero padding matters.
 */
class DateConversionTest {

    @Test
    fun `a date is formatted as dd MM yyyy`() {
        val cal = GregorianCalendar(2024, Calendar.MARCH, 7)

        assertThat(calendarToDateString(cal)).isEqualTo("07.03.2024")
    }

    @Test
    fun `day and month are zero padded to two digits`() {
        assertThat(calendarToDateString(GregorianCalendar(2024, Calendar.JANUARY, 1)))
            .isEqualTo("01.01.2024")
    }

    @Test
    fun `two digit days and months are not padded further`() {
        assertThat(calendarToDateString(GregorianCalendar(2024, Calendar.DECEMBER, 31)))
            .isEqualTo("31.12.2024")
    }

    @Test
    fun `a null date formats as an empty string`() {
        // ReportDb writes whatever this returns, so it must not throw
        assertThat(calendarToDateString(null)).isEmpty()
    }

    @Test
    fun `a formatted date parses back to the same day`() {
        val parsed = dateStringToCalendar("07.03.2024")

        assertThat(parsed.get(Calendar.DAY_OF_MONTH)).isEqualTo(7)
        assertThat(parsed.get(Calendar.MONTH)).isEqualTo(Calendar.MARCH)
        assertThat(parsed.get(Calendar.YEAR)).isEqualTo(2024)
    }

    @Test
    fun `parsing then formatting round trips`() {
        for (date in listOf("01.01.2020", "29.02.2024", "31.12.2099", "15.06.2024")) {
            assertThat(calendarToDateString(dateStringToCalendar(date))).isEqualTo(date)
        }
    }

    @Test
    fun `formatting then parsing round trips`() {
        for (cal in listOf(
            GregorianCalendar(2020, Calendar.JANUARY, 1),
            GregorianCalendar(2024, Calendar.FEBRUARY, 29),
            GregorianCalendar(2099, Calendar.DECEMBER, 31),
        )) {
            val parsed = dateStringToCalendar(calendarToDateString(cal))
            assertThat(parsed.get(Calendar.DAY_OF_MONTH)).isEqualTo(cal.get(Calendar.DAY_OF_MONTH))
            assertThat(parsed.get(Calendar.MONTH)).isEqualTo(cal.get(Calendar.MONTH))
            assertThat(parsed.get(Calendar.YEAR)).isEqualTo(cal.get(Calendar.YEAR))
        }
    }

    @Test
    fun `the end of a month parses correctly regardless of today's date`() {
        // dateStringToCalendar starts from today and overwrites the fields one by one, so a
        // target day that does not exist in today's month must still come out right.
        val parsed = dateStringToCalendar("31.01.2024")

        assertThat(parsed.get(Calendar.DAY_OF_MONTH)).isEqualTo(31)
        assertThat(parsed.get(Calendar.MONTH)).isEqualTo(Calendar.JANUARY)
        assertThat(parsed.get(Calendar.YEAR)).isEqualTo(2024)
    }

    @Test
    fun `a leap day parses correctly`() {
        val parsed = dateStringToCalendar("29.02.2024")

        assertThat(parsed.get(Calendar.DAY_OF_MONTH)).isEqualTo(29)
        assertThat(parsed.get(Calendar.MONTH)).isEqualTo(Calendar.FEBRUARY)
        assertThat(parsed.get(Calendar.YEAR)).isEqualTo(2024)
    }

    @Test
    fun `the date string is sliced the way the report ID expects`() {
        // ReportData.buildId() takes substring(6,10) as the year, (3,5) as the month and
        // (0,2) as the day, so the format has to stay exactly this wide.
        val formatted = calendarToDateString(GregorianCalendar(2024, Calendar.MARCH, 7))

        assertThat(formatted).hasLength(10)
        assertThat(formatted.substring(0, 2)).isEqualTo("07")
        assertThat(formatted.substring(3, 5)).isEqualTo("03")
        assertThat(formatted.substring(6, 10)).isEqualTo("2024")
    }
}
