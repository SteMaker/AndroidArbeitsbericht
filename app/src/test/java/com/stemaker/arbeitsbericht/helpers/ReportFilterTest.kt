package com.stemaker.arbeitsbericht.helpers

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.data.preferences.ConfigElement
import com.stemaker.arbeitsbericht.data.report.ReportData.ReportState
import com.stemaker.arbeitsbericht.support.configElementOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/* ReportFilter keeps no state of its own: it is a view onto three preference entries, and
 * the report list is rebuilt whenever it signals a change. Both halves matter — the state
 * bitmask has to stay consistent, and the change signal has to fire exactly when the
 * filter really changed, otherwise the list either goes stale or reloads constantly.
 */
class ReportFilterTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var projectName: ConfigElement<String>
    private lateinit var projectExtra: ConfigElement<String>
    private lateinit var states: ConfigElement<Int>
    private lateinit var filter: ReportFilter
    private var updates = 0

    private fun maskOf(vararg s: ReportState) = s.fold(0) { acc, st -> acc or (1 shl st.ordinal) }

    @Before
    fun setUp() {
        projectName = configElementOf("")
        projectExtra = configElementOf("")
        states = configElementOf(maskOf(ReportState.IN_WORK, ReportState.ON_HOLD, ReportState.DONE))
        filter = ReportFilter(projectName, projectExtra, states)
        filter.live.observeForever { updates++ }
        updates = 0
    }

    // ---------------------------------------------------------------- states

    @Test
    fun `the default filter shows everything but archived reports`() {
        assertThat(filter.inWork).isTrue()
        assertThat(filter.onHold).isTrue()
        assertThat(filter.done).isTrue()
        assertThat(filter.archived).isFalse()
    }

    @Test
    fun `remainingStates lists the ordinals of the enabled states`() {
        assertThat(filter.remainingStates).containsExactly(
            ReportState.IN_WORK.ordinal,
            ReportState.ON_HOLD.ordinal,
            ReportState.DONE.ordinal)
    }

    @Test
    fun `enabling archived adds it without disturbing the others`() {
        filter.archived = true

        assertThat(filter.archived).isTrue()
        assertThat(filter.inWork).isTrue()
        assertThat(filter.onHold).isTrue()
        assertThat(filter.done).isTrue()
        assertThat(filter.remainingStates).hasSize(4)
    }

    @Test
    fun `disabling one state leaves the others enabled`() {
        filter.onHold = false

        assertThat(filter.onHold).isFalse()
        assertThat(filter.inWork).isTrue()
        assertThat(filter.done).isTrue()
        assertThat(filter.remainingStates).containsExactly(
            ReportState.IN_WORK.ordinal, ReportState.DONE.ordinal)
    }

    @Test
    fun `every state can be toggled independently`() {
        for (state in ReportState.entries) {
            states.value = 0
            val setter: (Boolean) -> Unit = when (state) {
                ReportState.IN_WORK -> { v -> filter.inWork = v }
                ReportState.ON_HOLD -> { v -> filter.onHold = v }
                ReportState.DONE -> { v -> filter.done = v }
                ReportState.ARCHIVED -> { v -> filter.archived = v }
            }
            val getter: () -> Boolean = when (state) {
                ReportState.IN_WORK -> { { filter.inWork } }
                ReportState.ON_HOLD -> { { filter.onHold } }
                ReportState.DONE -> { { filter.done } }
                ReportState.ARCHIVED -> { { filter.archived } }
            }

            assertThat(getter()).isFalse()
            setter(true)
            assertThat(getter()).isTrue()
            assertThat(filter.remainingStates).containsExactly(state.ordinal)
            setter(false)
            assertThat(getter()).isFalse()
            assertThat(filter.remainingStates).isEmpty()
        }
    }

    @Test
    fun `disabling every state leaves an empty filter`() {
        filter.inWork = false
        filter.onHold = false
        filter.done = false
        filter.archived = false

        assertThat(filter.remainingStates).isEmpty()
    }

    @Test
    fun `state changes are written through to the preference`() {
        filter.archived = true

        assertThat(states.value and (1 shl ReportState.ARCHIVED.ordinal)).isNotEqualTo(0)
    }

    @Test
    fun `the filter reads state back from the preference`() {
        states.value = maskOf(ReportState.ARCHIVED)

        assertThat(filter.archived).isTrue()
        assertThat(filter.inWork).isFalse()
        assertThat(filter.remainingStates).containsExactly(ReportState.ARCHIVED.ordinal)
    }

    // ---------------------------------------------------------------- text

    @Test
    fun `project name is read from and written to the preference`() {
        filter.projectName = "Mueller"

        assertThat(filter.projectName).isEqualTo("Mueller")
        assertThat(projectName.value).isEqualTo("Mueller")
    }

    @Test
    fun `project extra is read from and written to the preference`() {
        filter.projectExtra = "Dach"

        assertThat(filter.projectExtra).isEqualTo("Dach")
        assertThat(projectExtra.value).isEqualTo("Dach")
    }

    // ---------------------------------------------------------------- signalling

    @Test
    fun `a changed state signals the list to reload`() {
        filter.archived = true

        assertThat(updates).isEqualTo(1)
    }

    @Test
    fun `setting a state to the value it already has does not signal`() {
        // Otherwise every redraw of the filter dialog would reload the whole report list
        filter.inWork = true

        assertThat(updates).isEqualTo(0)
    }

    @Test
    fun `clearing an already cleared state does not signal`() {
        filter.archived = false

        assertThat(updates).isEqualTo(0)
    }

    @Test
    fun `a changed project name signals once`() {
        filter.projectName = "Mueller"

        assertThat(updates).isEqualTo(1)
    }

    @Test
    fun `writing the same project name again does not signal`() {
        filter.projectName = "Mueller"
        updates = 0

        filter.projectName = "Mueller"

        assertThat(updates).isEqualTo(0)
    }

    @Test
    fun `a changed project extra signals once`() {
        filter.projectExtra = "Dach"

        assertThat(updates).isEqualTo(1)
    }

    @Test
    fun `blockUpdate suppresses signalling while it is set`() {
        filter.blockUpdate = true

        filter.projectName = "Mueller"
        filter.projectExtra = "Dach"
        filter.archived = true

        assertThat(updates).isEqualTo(0)
    }

    @Test
    fun `clearing blockUpdate signals once for the whole batch`() {
        // The filter dialog applies several changes at once and wants a single reload
        filter.blockUpdate = true
        filter.projectName = "Mueller"
        filter.projectExtra = "Dach"
        filter.archived = true

        filter.blockUpdate = false

        assertThat(updates).isEqualTo(1)
        assertThat(filter.projectName).isEqualTo("Mueller")
        assertThat(filter.projectExtra).isEqualTo("Dach")
        assertThat(filter.archived).isTrue()
    }
}
