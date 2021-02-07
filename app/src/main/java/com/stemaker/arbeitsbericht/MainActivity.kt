package com.stemaker.arbeitsbericht

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import android.view.*
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.databinding.ActivityMainBinding
import com.stemaker.arbeitsbericht.helpers.ReportListAdapter
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface ReportCardInterface {
    fun onClickReport(id: String)
    fun onClickDeleteReport(report: ReportData, onDeleted:()->Unit)
    fun onSetReportState(report: ReportData, state: ReportData.ReportState)
    /* This is only for test purposes to create many reports. All are marked with MANY_REPORTS */
    /*fun onCopyReport(report: ReportData)*/
}
fun Boolean.toInt() = if (this) 1 else 0

class ReportStateVisibility {
    var visibility: Int = defaultPattern
    var inWork
        get() = getter(0)
        set(v: Boolean) = setter(0, v)
    var onHold
        get() = getter(1)
        set(v: Boolean) = setter(1, v)
    var done
        get() = getter(2)
        set(v: Boolean)  = setter(2, v)
    var archived
        get() = getter(3)
        set(v: Boolean)  = setter(3, v)
    private fun setter(bitpos: Int, v: Boolean) {
        if(v)
            visibility = visibility or (1 shl bitpos)
        else
            visibility = visibility and pattern and (1 shl bitpos).inv()
    }
    private fun getter(bitpos: Int): Boolean {
        return (visibility and (1 shl bitpos)) != 0
    }

    fun isReportVisible(report: ReportData): Boolean = ((1 shl ReportData.ReportState.toInt(report.state.value!!)) and visibility) != 0

    companion object {
        const val pattern = 0xF
        const val defaultPattern = 0x7
    }
}

class MainActivity : AppCompatActivity(), ReportCardInterface {
    lateinit var binding: ActivityMainBinding
    var reportStateVisibility = ReportStateVisibility()
    lateinit var adapter: ReportListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            with(savedInstanceState) {
                reportStateVisibility.visibility = getInt(STATE_FILTER)
            }
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        setSupportActionBar(findViewById(R.id.main_activity_toolbar))
        supportActionBar?.setTitle(R.string.saved_reports)

        val layoutManager = LinearLayoutManager(this)
        adapter = ReportListAdapter(this, this)
        val reports = getNextReports(30)
        adapter.add(reports)
        val recyclerView = binding.reportListRv
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener( object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    val reports = getNextReports(30)
                    adapter.add(reports)
                }
            }
        })

        if(configuration().appUpdateWasDone) {
            val versionDialog = VersionDialogFragment()
            versionDialog.show(supportFragmentManager, "VersionDialog")
        }
    }

    var deliveredReports = 0
    private fun getNextReports(amount: Int): List<ReportData> {
        val reportRefs = storageHandler().getListOfReports()
        val reports = mutableListOf<ReportData>()
        var idx = deliveredReports
        var cnt = 0
        while (cnt < amount && idx < reportRefs.size) {
            val report = storageHandler().getReportByRef(reportRefs.get(idx), this@MainActivity)
            if(reportStateVisibility.isReportVisible(report)) {
                reports.add(report)
                cnt++
            }
            idx++
        }
        deliveredReports += cnt
        return reports
    }

    private fun resetDeliveredReports() {
        deliveredReports = 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("MainActivity", "onSaveInstanceState")
        super.onSaveInstanceState(outState)
        with(outState) {
            putInt(STATE_FILTER, reportStateVisibility.visibility)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Log.d("MainActivity", "onRestoreInstanceState")
        super.onRestoreInstanceState(savedInstanceState)
        with(savedInstanceState) {
            reportStateVisibility.visibility = getInt(STATE_FILTER)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_options, menu)
        return true
    }

    private fun createNewReport() {
        storageHandler().createNewReportAndSelect()
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        startActivity(intent)

    }

    fun onClickNewReport(@Suppress("UNUSED_PARAMETER") newReportButton: View) {
        createNewReport()
    }

    override fun onClickReport(id: String) {
        storageHandler().selectReportById(id, applicationContext)
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    override fun onClickDeleteReport(report:ReportData, onDeleted:()->Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@MainActivity)
            if (answer == AlertDialog.BUTTON_POSITIVE) {
                storageHandler().deleteReport(report, this@MainActivity)
                onDeleted()
            }
        }
    }

    /* This is only for test purposes to create many reports. All are marked with MANY_REPORTS */
    /*
    override fun onCopyReport(report: ReportData) {
        for (i in 0..100) {
            storageHandler().duplicateReport(report)
            adapter.add(report)
        }
    }*/

    override fun onSetReportState(report: ReportData, state: ReportData.ReportState) {
        report.state.value = state
        storageHandler().saveReportToFile(this@MainActivity, report, true)
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
            R.id.main_menu_filter -> {
                showFilterPopup()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterPopup() {
        val view = binding.mainActivityToolbar.findViewById<View>(R.id.main_menu_filter)
        PopupMenu(this@MainActivity, view).apply {
            setOnMenuItemClickListener { item ->
                when (item?.itemId) {
                    R.id.in_work -> {
                        reportStateVisibility.inWork = !item.isChecked
                        item.isChecked = !item.isChecked
                        resetDeliveredReports()
                        adapter.replaceAll(getNextReports(30))
                        true
                    }
                    R.id.on_hold -> {
                        reportStateVisibility.onHold = !item.isChecked
                        item.isChecked = !item.isChecked
                        resetDeliveredReports()
                        adapter.replaceAll(getNextReports(30))
                        true
                    }
                    R.id.done -> {
                        reportStateVisibility.done = !item.isChecked
                        item.isChecked = !item.isChecked
                        resetDeliveredReports()
                        adapter.replaceAll(getNextReports(30))
                        true
                    }
                    R.id.archived -> {
                        reportStateVisibility.archived = !item.isChecked
                        item.isChecked = !item.isChecked
                        resetDeliveredReports()
                        adapter.replaceAll(getNextReports(30))
                        true
                    }
                    else -> false
                }
            }
            inflate(R.menu.report_state_filter_menu)
            menu.findItem(R.id.in_work).setChecked(reportStateVisibility.inWork)
            menu.findItem(R.id.on_hold).setChecked(reportStateVisibility.onHold)
            menu.findItem(R.id.done).setChecked(reportStateVisibility.done)
            menu.findItem(R.id.archived).setChecked(reportStateVisibility.archived)
            show()
        }
    }

    companion object {
        const val STATE_FILTER = "stateFilter"
    }
}