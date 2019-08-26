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
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import com.google.android.material.card.MaterialCardView
import java.text.DateFormat
import java.util.*
import java.util.Calendar.*

const val TAG_ID: Int = 1
const val TAG_CARDVIEW: Int = 2

class MainActivity : AppCompatActivity(), ConfirmationDialogFragment.ConfirmationDialogListener {

    var delBtnPressed: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        StorageHandler.myInit(getApplicationContext())
        val reportListScrollContainer = report_list_scroll_container
        val reportIds = StorageHandler.getListOfReports()
        reportIds.forEach {
            // Get the active report
            val rep: Report = StorageHandler.getReportById(it, getApplicationContext())
            Log.d("Arbeitsbericht", "Report with ID: ${rep.id} from ${rep.create_date}")
            // Prepare a report_card_layout instance
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val cV = inflater.inflate(R.layout.report_card_layout, null) as CardView
            // Fill in the data
            cV.findViewById<TextView>(R.id.report_card_client).setText(rep.client_name.toString())
            cV.findViewById<TextView>(R.id.report_card_create_date).setText("Erstellt: " + rep.create_date)
            cV.findViewById<TextView>(R.id.report_card_change_date).setText("Letzte Änderung: " + rep.change_date)
            cV.findViewById<TextView>(R.id.report_card_id).setText(rep.id.toString())

            // Assign the report ID as tag to the button to know which one to delete
            val btnDel = cV.findViewById<ImageButton>(R.id.report_card_del_button)
            btnDel.setTag(R.id.TAG_REPORT_ID, rep.id)
            btnDel.setTag(R.id.TAG_CARDVIEW, cV)

            // Add The card to the scrollview
            val pos = reportListScrollContainer.getChildCount()
            Log.d("Arbeitsbericht", "Adding report card $pos to UI")
            reportListScrollContainer.addView(cV, pos)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_options, menu)
        return true
    }

    fun createNewReport() {
        StorageHandler.createNewReportAndSelect(getApplicationContext())
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickNewReport(@Suppress("UNUSED_PARAMETER") newReportButton: View) {
        createNewReport()
    }

    fun onClickReport(reportCard: View) {
        val id: String = reportCard.findViewById<TextView>(R.id.report_card_id).getText() as String
        Log.d("Arbeitsbericht", "Clicked report with id ${id}")
        StorageHandler.selectReportById(id.toInt(), getApplicationContext())
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickDelete(btn: View) {
        delBtnPressed = btn
        val newFragment = newConfirmationDialog(getString(R.string.del_confirmation))
        newFragment.show(getSupportFragmentManager(), "dialog")
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        Log.d("Arbeitsbericht.ReportEditorActivity.onDialogPositiveClick", "deleting")
        StorageHandler.deleteReport(delBtnPressed!!.getTag(R.id.TAG_REPORT_ID) as Int, getApplicationContext())
        val reportListScrollContainer = report_list_scroll_container
        reportListScrollContainer.removeView(delBtnPressed!!.getTag(R.id.TAG_CARDVIEW) as View)
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        Log.d("Arbeitsbericht.ReportEditorActivity.onDialogPositiveClick", "aborting")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.main_menu_settings -> {
                val intent = Intent(this, ConfigurationActivity::class.java).apply {}
                startActivity(intent)
                true
            }
            R.id.main_menu_new_report -> {
                createNewReport()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}