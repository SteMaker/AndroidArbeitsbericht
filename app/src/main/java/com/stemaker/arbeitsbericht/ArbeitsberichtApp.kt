package com.stemaker.arbeitsbericht

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.stemaker.arbeitsbericht.data.ReportDatabase
import com.stemaker.arbeitsbericht.data.configuration.configuration
import kotlinx.coroutines.*

private const val TAG = "ArbeitsberichtApp"
/* In the application we create the database component and spawn an initialization
 * task to execute application wide initialization - reading the configuration,
 *
 */
class ArbeitsberichtApp: Application() {

    val db = Room.databaseBuilder(
        applicationContext,
        ReportDatabase::class.java, "Arbeitsbericht-Reports"
    ).fallbackToDestructiveMigration().addMigrations(ReportDatabase.migr_2_3).build()

    lateinit var initJob: Job

    override fun onCreate() {
        super.onCreate()
        // Required for ApachePOI to work
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")
        appContext = applicationContext
        // Activities need to wait for this initJob before they are allowed to access the storageHandler()
        initJob = initialize()
    }

    private fun initialize(): Job {
        return GlobalScope.launch(Dispatchers.Main) {
            Log.d(TAG, "initializing storage")
            // Read the configuration. This needs to be low level -> no configuration() invocation yet
            StorageHandler.loadConfigurationFromFile(applicationContext)

            val cnts = withContext(Dispatchers.IO) {
                db.reportDao().getFilteredReportIds(configuration().reportFilter)
            }
            StorageHandler.visReportCnts.addAll(cnts)

            StorageHandler.checkAndRepairCurrentIdConsistency(activityContext)

            // Now we should be ready to do more
            if (configuration().activeReportId != -1) {
                if (StorageHandler.visReportCnts.contains(configuration().activeReportId)) {
                    StorageHandler.selectReportByCnt(configuration().activeReportId)
                } else {
                    configuration().activeReportId = -1
                }
            }
            configuration().reportFilter.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    StorageHandler.updateFilter()
                }
            })
        }
    }
    companion object {
        lateinit var appContext: Context
        fun getVersionCode(): Int {
            return appContext.packageManager.getPackageInfo(appContext.packageName!!, 0).versionCode
        }
        fun getInWorkIconDrawable() = R.drawable.ic_baseline_handyman_24
        fun getOnHoldIconDrawable() = R.drawable.ic_baseline_pause_24
        fun getDoneIconDrawable() = R.drawable.ic_baseline_done_24
        fun getArchivedIconDrawable() = R.drawable.ic_baseline_archive_24
    }
}