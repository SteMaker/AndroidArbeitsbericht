package com.stemaker.arbeitsbericht

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.content.Intent
import android.view.*
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.data.*
import com.stemaker.arbeitsbericht.databinding.ActivityReportEditorBinding
import com.stemaker.arbeitsbericht.editor_fragments.*
import kotlinx.coroutines.*

private const val TAG = "ReportEditorActivity"

class ReportEditorActivity : AppCompatActivity(),
    ProjectEditorFragment.OnProjectEditorInteractionListener,
    BillEditorFragment.OnBillEditorInteractionListener,
    WorkTimeEditorFragment.OnWorkTimeEditorInteractionListener,
    WorkItemEditorFragment.OnWorkItemEditorInteractionListener,
    MaterialEditorFragment.OnMaterialEditorInteractionListener,
    LumpSumEditorFragment.OnLumpSumEditorInteractionListener,
    PhotoEditorFragment.OnPhotoEditorInteractionListener {

    lateinit var topBinding : ActivityReportEditorBinding
    var storageInitJob: Job? = null
    /*****************/
    /* General stuff */
    /*****************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageInitJob = storageHandler().initialize()
        topBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_editor)

        setSupportActionBar(findViewById(R.id.report_editor_activity_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.editor)

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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.editor_menu, menu)
        return true
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
        runBlocking {
            storageHandler().saveActiveReport()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.goto_summary -> {
                val intent = Intent(this, SummaryActivity::class.java).apply {}
                GlobalScope.launch(Dispatchers.Main) {
                    storageHandler().saveActiveReport()
                    startActivity(intent)
                }
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private suspend fun waitForStorageHandler() {
        storageInitJob?.let {
            it.join()
        }
    }

    override suspend fun getProjectData(): ProjectData {
        waitForStorageHandler()
        return storageHandler().getReport().project
    }

    override suspend fun getBillData(): BillData {
        waitForStorageHandler()
        return storageHandler().getReport().bill
    }

    override suspend fun getWorkTimeContainerData(): WorkTimeContainerData {
        waitForStorageHandler()
        return storageHandler().getReport().workTimeContainer
    }

    override suspend fun getWorkItemContainerData(): WorkItemContainerData {
        waitForStorageHandler()
        return storageHandler().getReport().workItemContainer
    }

    override suspend fun getMaterialContainerData(): MaterialContainerData {
        waitForStorageHandler()
        return storageHandler().getReport().materialContainer
    }

    override suspend fun getLumpSumContainerData(): LumpSumContainerData {
        waitForStorageHandler()
        return storageHandler().getReport().lumpSumContainer
    }

    override suspend fun getPhotoContainerData(): PhotoContainerData {
        waitForStorageHandler()
        return storageHandler().getReport().photoContainer
    }
}
