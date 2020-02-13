package com.stemaker.arbeitsbericht.hoursreport

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.StorageHandler
import com.stemaker.arbeitsbericht.data.hoursreport.HoursReportData
import com.stemaker.arbeitsbericht.data.workreport.ReportData
import com.stemaker.arbeitsbericht.databinding.ActivityHoursMainBinding
import com.stemaker.arbeitsbericht.databinding.ActivityMainBinding
import com.stemaker.arbeitsbericht.databinding.HoursReportCardLayoutBinding
import com.stemaker.arbeitsbericht.databinding.ReportCardLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import com.stemaker.arbeitsbericht.workreport.ReportEditorActivity
import com.stemaker.arbeitsbericht.workreport.WorkReportMainActivity
import kotlinx.android.synthetic.main.activity_hours_main.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "HoursReportMainActivity"

class HoursMainActivity : AppCompatActivity() {
    lateinit var topBinding: ActivityHoursMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        topBinding = DataBindingUtil.setContentView(this, R.layout.activity_hours_main)
        topBinding.lifecycleOwner = this
        // Nothing to bind right now topBinding.... = ...!!

        val toolbar = findViewById<Toolbar>(R.id.hours_main_activity_toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_apps_black_24dp)
        toolbar.setNavigationOnClickListener {
            val popup = PopupMenu(this@HoursMainActivity, toolbar)
            popup.menuInflater.inflate(R.menu.tasks_menu, popup.getMenu())
            popup.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.app_arbeitsbericht -> {
                        val intent = Intent(this, WorkReportMainActivity::class.java).apply {}
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        supportActionBar?.setTitle(R.string.saved_hours_reports)
        supportActionBar?.setHomeButtonEnabled(true)

        val reportListScrollContainer = hours_report_list_scroll_container
        val reportIds = StorageHandler.getListOfHoursReports()
        reportIds.forEach {
            // Get the respective report
            val rep: HoursReportData = StorageHandler.getHoursReportById(it, getApplicationContext())
            Log.d("Arbeitsbericht", "Report with ID: ${rep.id}")
            // Prepare a report_card_layout instance
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val reportBinding: HoursReportCardLayoutBinding = DataBindingUtil.inflate(inflater,
                R.layout.hours_report_card_layout, null, false)
            // Bind in the data
            reportBinding.reportData = rep

            // Assign the report ID as tag to the button to know which one to delete
            val btnDel = reportBinding.root.findViewById<ImageButton>(R.id.hours_report_card_del_button)
            btnDel.setTag(R.id.TAG_REPORT_ID, rep.id)
            btnDel.setTag(R.id.TAG_CARDVIEW, reportBinding.root)
            reportBinding.root.setTag(R.id.TAG_REPORT_ID, it)

            // Add The card to the scrollview
            val pos = reportListScrollContainer.getChildCount()
            Log.d("Arbeitsbericht", "Adding report card $pos to UI")
            reportListScrollContainer.addView(reportBinding.root, pos)
        }
    }

    fun createNewReport() {
        StorageHandler.createNewHoursReportAndSelect()
        val intent = Intent(this, HoursEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickNewReport(@Suppress("UNUSED_PARAMETER") newReportButton: View) {
        createNewReport()
    }

    fun onClickReport(reportCard: View) {
        val id = reportCard.getTag(R.id.TAG_REPORT_ID) as Long
        Log.d(TAG, "Clicked hours report with id ${id}")
        StorageHandler.selectHoursReportById(id, applicationContext)
        val intent = Intent(this, HoursEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickDelete(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@HoursMainActivity)
            if(answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d(TAG, "deleting a report")
                StorageHandler.deleteHoursReport(
                    btn.getTag(R.id.TAG_REPORT_ID) as Long,
                    applicationContext
                )
                val reportListScrollContainer = hours_report_list_scroll_container
                reportListScrollContainer.removeView(btn.getTag(R.id.TAG_CARDVIEW) as View)
            } else {
                Log.d(TAG, "Cancelled deleting a report")
            }
        }
    }
}
