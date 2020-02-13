package com.stemaker.arbeitsbericht.workreport

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Context
import android.view.*
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.AboutDialogFragment
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.StorageHandler
import com.stemaker.arbeitsbericht.configuration.ConfigurationActivity
import com.stemaker.arbeitsbericht.configuration.LumpSumDefinitionActivity
import com.stemaker.arbeitsbericht.data.workreport.ReportData
import com.stemaker.arbeitsbericht.databinding.ActivityMainBinding
import com.stemaker.arbeitsbericht.databinding.ReportCardLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import com.stemaker.arbeitsbericht.hoursreport.HoursMainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/* TODO: We should have an ability to send the configuration, report and photo files to some (e.g. cloud) share
   TODO: Add the ability to collapse the signature pads to have the whole screen for the actual html report
*/

private const val TAG = "WorkReportMainActivity"

class WorkReportMainActivity : AppCompatActivity() {
    lateinit var topBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        topBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        topBinding.lifecycleOwner = this
        // Nothing to bind right now topBinding.... = ...!!

        val toolbar = findViewById<Toolbar>(R.id.main_activity_toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_apps_black_24dp)
        toolbar.setNavigationOnClickListener {
            val popup = PopupMenu(this@WorkReportMainActivity, toolbar)
            popup.menuInflater.inflate(R.menu.tasks_menu, popup.getMenu())
            popup.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.app_stundenbericht -> {
                        val intent = Intent(this, HoursMainActivity::class.java).apply {}
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        supportActionBar?.setTitle(R.string.saved_reports)
        supportActionBar?.setHomeButtonEnabled(true)

        val reportListScrollContainer = report_list_scroll_container
        val reportIds = StorageHandler.getListOfReports()
        reportIds.forEach {
            // Get the respective report
            val rep: ReportData = StorageHandler.getReportById(it, getApplicationContext())
            Log.d("Arbeitsbericht", "Report with ID: ${rep.id} from ${rep.create_date}")
            // Prepare a report_card_layout instance
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val reportBinding: ReportCardLayoutBinding = DataBindingUtil.inflate(inflater,
                R.layout.report_card_layout, null, false)
            // Bind in the data
            reportBinding.reportData = rep

            // Assign the report ID as tag to the button to know which one to delete
            val btnDel = reportBinding.root.findViewById<ImageButton>(R.id.report_card_del_button)
            btnDel.setTag(R.id.TAG_REPORT_ID, rep.id)
            btnDel.setTag(R.id.TAG_CARDVIEW, reportBinding.root)
            reportBinding.root.setTag(R.id.TAG_REPORT_ID, it)

            // Add The card to the scrollview
            val pos = reportListScrollContainer.getChildCount()
            Log.d("Arbeitsbericht", "Adding report card $pos to UI")
            reportListScrollContainer.addView(reportBinding.root, pos)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_options, menu)
        return true
    }

    fun createNewReport() {
        StorageHandler.createNewReportAndSelect()
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickNewReport(@Suppress("UNUSED_PARAMETER") newReportButton: View) {
        createNewReport()
    }

    fun onClickReport(reportCard: View) {
        val id = reportCard.getTag(R.id.TAG_REPORT_ID) as String
        Log.d("Arbeitsbericht", "Clicked report with id ${id}")
        StorageHandler.selectReportById(id, applicationContext)
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickDelete(btn: View) {
        // TODO: We should delete the related photo files as well
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@WorkReportMainActivity)
            if(answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d("Arbeitsbericht.ReportEditorActivity.onClickDelete", "deleting a report")
                StorageHandler.deleteReport(
                    btn.getTag(R.id.TAG_REPORT_ID) as String,
                    applicationContext
                )
                val reportListScrollContainer = report_list_scroll_container
                reportListScrollContainer.removeView(btn.getTag(R.id.TAG_CARDVIEW) as View)
            } else {
                Log.d("Arbeitsbericht.MainActivity.onClickDelete", "Cancelled deleting a report")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.main_menu_settings -> {
                val intent = Intent(this, ConfigurationActivity::class.java).apply {}
                startActivity(intent)
                true
            }
            R.id.main_menu_lump_sums -> {
                val intent = Intent(this, LumpSumDefinitionActivity::class.java).apply {}
                startActivity(intent)
                true
            }
            R.id.main_menu_new_report -> {
                createNewReport()
                true
            }
            R.id.main_menu_about -> {
                val aboutDialog = AboutDialogFragment()
                aboutDialog.show(supportFragmentManager, "AboutDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}