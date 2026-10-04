package com.stemaker.arbeitsbericht.data.report

import android.util.Log
import android.util.LruCache
import androidx.lifecycle.MutableLiveData
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.helpers.ReportFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

const val CACHE_SIZE = 50
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
            if(evicted) {
                Log.d(TAG, "Report ${oldValue?.cnt} with project ${oldValue?.project?.name?.value} evicted")
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
                }
            }
            super.entryRemoved(evicted, key, oldValue, newValue)
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
    /* This has to be a computed property. As a plain val it was evaluated once during
     * construction, when no job can possibly be running, so it was permanently false and
     * ArbeitsberichtApp.waitForShutdown() never waited for pending saves to finish.
     */
    val isJobRunning: Boolean
        get() = runningJobs.get() != 0

    /* Loads the visible report list and reports the highest report id in the database.
     *
     * This used to wrap a bare scope.launch in runBlocking. runBlocking does not wait for a
     * coroutine launched into a *different* scope, and scope is bound to Dispatchers.Main,
     * which runBlocking was blocking at the time. So the body could not run before
     * initialize() had already returned, and initialize() always returned 0 with reportUids
     * still empty. Both results are used by ArbeitsberichtApp right after the call: the
     * return value recovers the id counter when the preferences were lost, and the report
     * list decides whether the last active report can be restored. This function is
     * suspending, so it can simply do the work and await it.
     */
    suspend fun initialize(): Int {
        val usedIds = withContext(Dispatchers.IO) { reportDao.getReportUids() }
        val uids = withContext(Dispatchers.IO) { reportDao.getFilteredReportIds(filter) }
        reportUidsMutex.withLock {
            reportUids.clear()
            reportUids.addAll(uids)
        }
        return usedIds.maxOrNull() ?: 0 // 0 if the database is still empty
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
            Log.d(TAG, "Adding new report ${r.cnt} with project ${r.project.name.value} to cache")
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

    private fun getReportByUid(cnt: Int, onLoaded:((r: ReportData)->Unit)? = null): ReportData {
        // If we have it in the cache, take if from there, else query the database and push it in the cache
        return reports.get(cnt)?.let { r ->
            onLoaded?.let { it(r) }
            r
        } ?: run {
            // If we don't have it in the cache then create one and let it populate from the database
            val report = ReportData.createReport(cnt, prefs)
            report.cacheState = ReportData.CacheState.CACHE_STATE_LOAD_ONGOING
            Log.d(TAG, "Adding report ${cnt} with project ${report.project.name.value} to cache")
            reports.put(cnt, report)
            scope.launch {
                val rDb = withContext(Dispatchers.IO) {
                    reportDao.getReportByCnt(cnt)
                }
                report.getReportFromDb(rDb)
                Log.d(TAG, "Loaded report ${report.cnt} with ${report.id.value} from database")
                onLoaded?.let { it(report) }
                report.cacheState = ReportData.CacheState.CACHE_STATE_READY
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
        r.cacheState = ReportData.CacheState.CACHE_STATE_READY
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
        r.cacheState = ReportData.CacheState.CACHE_STATE_READY
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
            Log.d(TAG, "Saving report with project name ${r.project.name.value}")
            reportDao.update(ReportDb.fromReport(r))
        }
        Log.d(TAG, "Saved report ${r.cnt} with project ${r.project.name.value} to database")
        r.modified = false
    }

    fun saveReport(r: ReportData) {
        if(r.cacheState == ReportData.CacheState.CACHE_STATE_READY) {
            if(!r.modified) {
                Log.w(TAG, "Warning: Saving a report which is not marked as modified")
            }
            launchAndMaintain {
                saveReportFromScope(r)
            }
        }
    }

    fun saveReports() {
        val cacheSnapshot = reports.snapshot()
        launchAndMaintain {
            cacheSnapshot.forEach { report ->
                if(report.value.modified && report.value.cacheState == ReportData.CacheState.CACHE_STATE_READY) {
                    saveReportFromScope(report.value)
                }
            }
        }
    }
}