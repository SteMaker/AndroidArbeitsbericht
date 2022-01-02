package com.stemaker.arbeitsbericht

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.data.*
import com.stemaker.arbeitsbericht.databinding.ActivityReportEditorBinding
import com.stemaker.arbeitsbericht.editor_fragments.*
import com.stemaker.arbeitsbericht.helpers.OrientationNotificationDialogFragment
import kotlinx.coroutines.*

private const val TAG = "ReportEditorActivity"

class ReportEditorActivity() : AppCompatActivity(),
    ReportEditorSectionFragment.OnReportEditorInteractionListener,
    OrientationNotificationDialogFragment.ForcePortraitListener {

    lateinit var topBinding : ActivityReportEditorBinding
    var storageInitJob: Job? = null

    /*****************/
    /* General stuff */
    /*****************/
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = when(configuration().lockScreenOrientation) {
            true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        super.onCreate(savedInstanceState)
        storageInitJob = storageHandler().initialize()
        topBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_editor)

        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        GlobalScope.launch(Dispatchers.Main) {
            delay(200)
            storageInitJob?.let {
                if (!it.isCompleted) {
                    topBinding.progressBar.visibility = View.VISIBLE
                    topBinding.loadNotify.visibility = View.VISIBLE
                    it.join()
                    topBinding.progressBar.visibility = View.GONE
                    topBinding.loadNotify.visibility = View.GONE
                }
            } ?: run { Log.e(TAG, "storageHandler job was null :(") }

            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }

        topBinding.reportEditorActivityToolbar.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.goto_summary -> {
                    val intent = Intent(this, SummaryActivity::class.java).apply {}
                    GlobalScope.launch(Dispatchers.Main) {
                        storageHandler().saveActiveReport()
                        startActivity(intent)
                    }
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        topBinding.reportEditorActivityToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        if(!configuration().lockScreenOrientationNoInfo && !configuration().lockScreenOrientation &&
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val screenOrientationDialog = OrientationNotificationDialogFragment()
            screenOrientationDialog.setForcePortraitListener(this)
            screenOrientationDialog.show(supportFragmentManager, "Orientation dialog")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    override fun forcePortrait() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        recreate()
    }

    override fun onStop() {
        super.onStop()
        runBlocking {
            storageHandler().saveActiveReport()
        }
    }

    private suspend fun waitForStorageHandler() {
        storageInitJob?.let {
            it.join()
        }
    }

    override suspend fun getReportData(): ReportData {
        waitForStorageHandler()
        return storageHandler().getReport()!!
    }
}
