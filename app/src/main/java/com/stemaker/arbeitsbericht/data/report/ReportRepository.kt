package com.stemaker.arbeitsbericht.data.report

import android.util.Log
import android.util.LruCache
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.helpers.ReportFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicInteger

const val CACHE_SIZE = 10
private const val TAG="ReportRepository"

class ReportRepository(
    private val reportDao: ReportDao,
    private val prefs: AbPreferences)
{
    class ReportListChangeEvent(val type: Type, val reportUid: Int = 0, val pos: Int = 0) {
        enum class Type {
            LIST_ADD, LIST_REMOVE, LIST_CHANGE
        }
    }

    // The reports cache
    private val reports = object : LruCache<Int, ReportData>(CACHE_SIZE) {
        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: ReportData?, newValue: ReportData?) {
            Log.d(TAG, "Report ${oldValue?.cnt} evicted")
            if (oldValue?.modified != false) {
                scope.launch {
                    oldValue?.let {
                        saveReport(it)
                    } ?: also {
                        Log.e(TAG, "Error: report evicted in cache, but no oldValue provided")
                        key?.let {
                            saveReport(getReportByUid(key))
                        } ?: also {
                            Log.e(TAG, "Error: And also no key is provided")
                        }
                    }
                }
                super.entryRemoved(evicted, key, oldValue, newValue)
            }
        }
    }

    /* reportUids always needs to be up-to-date with the list of report uids that are not filtered out. We need to update it when adding and deleting reports
     * as well as when the filter changes. A user can register a listener to live and will then get informed about the report that was added /
     * removed (the value of liveAdd / liveRemove) or the list being changed.
     * Any modification of reportUids should be handled in scope+Main context, need to double check that this enforces serialization of the jobs. Reading shall be done
     * in UI thread (Dispatchers.Main)
     */
    private val reportUids = mutableListOf<Int>()
    private val reportUidsMutex = Mutex()

    val scope = CoroutineScope(Dispatchers.Main)

    private val _live = MutableLiveData<ReportListChangeEvent>()
    val live
        get() = _live

    // We setup a filter that is based directly on the preferences and will automatically adapt when the
    // filter settings in the preferences are changed. Whenever the filter changes we read all the reportUids anew
    val filter = ReportFilter(prefs.filterProjectName, prefs.filterProjectExtra, prefs.filterStates)
    init {
        filter.live.observeForever(){
            getReportUidsFromDb()}
    }

    private val runningJobs = AtomicInteger(0)
    private fun launchAndMaintain(fct: suspend () -> Unit) {
        scope.launch {
            runningJobs.addAndGet(1)
            fct()
            runningJobs.decrementAndGet()
        }
    }
    val isJobRunning = runningJobs.get()!=0

    suspend fun initialize(): Int {
        var ret = 0
        runBlocking {
            scope.launch {
                ret = withContext(Dispatchers.IO) {
                    val usedIds = reportDao.getReportUids()
                    reportUidsMutex.lock() // most likely not needed here, but doesn't harm
                    val uids = withContext(Dispatchers.IO) {
                        reportDao.getFilteredReportIds(filter)
                    }
                    reportUids.addAll(uids)
                    reportUidsMutex.unlock()
                    usedIds.maxOrNull() ?: 0 // If file didn't exist yet, initial value will be 1
                }
            }
        }
        return ret
    }

    ////////////////////////////////////////////////////
    // Internal report list handling
    ////////////////////////////////////////////////////
    private fun getReportUidsFromDb() {
        Log.d(TAG, "getReportUidsFromDb")
        scope.launch {
            Log.d(TAG, "getReportUidsFromDb2")
            reportUidsMutex.lock()
            val uids = withContext(Dispatchers.IO) {
                reportDao.getFilteredReportIds(filter)
            }
            reportUids.clear()
            reportUids.addAll(uids)
            reportUidsMutex.unlock()
            _live.value = ReportListChangeEvent(ReportListChangeEvent.Type.LIST_CHANGE)// Trigger event that list has been modified (not only single add / remove)
        }
    }
    private fun _deleteReport(r: ReportData, onDeleted:((r: ReportData)->Unit)? = null) {
        launchAndMaintain {
            reportUidsMutex.lock()
            reports.remove(r.cnt) // Remove from cache
            val index = reportUids.indexOf(r.cnt)
            reportUids.remove(r.cnt) // Remove from report cnt list
            reportUidsMutex.unlock()
            withContext(Dispatchers.IO) {
                reportDao.deleteByCnt(r.cnt) // Remove from DB
            }
            _live.value = ReportListChangeEvent(ReportListChangeEvent.Type.LIST_REMOVE, r.cnt, index) // Inform about removal
            onDeleted?.let { it(r) }
        }
    }
    private fun _addReport(r: ReportData, onAdded:((r: ReportData)->Unit)? = null) {
        launchAndMaintain {
            reportUidsMutex.lock()
            reports.put(r.cnt, r) // Add to cache
            reportUids.add(0, r.cnt) // Add to report cnt list
            reportUidsMutex.unlock()
            withContext(Dispatchers.IO) {
                reportDao.insert(ReportDb.fromReport(r)) // Add to DB
            }
            _live.value = ReportListChangeEvent(ReportListChangeEvent.Type.LIST_ADD, r.cnt, reportUids.indexOf(r.cnt))// Inform about addition
            onAdded?.let { it(r) }
        }
    }

    ////////////////////////////////////////////////////
    // Public API
    ////////////////////////////////////////////////////
    val amountOfReports: Int
        get() = reportUids.size

    lateinit var activeReport: ReportData
        private set

    fun isReportVisible(uid: Int): Boolean {
        return reportUids.contains(uid)
    }

    fun getReportByUid(cnt: Int, onLoaded:((r: ReportData)->Unit)? = null): ReportData {
        // If we have it in the cache, take if from there, else query the database and push it in the cache
        return reports.get(cnt)?.let { r ->
            onLoaded?.let { it(r) }
            r
        } ?: run {
            // If we don't have it in the cache then create one and let it populate from the database
            val report = ReportData.createReport(cnt, prefs)
            reports.put(cnt, report)
            scope.launch {
                val rDb = withContext(Dispatchers.IO) {
                    reportDao.getReportByCnt(cnt)
                }
                report.getReportFromDb(rDb)
                Log.d(TAG, "Loaded report ${report.cnt} with ${report.id.value} from database")
                onLoaded?.let { it(report) }
            }
            return report
        }
    }

    fun getReportByIndex(idx: Int, onLoaded:(r: ReportData)->Unit): ReportData {
        return getReportByUid(reportUids[idx], onLoaded)
    }

    fun setActiveReport(cnt: Int, onActivated:((r: ReportData)->Unit)? = null) {
        activeReport = getReportByUid(cnt, onActivated)
    }

    private fun createReport(onCreated:((r: ReportData)->Unit)?): ReportData {
        val r = ReportData.createReport(prefs.allocateReportId(), prefs)
        _addReport(r, onCreated)
        return r
    }

    fun createReportAndActivate(onCreated:((r: ReportData)->Unit)?): ReportData {
        val r = createReport(onCreated)
        prefs.activeReportId.value = r.cnt
        activeReport = r
        return r
    }

    fun deleteReport(r: ReportData) {
        _deleteReport(r)
    }

    fun duplicateReport(origin: ReportData, onCreated:((r: ReportData)->Unit)?): ReportData {
        val r = ReportData.createReport(prefs.allocateReportId(), prefs)
        r.copy(origin, copyDate = false)
        _addReport(r, onCreated)
        return r
    }

    fun duplicateReportAndActivate(origin: ReportData, onCreated:((r: ReportData)->Unit)?): ReportData {
        val r = duplicateReport(origin, onCreated)
        prefs.activeReportId.value = r.cnt
        activeReport = r
        return r
    }

    private suspend fun saveReportFromScope(r: ReportData) {
        withContext(Dispatchers.IO) {
                reportDao.update(ReportDb.fromReport(r))
        }
        Log.d(TAG, "Saved report ${r.cnt} to database")
        r.modified = false
    }

    fun saveReport(r: ReportData) {
        if(!r.modified) {
            Log.w(TAG, "Warning: Saving a report which is not marked as modified")
        }
        launchAndMaintain {
            saveReportFromScope(r)
        }
    }

    fun saveReports() {
        val cacheSnapshot = reports.snapshot()
        launchAndMaintain {
            cacheSnapshot.forEach { report ->
                if(report.value.modified) {
                    saveReportFromScope(report.value)
                }
            }
        }
    }
}