package com.stemaker.arbeitsbericht.support

import com.stemaker.arbeitsbericht.data.report.ReportDao
import com.stemaker.arbeitsbericht.data.report.ReportDb

/* In-memory stand-in for the Room DAO. Mirrors the SQL semantics the real queries have:
 * ids come back newest first, and the filter matches on state plus a case-insensitive
 * substring of project name and extra.
 */
class FakeReportDao : ReportDao {
    val rows = linkedMapOf<Int, ReportDb>()

    var insertCount = 0
        private set
    var updateCount = 0
        private set
    var deleteCount = 0
        private set

    override suspend fun getReports(): List<ReportDb> = rows.values.toList()

    override suspend fun getReportByCnt(cnt: Int): ReportDb? = rows[cnt]

    // "SELECT cnt FROM ReportDb ORDER BY cnt DESC"
    override suspend fun getReportUids(): List<Int> = rows.keys.sortedDescending()

    override suspend fun getFilteredReportIdsString(
        stateFilter: Set<Int>,
        proj: String,
        extra: String,
    ): List<Int> {
        // The caller wraps the needles in SQL '%' wildcards and upper-cases them
        val projNeedle = proj.trim('%')
        val extraNeedle = extra.trim('%')
        return rows.values
            .filter { it.state in stateFilter }
            .filter { it.project.projectName.uppercase().contains(projNeedle) }
            .filter { it.project.extra1.uppercase().contains(extraNeedle) }
            .map { it.cnt }
            .sortedDescending()
    }

    override suspend fun insert(reportDb: ReportDb) {
        insertCount++
        rows[reportDb.cnt] = reportDb
    }

    override suspend fun update(reportDb: ReportDb) {
        updateCount++
        rows[reportDb.cnt] = reportDb
    }

    override suspend fun deleteByCnt(cnt: Int) {
        deleteCount++
        rows.remove(cnt)
    }

    override fun deleteTable() {
        rows.clear()
    }
}
