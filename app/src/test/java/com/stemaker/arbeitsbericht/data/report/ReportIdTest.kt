package com.stemaker.arbeitsbericht.data.report

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.support.FakePreferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

/* The report ID is user-facing: it names the generated ODF/PDF/XLSX file and appears in the
 * report list and in the mail subject. It is assembled from a user-defined pattern, so the
 * placeholder expansion and the sanitising of the result are worth covering closely.
 *
 * The documented placeholders (README) are:
 *   %c running counter, %Nc zero-padded counter, %y year, %m month, %d day,
 *   %e employee name, %p device name, %k project/client name, %z project extra
 */
class ReportIdTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /* Must not be a field initialiser: ConfigElement sets a LiveData value while being
     * constructed, and LiveData asserts it runs on the main thread. Field initialisers run
     * during test-class construction, before InstantTaskExecutorRule is in place.
     */
    private lateinit var prefs: FakePreferences

    @Before
    fun setUp() {
        prefs = FakePreferences()
    }

    private fun report(
        cnt: Int = 1,
        pattern: String = "%c",
        employee: String = "",
        device: String = "",
        projectName: String = "",
        projectExtra: String = "",
    ): ReportData {
        prefs.reportIdPattern.value = pattern
        prefs.employeeName.value = employee
        prefs.deviceName.value = device
        val r = ReportData.createReport(cnt, prefs.mock)
        // The id is a MediatorLiveData, so it needs an observer to recompute
        r.id.observeForever { }
        r.project.name.value = projectName
        r.project.extra1.value = projectExtra
        return r
    }

    private fun today(field: Int): String {
        val cal = Calendar.getInstance()
        return when (field) {
            Calendar.YEAR -> cal.get(Calendar.YEAR).toString()
            Calendar.MONTH -> (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            else -> cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        }
    }

    @Test
    fun `%c expands to the running counter`() {
        assertThat(report(cnt = 42, pattern = "%c").id.value).isEqualTo("42")
    }

    @Test
    fun `%Nc zero-pads the running counter to N digits`() {
        assertThat(report(cnt = 42, pattern = "%4c").id.value).isEqualTo("0042")
        assertThat(report(cnt = 7, pattern = "%6c").id.value).isEqualTo("000007")
    }

    @Test
    fun `a counter wider than the padding is not truncated`() {
        assertThat(report(cnt = 1234567, pattern = "%4c").id.value).isEqualTo("1234567")
    }

    @Test
    fun `%y %m %d expand to the creation date`() {
        val r = report(pattern = "%y-%m-%d")
        assertThat(r.id.value).isEqualTo(
            "${today(Calendar.YEAR)}-${today(Calendar.MONTH)}-${today(Calendar.DAY_OF_MONTH)}")
    }

    @Test
    fun `%e and %p expand to employee and device name`() {
        val r = report(pattern = "%e_%p", employee = "Hans", device = "Tablet")
        assertThat(r.id.value).isEqualTo("Hans_Tablet")
    }

    @Test
    fun `%k and %z expand to project name and project extra`() {
        val r = report(pattern = "%k-%z", projectName = "Mueller", projectExtra = "Dach")
        assertThat(r.id.value).isEqualTo("Mueller-Dach")
    }

    @Test
    fun `the default pattern combines device, year and a padded counter`() {
        val r = report(cnt = 5, pattern = "%p-%y-%6c", device = "Tablet")
        assertThat(r.id.value).isEqualTo("Tablet-${today(Calendar.YEAR)}-000005")
    }

    @Test
    fun `German umlauts are preserved in the id`() {
        // The sanitiser explicitly allows these, since customer names commonly contain them
        val r = report(pattern = "%k", projectName = "Müller-Schrödinger")
        assertThat(r.id.value).isEqualTo("Müller-Schrödinger")
    }

    @Test
    fun `characters that are problematic in file names are replaced by underscores`() {
        val r = report(pattern = "%k", projectName = "a/b\\c:d*e?f\"g<h>i|j")
        assertThat(r.id.value).isEqualTo("a_b_c_d_e_f_g_h_i_j")
    }

    @Test
    fun `spaces are replaced so the id is usable as a file name`() {
        val r = report(pattern = "%k", projectName = "Firma Mueller GmbH")
        assertThat(r.id.value).isEqualTo("Firma_Mueller_GmbH")
    }

    @Test
    fun `hyphen and underscore survive sanitising`() {
        val r = report(pattern = "%k", projectName = "a-b_c")
        assertThat(r.id.value).isEqualTo("a-b_c")
    }

    @Test
    fun `an empty placeholder value collapses to nothing`() {
        val r = report(cnt = 3, pattern = "%k%c", projectName = "")
        assertThat(r.id.value).isEqualTo("3")
    }

    @Test
    fun `a pattern without placeholders is returned as is`() {
        assertThat(report(pattern = "Bericht").id.value).isEqualTo("Bericht")
    }

    @Test
    fun `the id tracks later changes to the project name`() {
        val r = report(pattern = "%k", projectName = "First")
        assertThat(r.id.value).isEqualTo("First")

        r.project.name.value = "Second"

        assertThat(r.id.value).isEqualTo("Second")
    }

    @Test
    fun `the id tracks a changed pattern`() {
        val r = report(cnt = 9, pattern = "%c")
        assertThat(r.id.value).isEqualTo("9")

        prefs.reportIdPattern.value = "Nr-%3c"

        assertThat(r.id.value).isEqualTo("Nr-009")
    }

    @Test
    fun `a newly created report is marked as modified so it gets persisted`() {
        val r = ReportData.createReport(1, prefs.mock)

        assertThat(r.modified).isTrue()
    }

    @Test
    fun `a new report starts in state IN_WORK`() {
        val r = ReportData.createReport(1, prefs.mock)

        assertThat(r.state.value).isEqualTo(ReportData.ReportState.IN_WORK)
    }

    @Test
    fun `report state maps to and from its integer representation`() {
        for (state in ReportData.ReportState.entries) {
            val asInt = ReportData.ReportState.toInt(state)
            assertThat(ReportData.ReportState.fromInt(asInt)).isEqualTo(state)
        }
        assertThat(ReportData.ReportState.max)
            .isEqualTo(ReportData.ReportState.ARCHIVED.ordinal)
    }
}
