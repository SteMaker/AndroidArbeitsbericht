package com.stemaker.arbeitsbericht

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Context
import android.view.*
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.databinding.ActivityMainBinding
import com.stemaker.arbeitsbericht.databinding.ReportCardLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    lateinit var topBinding: ActivityMainBinding
    private val mapState2MenuItemId = mapOf(ReportData.ReportState.IN_WORK to R.id.status_in_work,
        ReportData.ReportState.ON_HOLD to R.id.status_on_hold,
        ReportData.ReportState.DONE to R.id.status_done)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        topBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        topBinding.lifecycleOwner = this
        // Nothing to bind right now topBinding.... = ...!!

        setSupportActionBar(findViewById(R.id.main_activity_toolbar))
        supportActionBar?.setTitle(R.string.saved_reports)


        val reportListScrollContainer = report_list_scroll_container
        val reportIds = storageHandler().getListOfReports()
        reportIds.forEach {
            // Get the respective report
            val rep: ReportData = storageHandler().getReportById(it, getApplicationContext())
            Log.d("Arbeitsbericht", "Report with ID: ${rep.id} from ${rep.create_date}")
            // Prepare a report_card_layout instance
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val reportBinding: ReportCardLayoutBinding = DataBindingUtil.inflate(inflater, R.layout.report_card_layout, null, false)
            // Bind in the data
            reportBinding.reportData = rep

            // Assign the report ID as tag to the button to know which one to delete
            val btnReportMenu = reportBinding.root.findViewById<ImageButton>(R.id.report_card_menu_button)
            btnReportMenu.setTag(R.id.TAG_REPORT_ID, rep.id)
            btnReportMenu.setTag(R.id.TAG_CARDVIEW, reportBinding.root)
            reportBinding.root.setTag(R.id.TAG_REPORT_ID, it)

            // Add The card to the scrollview
            val pos = reportListScrollContainer.getChildCount()
            Log.d("Arbeitsbericht", "Adding report card $pos to UI")
            reportListScrollContainer.addView(reportBinding.root, pos)
        }
        if(configuration().appUpdateWasDone) {
            val versionDialog = VersionDialogFragment()
            versionDialog.show(supportFragmentManager, "VersionDialog")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_options, menu)
        return true
    }

    fun onClickContext(v: View) {
        val popup = PopupMenu(this, v).apply {
            val reportId = v.getTag(R.id.TAG_REPORT_ID) as String
            val cardV = v.getTag(R.id.TAG_CARDVIEW) as View
            val report = storageHandler().getReportById(reportId, applicationContext)
            storageHandler().selectReportById(reportId)
            setOnMenuItemClickListener(object: PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem?): Boolean {
                    return when (item?.itemId) {
                        R.id.delete-> {
                            deleteReport(reportId, cardV)
                            true
                        }
                        R.id.status_in_work -> {
                            storageHandler().getReport().state.value = ReportData.ReportState.IN_WORK
                            storageHandler().saveActiveReportToFile(applicationContext)
                            true
                        }
                        R.id.status_on_hold -> {
                            storageHandler().getReport().state.value = ReportData.ReportState.ON_HOLD
                            storageHandler().saveActiveReportToFile(applicationContext)
                            true
                        }
                        R.id.status_done -> {
                            Log.d("MainActivity", "Report ${report.id} set to done")
                            storageHandler().getReport().state.value = ReportData.ReportState.DONE
                            storageHandler().saveActiveReportToFile(applicationContext)
                            true
                        }
                        else -> false
                    }
                }
            })
            inflate(R.menu.report_actions_menu)
            menu.findItem(mapState2MenuItemId[report.state.value!!]!!).setVisible(false)
            show()
        }
    }


    fun createNewReport() {
        storageHandler().createNewReportAndSelect()
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        startActivity(intent)

    }

    fun onClickNewReport(@Suppress("UNUSED_PARAMETER") newReportButton: View) {
        createNewReport()
    }

    fun onClickReport(reportCard: View) {
        val id = reportCard.getTag(R.id.TAG_REPORT_ID) as String
        Log.d("Arbeitsbericht", "Clicked report with id ${id}")
        storageHandler().selectReportById(id, applicationContext)
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    fun deleteReport(id: String, cardV: View) {
        // TODO: We should delete the related photo files as well
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@MainActivity)
            if(answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d("Arbeitsbericht.ReportEditorActivity.onClickDelete", "deleting a report")
                storageHandler().deleteReport(id, applicationContext)
                val reportListScrollContainer = report_list_scroll_container
                reportListScrollContainer.removeView(cardV)
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
            R.id.main_menu_versions -> {
                val versionDialog = VersionDialogFragment()
                versionDialog.show(supportFragmentManager, "VersionDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}