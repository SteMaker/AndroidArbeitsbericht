package com.stemaker.arbeitsbericht

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitAppInitialised
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitUntil
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitView
import com.stemaker.arbeitsbericht.support.UiTestSupport.onMain
import com.stemaker.arbeitsbericht.support.UiTestSupport.prefs
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/* The settings screen is the only way a user configures report ids, the mail recipient and
 * the output format, and everything it writes goes through AbPreferences. These tests drive
 * the real screen and read the result back out of the preferences.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConfigurationActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ConfigurationActivity::class.java)

    private val marker = Random.nextInt(100_000, 999_999).toString()

    private lateinit var originalEmployee: String
    private lateinit var originalDevice: String
    private lateinit var originalPattern: String
    private lateinit var originalMail: String

    @Before
    fun setUp() {
        awaitAppInitialised()
        awaitView(withId(R.id.config_employee_name))
        originalEmployee = prefs.employeeName.value
        originalDevice = prefs.deviceName.value
        originalPattern = prefs.reportIdPattern.value
        originalMail = prefs.recvMail.value
    }

    @After
    fun tearDown() {
        onMain {
            prefs.employeeName.value = originalEmployee
            prefs.deviceName.value = originalDevice
            prefs.reportIdPattern.value = originalPattern
            prefs.recvMail.value = originalMail
        }
    }

    @Test
    fun theGeneralSettingsAreWrittenToThePreferences() {
        onView(withId(R.id.config_employee_name))
            .perform(scrollTo(), replaceText("Mitarbeiter $marker"), closeSoftKeyboard())
        onView(withId(R.id.config_device_name))
            .perform(scrollTo(), replaceText("Geraet $marker"), closeSoftKeyboard())
        onView(withId(R.id.config_report_id_pattern))
            .perform(scrollTo(), replaceText("%p-%y-%4c"), closeSoftKeyboard())
        onView(withId(R.id.config_mail_receiver))
            .perform(scrollTo(), replaceText("app.stemaker@gmail.com"), closeSoftKeyboard())

        awaitUntil("the employee name to be stored") {
            prefs.employeeName.value == "Mitarbeiter $marker"
        }
        assertThat(prefs.deviceName.value).isEqualTo("Geraet $marker")
        assertThat(prefs.reportIdPattern.value).isEqualTo("%p-%y-%4c")
        assertThat(prefs.recvMail.value).isEqualTo("app.stemaker@gmail.com")
    }

    @Test
    fun theSettingsAreStillThereAfterTheScreenIsRecreated() {
        onView(withId(R.id.config_employee_name))
            .perform(scrollTo(), replaceText("Dauerhaft $marker"), closeSoftKeyboard())
        awaitUntil("the name to be stored") {
            prefs.employeeName.value == "Dauerhaft $marker"
        }

        activityRule.scenario.recreate()
        awaitView(withId(R.id.config_employee_name))

        onView(withId(R.id.config_employee_name))
            .perform(scrollTo())
        onView(withId(R.id.config_employee_name))
            .check(matches(withText("Dauerhaft $marker")))
    }

    @Test
    fun theSaveButtonLeavesTheSettingsScreen() {
        onView(withId(R.id.config_employee_name))
            .perform(scrollTo(), replaceText("Gespeichert $marker"), closeSoftKeyboard())

        onView(withId(R.id.config_save_button)).perform(click())

        awaitUntil("the name to be stored") {
            prefs.employeeName.value == "Gespeichert $marker"
        }
    }

    @Test
    fun theCrashlyticsToggleIsWrittenToThePreferences() {
        val before = prefs.crashlyticsEnable.value
        onView(withId(R.id.crashlog_enable)).perform(scrollTo())
        onView(withId(R.id.crashlog_enable))
            .check(matches(if (before) isChecked() else isNotChecked()))

        onView(withId(R.id.crashlog_enable)).perform(click())

        awaitUntil("the toggle to be stored") { prefs.crashlyticsEnable.value != before }
        // Put it back
        onView(withId(R.id.crashlog_enable)).perform(click())
        awaitUntil("the toggle to be restored") { prefs.crashlyticsEnable.value == before }
    }

    @Test
    fun anEmptyReportIdPatternIsAcceptedWithoutCrashing() {
        // The pattern is free text, so the app has to cope with nonsense in it
        onView(withId(R.id.config_report_id_pattern))
            .perform(scrollTo(), replaceText(""), closeSoftKeyboard())

        awaitUntil("the empty pattern to be stored") { prefs.reportIdPattern.value.isEmpty() }
        onView(withId(R.id.config_employee_name)).perform(scrollTo())
    }
}
