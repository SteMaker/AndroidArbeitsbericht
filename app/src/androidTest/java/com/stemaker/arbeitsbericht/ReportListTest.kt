package com.stemaker.arbeitsbericht

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/* The report list is the app's home screen. Its contents come from the repository's filter,
 * and the per-report actions (delete, duplicate, change state) are what a user reaches for
 * most often. The repository drives all of it, so these tests go through the repository and
 * check the list on screen follows.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ReportListTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val marker = "L${Random.nextInt(100_000, 999_999)}"
    private val created = mutableListOf<ReportData>()

    @Before
    fun setUp() {
        awaitAppInitialised()
        dismissVersionDialogIfShown()
        awaitView(withId(R.id.report_list_rv))
    }

    @After
    fun tearDown() {
        onMain { created.forEach { app.reportRepo.deleteReport(it) } }
        resetReportFilter()
    }

    private fun addReport(projectName: String): ReportData {
        val report = onMain { app.reportRepo.createReportAndActivate(null) }
        awaitUntil("the report to be listed") { app.reportRepo.isReportVisible(report.cnt) }
        onMain {
            report.id.observeForever { }
            report.project.name.value = projectName
            report.project.extra1.value = marker
            app.reportRepo.saveReport(report)
        }
        awaitUntil("the report to be saved") { !report.modified }
        created.add(report)
        return report
    }

    @Test
    fun aReportAppearsInTheListWithItsProjectName() {
        val report = addReport("Mueller $marker")

        awaitView(withText("Mueller $marker"))
        assertThat(app.reportRepo.isReportVisible(report.cnt)).isTrue()
    }

    @Test
    fun deletingAReportTakesItOutOfTheListAndTheDatabase() {
        val report = addReport("Zu loeschen $marker")
        awaitView(withText("Zu loeschen $marker"))

        onMain { app.reportRepo.deleteReport(report) }

        awaitUntil("the report to disappear") { !app.reportRepo.isReportVisible(report.cnt) }
        created.remove(report)
    }

    @Test
    fun duplicatingAReportProducesASecondEntry() {
        val report = addReport("Vorlage $marker")
        awaitView(withText("Vorlage $marker"))
        val before = app.reportRepo.amountOfReports

        val duplicate = onMain { app.reportRepo.duplicateReportAndActivate(report, null) }
        awaitUntil("the duplicate to be listed") {
            app.reportRepo.amountOfReports == before + 1
        }
        created.add(duplicate)

        assertThat(duplicate.cnt).isNotEqualTo(report.cnt)
        assertThat(duplicate.project.name.value).isEqualTo("Vorlage $marker")
    }

    @Test
    fun aReportSetToArchivedDisappearsUnderTheDefaultFilter() {
        // The default filter shows in-work, on-hold and done, but not archived
        val report = addReport("Archiviert $marker")
        awaitView(withText("Archiviert $marker"))

        onMain {
            report.state.value = ReportData.ReportState.ARCHIVED
            app.reportRepo.saveReport(report)
        }
        awaitUntil("the report to be saved") { !report.modified }
        // Re-apply the filter, which is what the list does on any filter change
        onMain { app.reportRepo.filter.archived = true }
        awaitUntil("archived reports to show") { app.reportRepo.isReportVisible(report.cnt) }
        onMain { app.reportRepo.filter.archived = false }

        awaitUntil("the archived report to be filtered out") {
            !app.reportRepo.isReportVisible(report.cnt)
        }
    }

    @Test
    fun theProjectNameFilterNarrowsTheListToMatchingReports() {
        val wanted = addReport("Gesucht $marker")
        val other = addReport("Anderer $marker")
        awaitView(withText("Gesucht $marker"))

        onMain { app.reportRepo.filter.projectName = "Gesucht" }

        awaitUntil("the list to narrow down") {
            app.reportRepo.isReportVisible(wanted.cnt) &&
                !app.reportRepo.isReportVisible(other.cnt)
        }
        awaitView(withText("Gesucht $marker"))
    }

    @Test
    fun clearingTheFilterBringsTheOtherReportsBack() {
        val wanted = addReport("Gesucht $marker")
        val other = addReport("Anderer $marker")
        onMain { app.reportRepo.filter.projectName = "Gesucht" }
        awaitUntil("the list to narrow down") { !app.reportRepo.isReportVisible(other.cnt) }

        onMain { app.reportRepo.filter.projectName = "" }

        awaitUntil("both reports to be listed again") {
            app.reportRepo.isReportVisible(wanted.cnt) &&
                app.reportRepo.isReportVisible(other.cnt)
        }
    }

    @Test
    fun tappingAReportInTheListOpensTheEditor() {
        addReport("Antippen $marker")
        awaitView(withText("Antippen $marker"))

        // The newest report is at position 0, which is the one just added. Clicking the
        // holder hits the card root, where the list adapter puts its click listener.
        onView(withId(R.id.report_list_rv)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // The editor shows the project section for the report that was tapped
        awaitView(withId(R.id.project_name))
    }

    @Test
    fun theReportListSurvivesTheActivityBeingRecreated() {
        addReport("Neustart $marker")
        awaitView(withText("Neustart $marker"))

        activityRule.scenario.recreate()

        dismissVersionDialogIfShown()
        awaitView(withId(R.id.report_list_rv))
        awaitView(withText("Neustart $marker"))
    }
}
