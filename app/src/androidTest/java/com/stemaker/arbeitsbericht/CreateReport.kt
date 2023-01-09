package com.stemaker.arbeitsbericht

import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.ComponentNameMatchers.hasClassName
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.core.AllOf.allOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateReport{
    @get:Rule
    val activityRuleMain = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val intentsTestRule = IntentsTestRule(MainActivity::class.java)


    @Test fun createReport() {
        val random = Random.nextLong()
        onView(withId(R.id.new_report_button))
            .perform(click())
        intended(hasComponent(hasClassName(ReportEditorActivity::class.java.name)))
        onView(withId(R.id.project_name))
            .perform(scrollTo(), typeText("Kundenname $random"))
        onView(withId(R.id.project_extra1))
            .perform(scrollTo(), typeText("Kundenzusatz $random"))
        // Projektbereich schließen
        onView(allOf(withId(R.id.resh_headline_textview), withText("Projekt / Kunde")))
            .perform(scrollTo(), click())
        // Rechnungsbereich öffnen
        onView(allOf(withId(R.id.resh_headline_textview), withText("Rechnungsadresse")))
            .perform(scrollTo(), click())
        onView(withId(R.id.bill_address_name))
            .perform(scrollTo(), typeText(("Rechnung Name $random")))
        onView(withId(R.id.bill_address_street))
            .perform(scrollTo(), typeText(("Rechnung Strasse $random")))
        onView(withId(R.id.bill_address_zip))
            .perform(scrollTo(), typeText(("12345")))
        onView(withId(R.id.bill_address_city))
            .perform(scrollTo(), typeText(("Rechnung Stadt $random")))
        // Rechnungsbereich schließen
        onView(allOf(withId(R.id.resh_headline_textview), withText("Rechnungsadresse")))
            .perform(scrollTo(), click())
        activityRuleMain.scenario.close()
    }
}
