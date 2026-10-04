package com.stemaker.arbeitsbericht

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.support.UiTestSupport.app
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitAppInitialised
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitUntil
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitView
import com.stemaker.arbeitsbericht.support.UiTestSupport.onMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/* The summary screen holds the two signature pads and is where report generation starts.
 * Its lifecycle is the interesting part: it saves the signatures on pause and on back, and
 * the signature pads render themselves into bitmaps when their state is saved.
 *
 * SummaryActivity reads reportRepo.activeReport, which is a lateinit, so a report has to
 * exist and be active before the activity is launched. That rules out ActivityScenarioRule,
 * which launches before @Before runs.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SummaryActivityTest {

    private lateinit var report: ReportData
    private var scenario: ActivityScenario<SummaryActivity>? = null

    @Before
    fun setUp() {
        awaitAppInitialised()
        report = onMain { app.reportRepo.createReportAndActivate(null) }
        awaitUntil("the report to be stored") { app.reportRepo.isReportVisible(report.cnt) }
        onMain {
            report.id.observeForever { }
            report.project.name.value = "Zusammenfassung"
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        onMain { app.reportRepo.deleteReport(report) }
    }

    private fun launch(): ActivityScenario<SummaryActivity> =
        ActivityScenario.launch(SummaryActivity::class.java).also {
            scenario = it
            awaitView(withId(R.id.webview))
        }

    @Test
    fun theSummaryScreenShowsTheReportPreview() {
        launch()

        awaitView(withId(R.id.webview))
    }

    @Test
    fun theSignatureSectionsStartCollapsed() {
        // This is what leaves the pads without a size, which is the state the instance
        // state crash below depends on.
        launch()

        onView(withId(R.id.employee_signature_card))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        onView(withId(R.id.client_signature_card))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun backgroundingTheScreenWithUnlaidOutSignaturePadsDoesNotCrash() {
        /* Regression test. SignaturePad.onSaveInstanceState() builds a bitmap from the
         * view's own width and height, which are 0 until the view has been laid out, and
         * Bitmap.createBitmap then throws and takes the activity down. Driving the
         * activity to CREATED forces onSaveInstanceState on every pad.
         */
        val s = launch()

        s.moveToState(Lifecycle.State.CREATED)
        s.moveToState(Lifecycle.State.RESUMED)

        awaitView(withId(R.id.webview))
        assertThat(s.state).isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    fun theScreenSurvivesBeingRecreated() {
        val s = launch()

        s.recreate()

        awaitView(withId(R.id.webview))
    }

    @Test
    fun pausingTheScreenPersistsTheReport() {
        val s = launch()
        onMain { report.project.name.value = "Pausiert" }
        awaitUntil("the edit to land") { report.project.name.value == "Pausiert" }

        // onPause saves the signatures, which saves the report
        s.moveToState(Lifecycle.State.CREATED)

        awaitUntil("the report to be written back") { !report.modified }
    }
}
