package com.stemaker.arbeitsbericht

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_report_editor.*
import android.content.Intent
import android.content.Context
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import android.R.*
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.databinding.ActivityReportEditorBinding

var myTestText: String = "A"

class ReportEditorActivity : AppCompatActivity(),
    ProjectEditorFragment.OnProjectEditorInteractionListener,
    BillEditorFragment.OnBillEditorInteractionListener {

    lateinit var topBinding : ActivityReportEditorBinding
    //var clientHeadBinding : ReportEditorSectionLayoutBinding? = null
    /*****************/
    /* General stuff */
    /*****************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht.ReportEditorActivity.onCreate", "start")

        topBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_editor)
/*        val csc = findViewById<View>(R.id.client_section_container)
        clientHeadBinding = DataBindingUtil.getBinding(csc)
        clientHeadBinding?.reslHeadlineText = "Projekt / Kunde"
*/
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
        storageHandler().saveMaterialDictionaryToFile(getApplicationContext())
        storageHandler().saveWorkItemDictionaryToFile(getApplicationContext())
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
}
