package com.stemaker.arbeitsbericht

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.stemaker.arbeitsbericht.data.client.ClientRepository
import com.stemaker.arbeitsbericht.data.configuration.configuration
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.data.report.ReportRepository
import com.stemaker.arbeitsbericht.databinding.ActivityReportEditorBinding
import com.stemaker.arbeitsbericht.editor_fragments.*
import com.stemaker.arbeitsbericht.helpers.OrientationNotificationDialogFragment

private const val TAG = "ReportEditorActivity"

class ReportEditorFragmentFactory(private val clientRepository: ClientRepository,
                                  private val reportRepository: ReportRepository):
    FragmentFactory()
{
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when(className) {
            BillEditorFragment::class.java.name -> BillEditorFragment(clientRepository, reportRepository.activeReport)
            LumpSumEditorFragment::class.java.name ->  LumpSumEditorFragment(reportRepository.activeReport)
            MaterialEditorFragment::class.java.name -> MaterialEditorFragment(reportRepository.activeReport)
            PhotoEditorFragment::class.java.name -> PhotoEditorFragment(reportRepository.activeReport)
            ProjectBillEditorFragment::class.java.name -> ProjectBillEditorFragment(clientRepository, reportRepository.activeReport)
            ProjectEditorFragment::class.java.name -> ProjectEditorFragment(reportRepository.activeReport)
            WorkItemEditorFragment::class.java.name -> WorkItemEditorFragment(reportRepository.activeReport)
            WorkTimeEditorFragment::class.java.name -> WorkTimeEditorFragment(reportRepository.activeReport)
            else -> super.instantiate(classLoader, className)
        }
    }
}

class ReportEditorActivity : ArbeitsberichtActivity(),
    ReportEditorSectionFragment.OnReportEditorInteractionListener,
    OrientationNotificationDialogFragment.ForcePortraitListener
{

    lateinit var topBinding : ActivityReportEditorBinding
    override lateinit var report: ReportData

    override fun onCreate(savedInstanceState: Bundle?) {
        if(!onCreateWrapper(savedInstanceState))
            return

        // Here we expect that the app initialization is done
        supportFragmentManager.fragmentFactory = ReportEditorFragmentFactory(app.clientRepo, app.reportRepo)
        report = app.reportRepo.activeReport
        super.onCreate(savedInstanceState)

        topBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_editor)

        requestedOrientation = when(configuration().lockScreenOrientation) {
            true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }

        topBinding.reportEditorActivityToolbar.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.goto_summary -> {
                    app.reportRepo.saveReports()
                    val intent = Intent(this, SummaryActivity::class.java).apply {}
                    startActivity(intent)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        topBinding.reportEditorActivityToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        if(!configuration().lockScreenOrientationNoInfo && !configuration().lockScreenOrientation &&
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val screenOrientationDialog = OrientationNotificationDialogFragment()
            screenOrientationDialog.setForcePortraitListener(this)
            screenOrientationDialog.show(supportFragmentManager, "Orientation dialog")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    override fun forcePortrait() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        recreate()
    }

    override fun onStop() {
        super.onStop()
        app.reportRepo.saveReports()
    }
}
