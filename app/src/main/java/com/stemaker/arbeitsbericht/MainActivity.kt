package com.stemaker.arbeitsbericht

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Context
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.databinding.ActivityMainBinding
import com.stemaker.arbeitsbericht.databinding.FragmentProjectEditorBinding
import com.stemaker.arbeitsbericht.databinding.ReportCardLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/* TODO: We should have an ability to send the configuration, report and photo files to some (e.g. cloud) share
   TODO: In the ReportEditorActivity the toplevel containers should be CardViews to follow a more generic layout
   TODO: Add the ability to collapse the signature pads to have the whole screen for the actual html report
   TODO: Remove the buttons from the ReportEditorActivity and SummaryActivity and put them in the header
*/

class MainActivity : AppCompatActivity() {
    lateinit var topBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        topBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        topBinding.lifecycleOwner = this
        // Nothing to bind right now topBinding.... = ...!!

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
            val btnDel = reportBinding.root.findViewById<ImageButton>(R.id.report_card_del_button)
            btnDel.setTag(R.id.TAG_REPORT_ID, rep.id.value)
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
        storageHandler().createNewReportAndSelect(getApplicationContext())
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickNewReport(@Suppress("UNUSED_PARAMETER") newReportButton: View) {
        createNewReport()
    }

    fun onClickReport(reportCard: View) {
        val id: Int = reportCard.getTag(R.id.TAG_REPORT_ID) as Int
        Log.d("Arbeitsbericht", "Clicked report with id ${id}")
        storageHandler().selectReportById(id, getApplicationContext())
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickDelete(btn: View) {
        // TODO: We should delete the related photo files as well
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@MainActivity)
            if(answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d("Arbeitsbericht.ReportEditorActivity.onClickDelete", "deleting a report")
                storageHandler().deleteReport(btn.getTag(R.id.TAG_REPORT_ID) as Int, getApplicationContext())
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