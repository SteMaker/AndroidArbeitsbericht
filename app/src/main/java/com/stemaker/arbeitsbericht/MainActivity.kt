package com.stemaker.arbeitsbericht

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.databinding.ActivityMainBinding
import com.stemaker.arbeitsbericht.helpers.ReportFilterDialog
import com.stemaker.arbeitsbericht.helpers.ReportListAdapter
import com.stemaker.arbeitsbericht.helpers.VersionDialogFragment
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

private const val TAG = "MainActivity"

interface ReportCardInterface {
    fun onClickReport(uid: Int)
    fun onClickDeleteReport(report: ReportData)
    fun onSetReportState(report: ReportData, pos: Int, state: ReportData.ReportState)
    fun onClickDuplicateReport(report:ReportData)
    /* This is only for test purposes to create many reports. All are marked with MANY_REPORTS */
    /*fun onCopyReport(report: ReportData)*/
}
fun Boolean.toInt() = if (this) 1 else 0


class MainActivity:
    ArbeitsberichtActivity(),
    ReportCardInterface
{
    lateinit var binding: ActivityMainBinding
    lateinit var adapter: ReportListAdapter
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        if(!onCreateWrapper(savedInstanceState))
            return

        // Here we expect that the app initialization is done
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        val layoutManager = LinearLayoutManager(this)
        adapter = ReportListAdapter(this, app.reportRepo, this)
        val recyclerView = binding.reportListRv
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        requestedOrientation = when(prefs.lockScreenOrientation.value) {
            true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }

        if (app.appUpdateWasDone) {
            val versionDialog = VersionDialogFragment()
            try {
                versionDialog.show(supportFragmentManager, "VersionDialog")
            } catch (e: IllegalStateException) {}
        }
        binding.mainActivityToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.main_menu_settings -> {
                    val intent = Intent(this, ConfigurationActivity::class.java).apply {}
                    startActivity(intent)
                    true
                }
                R.id.main_menu_clients -> {
                    val intent = Intent(this, ClientListActivity::class.java).apply {}
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
        app.reportRepo.filter.inWork = true
        app.reportRepo.createReportAndActivate() {
            val intent = Intent(this@MainActivity, ReportEditorActivity::class.java).apply {}
            startActivity(intent)
        }
    }

    fun onClickNewReport(v_: View) {
        createNewReport()
    }

    fun onClickJumpTop(v_: View) {
        adapter.jumpTop()
    }

    override fun onClickReport(uid: Int) {
        app.reportRepo.setActiveReport(uid)
        val intent = Intent(this@MainActivity, ReportEditorActivity::class.java).apply {}
        startActivity(intent)
    }

    override fun onClickDeleteReport(report:ReportData) {
        scope.launch {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@MainActivity)
            if (answer == AlertDialog.BUTTON_POSITIVE) {
                app.reportRepo.deleteReport(report)
            }
        }
    }

    override fun onClickDuplicateReport(report:ReportData) {
        // On creating a new report, we should make in work reports visible
        app.reportRepo.filter.inWork = true
        app.reportRepo.duplicateReportAndActivate(report) {
            val intent = Intent(this@MainActivity, ReportEditorActivity::class.java).apply {}
            startActivity(intent)
        }
    }

    override fun onSetReportState(report: ReportData, pos: Int, state: ReportData.ReportState) {
        report.state.value = state
        // Not clear why I'd have to save here
        //GlobalScope.launch(Dispatchers.Main) {
        //    storageHandler().saveReport(report, true)
        //}
    }

    private fun showFilterDialog() {
        val dialog = ReportFilterDialog(app.reportRepo.filter /* falsch, sollte prefs sein */)
        dialog.show(supportFragmentManager, "Filter")
    }
}