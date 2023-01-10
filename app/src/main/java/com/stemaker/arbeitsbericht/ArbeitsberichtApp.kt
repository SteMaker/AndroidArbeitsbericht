package com.stemaker.arbeitsbericht

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap.Config
import android.os.Build
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import com.stemaker.arbeitsbericht.data.ReportDatabase
import com.stemaker.arbeitsbericht.data.client.ClientRepository
import com.stemaker.arbeitsbericht.data.configuration.Configuration
import com.stemaker.arbeitsbericht.data.configuration.configuration
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.data.report.ReportRepository
import com.stemaker.arbeitsbericht.helpers.ReportFilter
import kotlinx.coroutines.*

private const val TAG = "ArbeitsberichtApp"
/* In the application we create the database component and spawn an initialization
 * task to execute application wide initialization - reading the configuration,
 *
 */
class ArbeitsberichtApp: Application() {

    private lateinit var db: ReportDatabase
    var initJob: Job? = null
    lateinit var reportRepo: ReportRepository
    lateinit var reportFilter: ReportFilter
    lateinit var clientRepo: ClientRepository
    lateinit var prefs: AbPreferences

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            applicationContext,
            ReportDatabase::class.java, "Arbeitsbericht-Reports"
        ).fallbackToDestructiveMigration().addMigrations(ReportDatabase.migr_2_3).build()
        // Required for ApachePOI to work
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")
        appContext = applicationContext
        // Activities need to wait for this initJob before they are allowed to access the storageHandler() or the reportRepository
        initJob = initialize()
    }

    suspend fun waitForShutdown(): Boolean {
        reportRepo.saveReports()
        var cnt = 30 // Wait max 3 seconds
        while(reportRepo.isJobRunning && cnt!=0) {
            delay(100)
            cnt--
        }
        return reportRepo.isJobRunning
    }

    var appUpdateWasDone = false
        private set

    private fun initialize(): Job {
        return GlobalScope.launch(Dispatchers.Main) {
            Log.d(TAG, "initializing storage")
            // Read the configuration. This needs to be low level -> no configuration() invocation yet
            if(StorageHandler.loadConfigurationFromFile(applicationContext)) {
                // Handle switch to using preferences for storing configuration
                if (configuration().previousVersionCode <= 131) {
                    prefs = AbPreferences(appContext)
                    prefs.fromConfig(configuration())
                } else {
                    prefs = AbPreferences(appContext)
                }
            }
            appUpdateWasDone = prefs.previousVersionCode < prefs.versionCode.value!!

            reportRepo = ReportRepository(db.reportDao())
            clientRepo = ClientRepository(db.clientDao())
            // Check if highest uid of reports is > currentId, then something went wrong
            val maxUid = reportRepo.getMaxUid()
            if (maxUid >= configuration().currentId) {
                Log.e(TAG, "nextCnt  of configuration is less than max Report ID (cnt). Seems the configuration was lost.")
                configuration().currentId = maxUid + 1
                configuration().save()
            }

            // apply the filter
            reportFilter = ReportFilter()
            reportFilter.fromStore(configuration().filterProjectName, configuration().filterProjectExtra, configuration().filterStates)
            // At this point the reportRepo will be filled!!
            reportRepo.filter = reportFilter

            // Now we should be ready to do more
            if (configuration().activeReportId != -1) {
                if (reportRepo.isReportVisible(configuration().activeReportId)) {
                    reportRepo.setActiveReport(configuration().activeReportId)
                } else {
                    configuration().activeReportId = -1
                }
            }
            clientRepo.initialize()
        }
    }
    companion object {
        lateinit var appContext: Context

        fun getVersionCode(): Long {
            val info = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    appContext.packageManager.getPackageInfo(appContext.packageName, PackageManager.PackageInfoFlags.of(0))
                else ->
                    appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }
            return info.longVersionCode
        }
        fun getInWorkIconDrawable() = R.drawable.ic_baseline_handyman_24
        fun getOnHoldIconDrawable() = R.drawable.ic_baseline_pause_24
        fun getDoneIconDrawable() = R.drawable.ic_baseline_done_24
        fun getArchivedIconDrawable() = R.drawable.ic_baseline_archive_24
    }
}