package com.stemaker.arbeitsbericht

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.data.*
import com.stemaker.arbeitsbericht.databinding.ActivityReportEditorBinding
import com.stemaker.arbeitsbericht.editor_fragments.*

class ReportEditorActivity : AppCompatActivity(),
    ProjectEditorFragment.OnProjectEditorInteractionListener,
    BillEditorFragment.OnBillEditorInteractionListener,
    WorkTimeEditorFragment.OnWorkTimeEditorInteractionListener,
    WorkItemEditorFragment.OnWorkItemEditorInteractionListener,
    MaterialEditorFragment.OnMaterialEditorInteractionListener,
    LumpSumEditorFragment.OnLumpSumEditorInteractionListener,
    PhotoEditorFragment.OnPhotoEditorInteractionListener {

    lateinit var topBinding : ActivityReportEditorBinding
    /*****************/
    /* General stuff */
    /*****************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht.ReportEditorActivity.onCreate", "start")

        topBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_editor)

        setSupportActionBar(findViewById(R.id.report_editor_activity_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.editor)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.editor_menu, menu)
        return true
    }

    override fun onStart() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onStart", "called")
        super.onStart()
    }

    override fun onPause() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onPause", "called")
        super.onPause()
        saveReport()
    }

    override fun onStop() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onStop", "called")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onDestroy", "called")
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.goto_summary -> {
                saveReport()
                val intent = Intent(this, SummaryActivity::class.java).apply {}
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun saveAndBackToMain() {
        Log.d("Arbeitsbericht", "Kundenname ist ${storageHandler().getReport().project.name.value}")
        Log.d("Arbeitsbericht.debug","There are ${storageHandler().activeReport.photoContainer.items.size} photos, , object: ${storageHandler().activeReport.photoContainer.toString()}")
        saveReport()
        val intent = Intent(this, MainActivity::class.java).apply {}
        Log.d("Arbeitsbericht", "Switching to main activity")
        startActivity(intent)
    }

    override fun onBackPressed() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onBackPressed", "called")
        saveAndBackToMain()
    }

    fun saveReport() {
        storageHandler().saveActiveReportToFile(getApplicationContext())
    }

    override fun getProjectData(): ProjectData {
        return storageHandler().getReport().project
    }

    override fun getBillData(): BillData {
        return storageHandler().getReport().bill
    }

    override fun getWorkTimeContainerData(): WorkTimeContainerData {
        return storageHandler().getReport().workTimeContainer
    }

    override fun getWorkItemContainerData(): WorkItemContainerData {
        return storageHandler().getReport().workItemContainer
    }

    override fun getMaterialContainerData(): MaterialContainerData {
        return storageHandler().getReport().materialContainer
    }

    override fun getLumpSumContainerData(): LumpSumContainerData {
        return storageHandler().getReport().lumpSumContainer
    }

    override fun getPhotoContainerData(): PhotoContainerData {
        return storageHandler().getReport().photoContainer
    }
}
