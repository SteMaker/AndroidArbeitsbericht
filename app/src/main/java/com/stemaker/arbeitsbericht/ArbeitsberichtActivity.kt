package com.stemaker.arbeitsbericht

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.databinding.WaitingStartupLayoutBinding
import kotlinx.coroutines.*

private const val TAG = "ArbeitsberichtActivity"

abstract class ArbeitsberichtActivity(): AppCompatActivity() {
    lateinit var waitScreenBinding: WaitingStartupLayoutBinding
    lateinit var app: ArbeitsberichtApp
    lateinit var prefs: AbPreferences

    override fun onStop() {
        super.onStop()
        runBlocking {
            if(app.waitForShutdown()) {
                Log.e(TAG, "Error: A shutdown job at application could not finish in time")
            }
        }
    }

    /* From Android 15 on, apps targeting SDK 35 or above always get an edge-to-edge window:
     * the system bars no longer reserve space and content is drawn underneath them. All of
     * our layouts expect to own their area, so we inset the content view by whatever the
     * system bars and the display cutout occupy. onContentChanged is called by
     * AppCompatActivity right after setContentView, so every activity gets this for free.
     */
    override fun onContentChanged() {
        super.onContentChanged()
        val content = findViewById<View>(android.R.id.content) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(
                left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            windowInsets
        }
    }

    /* Registers a back handler that runs [onBack] and then lets the default back behaviour
     * proceed. Overriding onBackPressed() is not an option anymore: with targetSdk 36 and
     * above the predictive back gesture is enabled by default, and the framework then routes
     * back presses through OnBackPressedDispatcher without ever calling onBackPressed().
     */
    protected fun addBackPressedHandler(onBack: suspend () -> Unit) {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                lifecycleScope.launch {
                    onBack()
                    // Disable ourselves so the dispatcher falls through to the default
                    // handling (finishing the activity) instead of calling us again.
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    fun onCreateWrapper(savedInstanceState: Bundle?): Boolean {
        app = application as ArbeitsberichtApp
        val job = app.initJob?:GlobalScope.launch(Dispatchers.Main) {
            Log.e(TAG, "Error: Application initialization job non-existsnt during activity startup")
            delay(1000)
        }
        return if(job.isCompleted) {
            prefs = app.prefs
            true
        } else {
            super.onCreate(savedInstanceState)
            waitScreenBinding = DataBindingUtil.setContentView(this, R.layout.waiting_startup_layout)
            GlobalScope.launch(Dispatchers.Main) {
                job.join()
                recreate()
            }
            false
        }
    }
}
