package com.stemaker.arbeitsbericht

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@LargeTest
class SetupConfig {
    @get:Rule
    val activityRuleMain = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun configureApp() {
        val random = Random.nextLong()
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext())
        onView(withText("Einstellungen"))
            .perform(click())
        onView(withId(R.id.config_employee_name))
            .perform(scrollTo(), clearText(), typeText("Mitarbeiter $random"))
        onView(withId(R.id.config_device_name))
            .perform(scrollTo(), clearText(), typeText("Geraet $random"))
        onView(withId(R.id.config_report_id_pattern))
            .perform(scrollTo(), clearText(), typeText("%p-%y-%4c"))
        onView(withId(R.id.config_mail_receiver))
            .perform(scrollTo(), clearText(), typeText("app.stemaker@gmail.com"))
        onView(withId(R.id.radio_select_output))
            .perform(scrollTo(), click())
        onView(withId(R.id.xlsx_use_logo))
            .perform(scrollTo(), click())
        onView(withId(R.id.xlsx_use_footer))
            .perform(scrollTo(), click())
        onView(withId(R.id.radio_pdf_output))
            .perform(scrollTo(), click())
        onView(withId(R.id.pdf_use_logo))
            .perform(scrollTo(), click())
        onView(withId(R.id.pdf_use_footer))
            .perform(scrollTo(), click())
        onView(withId(R.id.config_save_button))
            .perform(click())
    }
}