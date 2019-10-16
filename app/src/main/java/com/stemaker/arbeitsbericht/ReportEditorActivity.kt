package com.stemaker.arbeitsbericht

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.content.Intent
import android.R.*
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.data.*
import com.stemaker.arbeitsbericht.databinding.ActivityReportEditorBinding

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

        // If no lump sums are defined in the configuration yet, then we present a hint
        // and disable the add button
        /*
        if (storageHandler().configuration.lumpSums.size == 0) {
            findViewById<TextView>(R.id.lump_sum_undef_hint).visibility = View.VISIBLE
            findViewById<ImageButton>(R.id.lump_sum_add_button).visibility = View.GONE
        }*/

        if (savedInstanceState != null) {
            Log.d("Arbeitsbericht.ReportEditorActivity.onCreate", "restoring active report ${savedInstanceState.getInt("activeReport").toString()}")
            storageHandler().selectReportById(savedInstanceState.getInt("activeReport"))
        }
        loadReports()
    }

    override fun onStart() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onStart", "called")
        super.onStart()
    }

    override fun onResume() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onResume", "called")
        super.onResume()
        updateReports()
    }

    override fun onPause() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onPause", "called")
        super.onPause()
    }

    override fun onStop() {
        Log.d("Arbeitsbericht.ReportEditorActivity.onStop", "called")
        super.onStop()
        saveReport()
    }

    fun saveAndBackToMain() {
        Log.d("Arbeitsbericht", "Kundenname ist ${storageHandler().getReport().project.name.value}")
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

    fun loadReports() {
        val report = storageHandler().getReport()
    }

    fun updateReports() {
        val report = storageHandler().getReport()

        //findViewById<EditText>(R.id.client_name).setText(report.client_name)
        //findViewById<EditText>(R.id.client_extra1).setText(report.client_extra1)
    }

    fun saveReport() {
        val report = storageHandler().getReport()

        //report.client_name = findViewById<EditText>(R.id.client_name).getText().toString()
        //if (report.client_name == "") report.client_name = getString(R.string.unknown)
        //report.client_extra1 = findViewById<EditText>(R.id.client_extra1).getText().toString()

        storageHandler().saveReportToFile(report, getApplicationContext())
    }

    fun onClickExpandContentButton(expandProjectButton: View) {
        Log.d("arbeitsbericht", "project expand clicked")
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
