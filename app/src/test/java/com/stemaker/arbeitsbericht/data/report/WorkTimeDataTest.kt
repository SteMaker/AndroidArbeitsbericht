package com.stemaker.arbeitsbericht.data.report

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

/* Work and drive times are what the report is actually billed from, so the duration
 * arithmetic is the most consequential calculation in the app.
 */
class WorkTimeDataTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var defaultEmployee: MutableLiveData<String>

    @Before
    fun setUp() {
        defaultEmployee = MutableLiveData("Hans")
    }

    /* workDuration is a MediatorLiveData, so it only recomputes while observed. */
    private fun workTime(
        start: String? = null,
        end: String? = null,
        pause: String? = null,
    ): WorkTimeData = WorkTimeData(defaultEmployee).also { wt ->
        wt.workDuration.observeForever { }
        pause?.let { wt.pauseDuration.value = it }
        start?.let { wt.startTime.value = it }
        end?.let { wt.endTime.value = it }
    }

    private fun dateOf(year: Int, month: Int, day: Int): Calendar =
        GregorianCalendar(year, month - 1, day)

    // ---------------------------------------------------------------- duration

    @Test
    fun `a plain shift is the difference between start and end`() {
        assertThat(workTime(start = "08:00", end = "16:00").workDuration.value).isEqualTo("08:00")
    }

    @Test
    fun `the pause is subtracted from the shift`() {
        assertThat(workTime(start = "08:00", end = "16:30", pause = "00:30").workDuration.value)
            .isEqualTo("08:00")
    }

    @Test
    fun `odd minutes are carried correctly`() {
        assertThat(workTime(start = "07:45", end = "16:15").workDuration.value).isEqualTo("08:30")
    }

    @Test
    fun `a shift ending past midnight is treated as ending on the next day`() {
        assertThat(workTime(start = "22:00", end = "06:00").workDuration.value).isEqualTo("08:00")
    }

    @Test
    fun `a shift ending earlier in the same hour is treated as ending on the next day`() {
        assertThat(workTime(start = "08:30", end = "08:15").workDuration.value).isEqualTo("23:45")
    }

    @Test
    fun `equal start and end is a zero duration, not a full day`() {
        assertThat(workTime(start = "08:00", end = "08:00").workDuration.value).isEqualTo("00:00")
    }

    @Test
    fun `a duration of more than a day is still reported in hours`() {
        // normalizedStandard(dayTime()) must not turn 24h into "1 day, 0 hours"
        assertThat(workTime(start = "08:00", end = "07:00").workDuration.value).isEqualTo("23:00")
    }

    @Test
    fun `an unset start or end time yields a zero duration instead of crashing`() {
        assertThat(WorkTimeData(defaultEmployee).also { it.workDuration.observeForever { } }
            .workDuration.value).isEqualTo("00:00")
        assertThat(workTime(start = "08:00").workDuration.value).isEqualTo("00:00")
        assertThat(workTime(end = "16:00").workDuration.value).isEqualTo("00:00")
    }

    @Test
    fun `an unparseable time yields a zero duration instead of crashing`() {
        assertThat(workTime(start = "oops", end = "16:00").workDuration.value).isEqualTo("00:00")
        assertThat(workTime(start = "08:00", end = "16:00", pause = "x").workDuration.value)
            .isEqualTo("00:00")
    }

    @Test
    fun `the duration is multiplied by the number of employees`() {
        // Two people working the same shift bill twice the hours
        val wt = workTime(start = "08:00", end = "16:00")
        assertThat(wt.workDuration.value).isEqualTo("08:00")

        wt.addEmployee()

        assertThat(wt.employees.getSize()).isEqualTo(2)
        assertThat(wt.workDuration.value).isEqualTo("16:00")
    }

    @Test
    fun `removing an employee again reduces the duration`() {
        val wt = workTime(start = "08:00", end = "16:00")
        val second = wt.addEmployee()
        assertThat(wt.workDuration.value).isEqualTo("16:00")

        wt.removeEmployee(second)

        assertThat(wt.employees.getSize()).isEqualTo(1)
        assertThat(wt.workDuration.value).isEqualTo("08:00")
    }

    @Test
    fun `the duration updates when the pause changes`() {
        val wt = workTime(start = "08:00", end = "16:00")
        assertThat(wt.workDuration.value).isEqualTo("08:00")

        wt.pauseDuration.value = "01:00"

        assertThat(wt.workDuration.value).isEqualTo("07:00")
    }

    // ---------------------------------------------------------------- defaults

    @Test
    fun `a new work time starts with one employee taken from the preferences`() {
        val wt = WorkTimeData(defaultEmployee)

        assertThat(wt.employees.getSize()).isEqualTo(1)
        assertThat(wt.employees.items[0].value).isEqualTo("Hans")
    }

    @Test
    fun `an added employee is prefilled with the default employee name`() {
        val wt = WorkTimeData(defaultEmployee)

        val added = wt.addEmployee()

        assertThat(added.value).isEqualTo("Hans")
    }

    @Test
    fun `a new work time has empty times so the editor offers the current time`() {
        val wt = WorkTimeData(defaultEmployee)

        assertThat(wt.startTime.value).isEmpty()
        assertThat(wt.endTime.value).isEmpty()
        assertThat(wt.pauseDuration.value).isEqualTo("00:00")
        assertThat(wt.driveTime.value).isEqualTo("00:00")
        assertThat(wt.distance.value).isEqualTo(0)
    }

    @Test
    fun `container defaults are applied to a newly added work time`() {
        val container = WorkTimeContainerData(defaultEmployee)
        val defaults = DefaultValues().apply {
            useDefaultDistance = true
            defaultDistance = 42
            useDefaultDriveTime = true
            defaultDriveTime = "00:45"
        }

        val wt = container.addWorkTime(defaults)

        assertThat(wt.distance.value).isEqualTo(42)
        assertThat(wt.driveTime.value).isEqualTo("00:45")
    }

    @Test
    fun `container defaults that are switched off are not applied`() {
        val container = WorkTimeContainerData(defaultEmployee)
        val defaults = DefaultValues().apply {
            useDefaultDistance = false
            defaultDistance = 42
            useDefaultDriveTime = false
            defaultDriveTime = "00:45"
        }

        val wt = container.addWorkTime(defaults)

        assertThat(wt.distance.value).isEqualTo(0)
        assertThat(wt.driveTime.value).isEqualTo("00:00")
    }

    // ---------------------------------------------------------------- clone

    @Test
    fun `clone carries over times, employees and distance`() {
        val origin = WorkTimeData(defaultEmployee).apply {
            date.value = dateOf(2024, 3, 12) // a Tuesday
            startTime.value = "07:30"
            endTime.value = "16:45"
            pauseDuration.value = "00:45"
            driveTime.value = "01:15"
            distance.value = 123
            addEmployee().value = "Grete"
        }

        val clone = WorkTimeData(defaultEmployee).apply { clone(origin) }

        assertThat(clone.startTime.value).isEqualTo("07:30")
        assertThat(clone.endTime.value).isEqualTo("16:45")
        assertThat(clone.pauseDuration.value).isEqualTo("00:45")
        assertThat(clone.driveTime.value).isEqualTo("01:15")
        assertThat(clone.distance.value).isEqualTo(123)
        assertThat(clone.employees.map { it.value }).containsExactly("Hans", "Grete").inOrder()
    }

    @Test
    fun `clone advances a weekday to the following day`() {
        val origin = WorkTimeData(defaultEmployee).apply { date.value = dateOf(2024, 3, 12) } // Tue

        val clone = WorkTimeData(defaultEmployee).apply { clone(origin) }

        assertThat(clone.date.value!!.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.WEDNESDAY)
        assertThat(clone.date.value!!.get(Calendar.DAY_OF_MONTH)).isEqualTo(13)
    }

    @Test
    fun `clone skips the weekend when starting on a Friday`() {
        val origin = WorkTimeData(defaultEmployee).apply { date.value = dateOf(2024, 3, 15) } // Fri

        val clone = WorkTimeData(defaultEmployee).apply { clone(origin) }

        assertThat(clone.date.value!!.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.MONDAY)
        assertThat(clone.date.value!!.get(Calendar.DAY_OF_MONTH)).isEqualTo(18)
    }

    @Test
    fun `clone of a Saturday entry lands on the following Monday`() {
        val origin = WorkTimeData(defaultEmployee).apply { date.value = dateOf(2024, 3, 16) } // Sat

        val clone = WorkTimeData(defaultEmployee).apply { clone(origin) }

        assertThat(clone.date.value!!.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.MONDAY)
        assertThat(clone.date.value!!.get(Calendar.DAY_OF_MONTH)).isEqualTo(18)
    }

    @Test
    fun `clone of a Sunday entry lands on the following Monday`() {
        val origin = WorkTimeData(defaultEmployee).apply { date.value = dateOf(2024, 3, 17) } // Sun

        val clone = WorkTimeData(defaultEmployee).apply { clone(origin) }

        assertThat(clone.date.value!!.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.MONDAY)
        assertThat(clone.date.value!!.get(Calendar.DAY_OF_MONTH)).isEqualTo(18)
    }

    @Test
    fun `clone does not alias the origin date`() {
        val origin = WorkTimeData(defaultEmployee).apply { date.value = dateOf(2024, 3, 12) }

        val clone = WorkTimeData(defaultEmployee).apply { clone(origin) }
        clone.date.value!!.add(Calendar.YEAR, 1)

        assertThat(origin.date.value!!.get(Calendar.YEAR)).isEqualTo(2024)
    }

    @Test
    fun `clone does not alias the origin employee list`() {
        val origin = WorkTimeData(defaultEmployee).apply { date.value = dateOf(2024, 3, 12) }

        val clone = WorkTimeData(defaultEmployee).apply { clone(origin) }
        clone.employees.items[0].value = "Someone else"

        assertThat(origin.employees.items[0].value).isEqualTo("Hans")
    }

    // ---------------------------------------------------------------- container

    @Test
    fun `sortByDate orders entries oldest first`() {
        val container = WorkTimeContainerData(defaultEmployee)
        container.addWorkTime().date.value = dateOf(2024, 3, 20)
        container.addWorkTime().date.value = dateOf(2024, 3, 10)
        container.addWorkTime().date.value = dateOf(2024, 3, 15)

        container.sortByDate()

        assertThat(container.items.map { it.date.value!!.get(Calendar.DAY_OF_MONTH) })
            .containsExactly(10, 15, 20).inOrder()
    }

    @Test
    fun `sortByDate is stable for an already sorted container`() {
        val container = WorkTimeContainerData(defaultEmployee)
        container.addWorkTime().date.value = dateOf(2024, 3, 10)
        container.addWorkTime().date.value = dateOf(2024, 3, 11)

        container.sortByDate()

        assertThat(container.items.map { it.date.value!!.get(Calendar.DAY_OF_MONTH) })
            .containsExactly(10, 11).inOrder()
    }

    @Test
    fun `addWorkTime with a reference clones that reference`() {
        val container = WorkTimeContainerData(defaultEmployee)
        val first = container.addWorkTime().apply {
            date.value = dateOf(2024, 3, 12)
            startTime.value = "07:00"
            distance.value = 50
        }

        val second = container.addWorkTime(first)

        assertThat(container.getSize()).isEqualTo(2)
        assertThat(second.startTime.value).isEqualTo("07:00")
        assertThat(second.distance.value).isEqualTo(50)
        assertThat(second.date.value!!.get(Calendar.DAY_OF_MONTH)).isEqualTo(13)
    }

    @Test
    fun `removeWorkTime takes the entry out of the container`() {
        val container = WorkTimeContainerData(defaultEmployee)
        val wt = container.addWorkTime()
        container.addWorkTime()

        container.removeWorkTime(wt)

        assertThat(container.getSize()).isEqualTo(1)
        assertThat(container.items).doesNotContain(wt)
    }
}
