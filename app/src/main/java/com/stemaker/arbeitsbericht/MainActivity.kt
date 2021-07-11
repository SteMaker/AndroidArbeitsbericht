package com.stemaker.arbeitsbericht

//import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.data.configuration
import com.stemaker.arbeitsbericht.databinding.ActivityMainBinding
import com.stemaker.arbeitsbericht.helpers.ReportFilterDialog
import com.stemaker.arbeitsbericht.helpers.ReportListAdapter
import com.stemaker.arbeitsbericht.helpers.VersionDialogFragment
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

interface ReportCardInterface {
    fun onClickReport(cnt: Int)
    fun onClickDeleteReport(report: ReportData)
    fun onSetReportState(report: ReportData, pos: Int, state: ReportData.ReportState)
    /* This is only for test purposes to create many reports. All are marked with MANY_REPORTS */
    /*fun onCopyReport(report: ReportData)*/
}
fun Boolean.toInt() = if (this) 1 else 0


class MainActivity : AppCompatActivity(), ReportCardInterface {
    lateinit var binding: ActivityMainBinding
    lateinit var adapter: ReportListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "starting")

        val storageInitJob = storageHandler().initialize()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

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
        binding.mainActivityToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
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
                    showFilterDialog()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
    }

    private fun createNewReport() {
        // On creating a new report, we should make in work reports visible
        configuration().reportFilter.inWork = true
        configuration().reportFilter.update()
        //storageHandler().updateFilter(reportStateVisibility.visibility)
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

    private fun showFilterDialog() {
        val dialog = ReportFilterDialog()
        dialog.show(supportFragmentManager, "Filter")
    }

    companion object {
        const val STATE_FILTER = "stateFilter"
    }
}