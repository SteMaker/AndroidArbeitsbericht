package com.stemaker.arbeitsbericht

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.room.Room
import com.stemaker.arbeitsbericht.data.ReportDatabase
import com.stemaker.arbeitsbericht.data.client.ClientRepository
import com.stemaker.arbeitsbericht.data.configuration.Configuration
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.data.report.ReportRepository
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
        // Activities need to wait for this initJob before they are allowed to access the preferences or one of the repositories
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
            prefs = AbPreferences(appContext)
            if(prefs.isNewlyCreated) {
                if(StorageHandler.loadConfigurationFromFile(applicationContext)) {
                    prefs.fromConfig(Configuration)
                    StorageHandler.deleteConfigurationFile(applicationContext)
                }
            }
            appUpdateWasDone = prefs.versionCode.value < getVersionCode()
            if(appUpdateWasDone) {
                prefs.versionCode.value = getVersionCode()
            }

            reportRepo = ReportRepository(db.reportDao(), prefs)
            clientRepo = ClientRepository(db.clientDao(), prefs)
            // Check if highest uid of reports is > currentId, then something went wrong
            val maxUid = reportRepo.initialize()
            if (maxUid >= prefs.currentId) {
                Log.e(TAG, "nextCnt  of configuration is less than max Report ID (cnt). Seems the configuration was lost.")
                prefs.overrideCurrentId(maxUid + 1)
            }

            // Now we should be ready to do more
            if (prefs.activeReportId.value != -1) {
                if (reportRepo.isReportVisible(prefs.activeReportId.value)) {
                    reportRepo.setActiveReport(prefs.activeReportId.value)
                } else {
                    prefs.activeReportId.value = -1
                }
            }
            clientRepo.initialize()
        }
    }
    companion object {
        lateinit var appContext: Context

        fun getVersionCode(): Long {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    appContext.packageManager.getPackageInfo(appContext.packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode
                else ->
                    appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionCode.toLong()
            }
        }
        fun getInWorkIconDrawable() = R.drawable.ic_baseline_handyman_24
        fun getOnHoldIconDrawable() = R.drawable.ic_baseline_pause_24
        fun getDoneIconDrawable() = R.drawable.ic_baseline_done_24
        fun getArchivedIconDrawable() = R.drawable.ic_baseline_archive_24
    }
}