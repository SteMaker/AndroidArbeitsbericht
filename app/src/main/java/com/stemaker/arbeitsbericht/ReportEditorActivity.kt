package com.stemaker.arbeitsbericht

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.content.Intent
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

        /*if (savedInstanceState != null) {
            Log.d("Arbeitsbericht.ReportEditorActivity.onCreate", "restoring active report ${savedInstanceState.getInt("activeReport").toString()}")
            StorageHandler.selectReportById(savedInstanceState.getInt("activeReport"))
        }*/
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

    fun saveAndBackToMain() {
        Log.d("Arbeitsbericht", "Kundenname ist ${StorageHandler.getReport().project.name.value}")
        Log.d("Arbeitsbericht.debug","There are ${storageHandler().activeReport.photoContainer.items.size} photos, , object: ${storageHandler().activeReport.photoContainer.toString()}")
        saveReport()
        val intent = Intent(this, MainActivity::class.java).apply {}
        Log.d("Arbeitsbericht", "Switching to main activity")
        startActivity(intent)
    }

    fun onClickBack(@Suppress("UNUSED_PARAMETER") backButton: View) {
        Log.d("Arbeitsbericht.ReportEditorActivity.onClickBack", "called")
        saveAndBackToMain()
    }

    override fun onBackPressed() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onBackPressed", "called")
        saveAndBackToMain()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt("activeReport", storageHandler().activeReport.id.value!!)
    }

    fun onClickSummary(@Suppress("UNUSED_PARAMETER") btn: View) {
        saveReport()
        val intent = Intent(this, SummaryActivity::class.java).apply {}
        startActivity(intent)
    }

    fun saveReport() {
        storageHandler().saveActiveReportToFile(getApplicationContext())
    }

    fun onClickExpandContentButton(expandProjectButton: View) {
        Log.d("arbeitsbericht", "project expand clicked")
    }

    override fun getProjectData(): ProjectData {
        return StorageHandler.getReport().project
    }

    override fun getBillData(): BillData {
        return StorageHandler.getReport().bill
    }

    override fun getWorkTimeContainerData(): WorkTimeContainerData {
        return StorageHandler.getReport().workTimeContainer
    }

    override fun getWorkItemContainerData(): WorkItemContainerData {
        return StorageHandler.getReport().workItemContainer
    }

    override fun getMaterialContainerData(): MaterialContainerData {
        return StorageHandler.getReport().materialContainer
    }

    override fun getLumpSumContainerData(): LumpSumContainerData {
        return StorageHandler.getReport().lumpSumContainer
    }

    override fun getPhotoContainerData(): PhotoContainerData {
        return StorageHandler.getReport().photoContainer
    }
}
