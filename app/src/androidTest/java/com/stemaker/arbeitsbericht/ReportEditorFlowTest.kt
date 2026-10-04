package com.stemaker.arbeitsbericht

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.support.UiTestSupport.app
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitAppInitialised
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitUntil
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitView
import com.stemaker.arbeitsbericht.support.UiTestSupport.dismissVersionDialogIfShown
import com.stemaker.arbeitsbericht.support.UiTestSupport.onMain
import com.stemaker.arbeitsbericht.support.UiTestSupport.resetReportFilter
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/* End-to-end coverage of the main user journey: create a report from the report list, type
 * into it, and confirm what was typed reaches the stored report.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ReportEditorFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val marker = "T${Random.nextInt(100_000, 999_999)}"
    private val createdReports = mutableListOf<ReportData>()

    @Before
    fun setUp() {
        Intents.init()
        awaitAppInitialised()
        // Must come first: while the changelog dialog is up it owns the focused window and
        // the report list behind it cannot be matched.
        dismissVersionDialogIfShown()
        awaitView(withId(R.id.report_list_rv))
    }

    @After
    fun tearDown() {
        Intents.release()
        // Leave the device as we found it
        onMain { createdReports.forEach { app.reportRepo.deleteReport(it) } }
        resetReportFilter()
    }

    private fun newReportFromList(): ReportData {
        val before = app.reportRepo.amountOfReports
        onView(withId(R.id.new_report_button)).perform(click())
        intended(hasComponent(ReportEditorActivity::class.java.name))
        awaitUntil("the new report to be registered") {
            app.reportRepo.amountOfReports > before
        }
        val report = app.reportRepo.activeReport
        createdReports.add(report)
        awaitView(withId(R.id.project_name))
        return report
    }

    @Test
    fun theNewReportButtonOpensTheEditorWithAnEmptyReport() {
        val report = newReportFromList()

        assertThat(report.project.name.value).isEmpty()
        assertThat(report.state.value).isEqualTo(ReportData.ReportState.IN_WORK)
        onView(withId(R.id.project_name)).check(matches(isDisplayed()))
    }

    @Test
    fun theProjectSectionIsStoredOnTheReport() {
        val report = newReportFromList()

        onView(withId(R.id.project_name))
            .perform(scrollTo(), replaceText("Kunde $marker"), closeSoftKeyboard())
        onView(withId(R.id.project_extra1))
            .perform(scrollTo(), replaceText(marker), closeSoftKeyboard())

        awaitUntil("the project name to reach the report") {
            report.project.name.value == "Kunde $marker"
        }
        assertThat(report.project.extra1.value).isEqualTo(marker)
    }

    @Test
    fun theReportIdFollowsTheProjectNameWhenThePatternUsesIt() {
        onMain { app.prefs.reportIdPattern.value = "%k-%z" }
        val report = newReportFromList()
        // id is a MediatorLiveData over the pattern and the project fields, so it only
        // recomputes while something observes it.
        onMain { report.id.observeForever { } }

        onView(withId(R.id.project_name))
            .perform(scrollTo(), replaceText("Kunde"), closeSoftKeyboard())
        onView(withId(R.id.project_extra1))
            .perform(scrollTo(), replaceText(marker), closeSoftKeyboard())

        awaitUntil("the id to be rebuilt") { report.id.value == "Kunde-$marker" }
        assertThat(report.id.value).isEqualTo("Kunde-$marker")
    }

    @Test
    fun theBillAddressIsStoredOnTheReport() {
        val report = newReportFromList()

        // The bill section starts collapsed; its headline toggles it
        onView(allOf(withId(R.id.resh_headline_textview), withText(R.string.bill_address)))
            .perform(scrollTo(), click())
        awaitView(withId(R.id.bill_address_name))

        onView(withId(R.id.bill_address_name))
            .perform(scrollTo(), replaceText("Rechnung $marker"), closeSoftKeyboard())
        onView(withId(R.id.bill_address_street))
            .perform(scrollTo(), replaceText("Hauptstr. 1"), closeSoftKeyboard())
        onView(withId(R.id.bill_address_zip))
            .perform(scrollTo(), replaceText("12345"), closeSoftKeyboard())
        onView(withId(R.id.bill_address_city))
            .perform(scrollTo(), replaceText("Musterstadt"), closeSoftKeyboard())

        awaitUntil("the bill name to reach the report") {
            report.bill.name.value == "Rechnung $marker"
        }
        assertThat(report.bill.street.value).isEqualTo("Hauptstr. 1")
        assertThat(report.bill.zip.value).isEqualTo("12345")
        assertThat(report.bill.city.value).isEqualTo("Musterstadt")
    }

    @Test
    fun editsSurviveBeingWrittenToAndReloadedFromTheDatabase() {
        val report = newReportFromList()
        onView(withId(R.id.project_name))
            .perform(scrollTo(), replaceText("Persistiert $marker"), closeSoftKeyboard())
        awaitUntil("the edit to reach the report") {
            report.project.name.value == "Persistiert $marker"
        }

        // Force the report out to the database and back in. The repository touches its
        // cache and its main-thread scope, so drive it from the main thread.
        onMain { app.reportRepo.saveReport(report) }
        awaitUntil("the report to be saved") { !report.modified }

        var reloaded: ReportData? = null
        onMain { app.reportRepo.setActiveReport(report.cnt) { reloaded = it } }
        awaitUntil("the report to be reloaded") { reloaded != null }

        assertThat(reloaded!!.project.name.value).isEqualTo("Persistiert $marker")
    }

    @Test
    fun aClearedFieldIsClearedOnTheReportToo() {
        val report = newReportFromList()
        onView(withId(R.id.project_name))
            .perform(scrollTo(), replaceText("Zu loeschen"), closeSoftKeyboard())
        awaitUntil("the text to be set") { report.project.name.value == "Zu loeschen" }

        onView(withId(R.id.project_name)).perform(scrollTo(), clearText(), closeSoftKeyboard())

        awaitUntil("the text to be cleared") { report.project.name.value.isNullOrEmpty() }
    }

    @Test
    fun theSummaryScreenCanBeReachedFromTheEditor() {
        newReportFromList()

        onView(withId(R.id.goto_summary)).perform(click())

        intended(hasComponent(SummaryActivity::class.java.name))
        awaitView(withId(R.id.webview))
    }
}
