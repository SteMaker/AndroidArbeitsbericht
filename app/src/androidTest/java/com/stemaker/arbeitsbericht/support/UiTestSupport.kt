package com.stemaker.arbeitsbericht.support

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.stemaker.arbeitsbericht.ArbeitsberichtApp
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import org.hamcrest.Matcher
import android.view.View

/* The app does its startup work (preferences, database, report list) in coroutines launched
 * on GlobalScope, which Espresso's idling machinery knows nothing about. On top of that
 * every activity shows a wait screen and then recreates itself once that work finishes. So
 * tests cannot rely on Espresso being "idle" meaning "ready", and instead wait for the view
 * they actually need to turn up.
 */
object UiTestSupport {

    val app: ArbeitsberichtApp
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            as ArbeitsberichtApp

    val prefs: AbPreferences
        get() {
            awaitAppInitialised()
            return app.prefs
        }

    /* Runs [block] on the main thread and waits for it.
     *
     * Test methods run on the instrumentation thread, but the app's preferences and filter
     * are LiveData, which refuses setValue() off the main thread. Hopping threads is the
     * right fix here; swapping in InstantTaskExecutorRule would also change how the real
     * activities under test receive their updates.
     */
    fun <T> onMain(block: () -> T): T {
        var result: T? = null
        var failure: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            try {
                result = block()
            } catch (t: Throwable) {
                failure = t
            }
        }
        failure?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /* Blocks until the application's startup job has finished. */
    fun awaitAppInitialised(timeoutMs: Long = 20_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val job = app.initJob
            if (job != null && job.isCompleted) return
            Thread.sleep(25)
        }
        throw AssertionError("application initialisation did not finish within $timeoutMs ms")
    }

    /* Waits until a view matching [matcher] is displayed, then returns. */
    fun awaitView(matcher: Matcher<View>, timeoutMs: Long = 15_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (e: NoMatchingViewException) {
                last = e
            } catch (e: AssertionError) {
                last = e
            }
            Thread.sleep(50)
        }
        throw AssertionError("view never appeared: $matcher", last)
    }

    /* Waits until [condition] holds. Used for state the app writes asynchronously. */
    fun awaitUntil(what: String, timeoutMs: Long = 15_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            Thread.sleep(25)
        }
        if (!condition()) throw AssertionError("timed out waiting for: $what")
    }

    /* MainActivity shows the changelog dialog whenever the stored version code is older
     * than the installed one, which is the case for every MainActivity created in a test
     * run that starts from a fresh install.
     *
     * It has to go before anything else is looked up: it is a DialogFragment, so it owns
     * the focused window, and Espresso resolves view matchers against that window. While it
     * is up, nothing in the activity behind it is findable.
     */
    fun dismissVersionDialogIfShown(waitMs: Long = 3_000) {
        val deadline = System.currentTimeMillis() + waitMs
        var shown = false
        while (System.currentTimeMillis() < deadline && !shown) {
            shown = isDisplayed(withId(R.id.ok_button))
            if (!shown) Thread.sleep(50)
        }
        if (!shown) return
        onView(withId(R.id.ok_button)).perform(click())
        awaitUntil("the changelog dialog to go away") { !isDisplayed(withId(R.id.ok_button)) }
    }

    private fun isDisplayed(matcher: Matcher<View>): Boolean = try {
        onView(matcher).check(matches(isDisplayed()))
        true
    } catch (e: NoMatchingViewException) {
        false
    } catch (e: AssertionError) {
        false
    }

    /* Narrows the report list to a marker only this test's reports carry, so assertions do
     * not depend on whatever reports the device already holds. Uses the app's own filter,
     * which is a real feature rather than a test back door.
     */
    fun isolateReportsBy(marker: String) {
        awaitAppInitialised()
        onMain {
            app.reportRepo.filter.blockUpdate = true
            app.reportRepo.filter.projectExtra = marker
            app.reportRepo.filter.inWork = true
            app.reportRepo.filter.blockUpdate = false
        }
    }

    fun resetReportFilter() = onMain {
        app.reportRepo.filter.blockUpdate = true
        app.reportRepo.filter.projectName = ""
        app.reportRepo.filter.projectExtra = ""
        app.reportRepo.filter.blockUpdate = false
    }
}
