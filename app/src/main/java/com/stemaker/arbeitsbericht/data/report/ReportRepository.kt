package com.stemaker.arbeitsbericht.data.report

import com.stemaker.arbeitsbericht.StorageHandler
import com.stemaker.arbeitsbericht.data.ReportDatabase
import com.stemaker.arbeitsbericht.data.configuration.Configuration
import com.stemaker.arbeitsbericht.helpers.ReportFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.beans.PropertyChangeEvent


class ReportListChangeEvent(val event: Int, val index: Int): PropertyChangeEvent() {
    const val NOTIFY_REPORT_ADDED = 1
    const val NOTIFY_REPORT_REMOVED = 2
    const val NOTIFY_REPORT_LIST_CHANGED = 3 // In this case index is irrelevant
}

class ReportRepository(val db: ReportDatabase, val cfg: Configuration, val filter: ReportFilter) :
    PropertyChangeListener
{
    /* reports shall likely be extended as a cache, for now it is a map of reports, potentially growing until all reports have been fetched from DB */
    private val reports: MutableMap<Int, ReportData> = mutableMapOf()

    /* reportCnts always needs to be up-to-date with the list of report cnts that are not filtered. We need to update it when adding and deleting reports
    *  as well as when the filter changes */
    private val reportCnts = mutableListOf<Int>()

    lateinit var initJob: Job

    fun initialize() {
        filter.addReportFilterObserver(this@ReportRepository)
        initJob = getReportCntsFromDb()
    }

    ////////////////////////////////////////////////////
    // Members regarding the class being an event source
    ////////////////////////////////////////////////////
    private val propCS = PropertyChangeSupport(this)
    fun addReportListObserver(listener: PropertyChangeListener) {
        propCS.addPropertyChangeListener(listener)
    }
    fun removeReportListObserver(listener: PropertyChangeListener) {
        propCS.removePropertyChangeListener(listener)
    }

    ////////////////////////////////////////////////////
    // Internal report list handling
    ////////////////////////////////////////////////////
    private fun getReportCntsFromDb(): Job {
        return GlobalScope.launch(Dispatchers.Main) {
            val reportCnts = withContext(Dispatchers.IO) {
                db.reportDao().getFilteredReportIds(filter)
            }
            propCS.firePropertyChange(ReportListChangeEvent(NOTIFY_REPORT_LIST_CHANGED, 0))
        }
    }
    private fun _deleteReport(r: ReportData) {
        reportCnts.remove(r.cnt)
        reports.remove(r.cnt)
        propCS.firePropertyChange(ReportListChangeEvent(NOTIFY_REPORT_REMOVED, r.cnt))
        GlobalScope.launch(Dispatchers.IO) {
            db.reportDao().deleteByCnt(cnt)
        }
    }
    private fun _addReport(r: ReportData) {
        reportCnts.add(r.cnt)
        reports[r.cnt] = r
        propCS.firePropertyChange(ReportListChangeEvent(NOTIFY_REPORT_ADDED, r.cnt))
        GlobalScope.launch(Dispatchers.IO) {
            db.reportDao().insert(ReportDb.fromReport(r))
        }
    }

    //////////////////////////////////////////////////////
    // Members regarding the class being an event listener
    //////////////////////////////////////////////////////
    // @TODO: Move this to java beans events
    override fun propertyChange(ev: PropertyChangeEvent) {
        if(ev is ReportFilterChangeEvent) {
            getReportCntsFromDb()
        }
    }
    override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
        if(sender is ReportData && propertyId == FILTER_IMPACTING_PROPERTY) {
            // Elements of the report changed that might change the filtered status
            if (filter.isFiltered(sender) && reportCnts.contains(sender.cnt)) {
                // New state is set to invisible and it is currently visible
                hideReport(sender.cnt)
                observers.forEach {
                    it.onPropertyChanged(this, NOTIFY_REPORT_REMOVED)
                }
            }
        }
//TODO: We do not support changing visible from false to true, not a use case right now
    }

    val amountOfReports: Int
        get() = reportCnts.size

    var activeReport: ReportData? = null
        private set

    suspend fun getReportByCnt(cnt: Int): ReportData {
        // If we have it in the cache, take if from there, else query the database and push it in the cache
        return reports[cnt]?.let {
            it
        } ?: run {
            val report = ReportData.createReport(cnt)
            reports[cnt] = report
            GlobalScope.launch(Dispatchers.Main) {
                val rDb = withContext(Dispatchers.IO) {
                    db.reportDao().getReportByCnt(cnt)
                }
                ReportData.getReportFromDb(rDb, report)
                report.addOnPropertyChangedCallback(this@ReportRepository)
            }
            return report
        }
    }

    suspend fun getReportByIndex(idx: Int): ReportData {
        return getReportByCnt(reportCnts[idx])
    }

    suspend fun setActiveReport(cnt: Int) {
        activeReport = getReportByCnt(cnt)
    }

    fun createReport(): ReportData {
        val r = ReportData.createReport(configuration().allocateId())
        configuration().activeReportId = r.cnt
        configuration().save()
        _addReport(r)
        return r
    }

    fun createReportAndActivate(): ReportData {
        val r = createReport()
        activeReport = r
        return r
    }

    fun deleteReport(r: ReportData) {
        // TODO: Remove observation?
        _deleteReport(r)
    }
}