package com.stemaker.arbeitsbericht

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
//import kotlinx.android.synthetic.main.activity_main.*
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

private const val TAG = "MainActivity"

interface ReportCardInterface {
    fun onClickReport(cnt: Int)
    fun onClickDeleteReport(report: ReportData)
    fun onSetReportState(report: ReportData, pos: Int, state: ReportData.ReportState)
    /* This is only for test purposes to create many reports. All are marked with MANY_REPORTS */
    /*fun onCopyReport(report: ReportData)*/
}
fun Boolean.toInt() = if (this) 1 else 0

class ReportStateVisibility {
    var visibility = mutableSetOf<Int>(
        ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK),
        ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD),
        ReportData.ReportState.toInt(ReportData.ReportState.DONE)
    )
    var inWork
        get() = visibility.contains(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
        set(v: Boolean) {
            if (v) visibility.add(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
            else visibility.remove(ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK))
        }
    var onHold
        get() = visibility.contains(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
        set(v: Boolean) {
            if (v) visibility.add(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
            else visibility.remove(ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD))
        }
    var done
        get() = visibility.contains(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
        set(v: Boolean) {
            if (v) visibility.add(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
            else visibility.remove(ReportData.ReportState.toInt(ReportData.ReportState.DONE))
        }
    var archived
        get() = visibility.contains(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
        set(v: Boolean) {
            if (v) visibility.add(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
            else visibility.remove(ReportData.ReportState.toInt(ReportData.ReportState.ARCHIVED))
        }
}

class MainActivity : AppCompatActivity(), ReportCardInterface {
    lateinit var binding: ActivityMainBinding
    var reportStateVisibility = ReportStateVisibility()
    lateinit var adapter: ReportListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "starting")

        val storageInitJob = storageHandler().initialize()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        setSupportActionBar(findViewById(R.id.main_activity_toolbar))
        supportActionBar?.setTitle(R.string.saved_reports)

        val layoutManager = LinearLayoutManager(this)
        adapter = ReportListAdapter(this, this)
        val recyclerView = binding.reportListRv
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        GlobalScope.launch(Dispatchers.Main) {
            binding.progressBar.visibility = View.VISIBLE
            binding.initNotify.visibility = View.VISIBLE
            storageInitJob?.join()
            binding.progressBar.visibility = View.GONE
            binding.initNotify.visibility = View.GONE
            adapter.registerReportListObserver()

            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            if (configuration().appUpdateWasDone) {
                val versionDialog = VersionDialogFragment()
                versionDialog.show(supportFragmentManager, "VersionDialog")
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_options, menu)
        return true
    }

    private fun createNewReport() {
        // On creating a new report, we should make in work reports visible
        reportStateVisibility.inWork = true
        storageHandler().setStateFilter(reportStateVisibility.visibility)
        storageHandler().createNewReportAndSelect()
        val intent = Intent(this@MainActivity, ReportEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickNewReport(v_: View) {
        createNewReport()
    }

    fun onClickJumpTop(v_: View) {
        adapter.jumpTop()
    }

    override fun onClickReport(cnt: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            storageHandler().selectReportByCnt(cnt)
            val intent = Intent(this@MainActivity, ReportEditorActivity::class.java).apply {}
            startActivity(intent)
        }
    }

    override fun onClickDeleteReport(report:ReportData) {
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@MainActivity)
            if (answer == AlertDialog.BUTTON_POSITIVE) {
                storageHandler().deleteReport(report.cnt)
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

    override fun onSetReportState(report: ReportData, pos: Int, state: ReportData.ReportState) {
        report.state.value = state
        GlobalScope.launch(Dispatchers.Main) {
            storageHandler().saveReport(report, true)
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
                        GlobalScope.launch(Dispatchers.Main) {
                            storageHandler().setStateFilter(reportStateVisibility.visibility)
                        }
                        true
                    }
                    R.id.on_hold -> {
                        reportStateVisibility.onHold = !item.isChecked
                        item.isChecked = !item.isChecked
                        GlobalScope.launch(Dispatchers.Main) {
                            storageHandler().setStateFilter(reportStateVisibility.visibility)
                        }
                        true
                    }
                    R.id.done -> {
                        reportStateVisibility.done = !item.isChecked
                        item.isChecked = !item.isChecked
                        GlobalScope.launch(Dispatchers.Main) {
                            storageHandler().setStateFilter(reportStateVisibility.visibility)
                        }
                        true
                    }
                    R.id.archived -> {
                        reportStateVisibility.archived = !item.isChecked
                        item.isChecked = !item.isChecked
                        GlobalScope.launch(Dispatchers.Main) {
                            storageHandler().setStateFilter(reportStateVisibility.visibility)
                        }
                        true
                    }
                    else -> false
                }
            }
            inflate(R.menu.report_state_filter_menu)
            menu.findItem(R.id.in_work).isChecked = reportStateVisibility.inWork
            menu.findItem(R.id.on_hold).isChecked = reportStateVisibility.onHold
            menu.findItem(R.id.done).isChecked = reportStateVisibility.done
            menu.findItem(R.id.archived).isChecked = reportStateVisibility.archived
            show()
        }
    }

    companion object {
        const val STATE_FILTER = "stateFilter"
    }
}