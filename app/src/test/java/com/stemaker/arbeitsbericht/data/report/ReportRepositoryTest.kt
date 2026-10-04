package com.stemaker.arbeitsbericht.data.report

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.support.FakePreferences
import com.stemaker.arbeitsbericht.support.FakeReportDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/* ReportRepository owns the report cache, the visible-report list and every write to the
 * database. The repository builds its own CoroutineScope on Dispatchers.Main, so the main
 * dispatcher has to be replaced before it is constructed.
 *
 * Its database work is hardcoded to Dispatchers.IO, which no test dispatcher can control,
 * so the tests use real time and await the outcome via awaitUntil instead of trying to
 * drive a virtual clock.
 *
 * Runs under Robolectric because the report cache is an android.util.LruCache. Against the
 * plain JVM stubs that class silently does nothing, so every lookup would miss the cache
 * and the tests would not exercise the real caching behaviour.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ReportRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var prefs: FakePreferences
    private lateinit var dao: FakeReportDao

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        prefs = FakePreferences()
        dao = FakeReportDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repository() = ReportRepository(dao, prefs.mock)

    /* Waits for work the repository dispatched to Dispatchers.IO to land. */
    private fun awaitUntil(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            Thread.sleep(2)
        }
        if (!condition()) throw AssertionError("timed out waiting for: $what")
    }

    /* Puts a row straight into the fake database, bypassing the repository. */
    private fun seedRow(cnt: Int, project: String = "", extra: String = "",
                        state: ReportData.ReportState = ReportData.ReportState.IN_WORK) {
        val r = ReportData.createReport(cnt, prefs.mock).apply {
            this.state.value = state
            this.project.name.value = project
            this.project.extra1.value = extra
        }
        dao.rows[cnt] = ReportDb.fromReport(r)
    }

    // ---------------------------------------------------------------- initialize

    @Test
    fun `initialize reports the highest report id already in the database`(): Unit = runBlocking {
        // ArbeitsberichtApp uses this to recover the id counter if the preferences were
        // lost, so returning 0 for a populated database would silently disable that.
        seedRow(3)
        seedRow(11)
        seedRow(7)
        val repo = repository()

        val maxUid = repo.initialize()

        assertThat(maxUid).isEqualTo(11)
    }

    @Test
    fun `initialize reports zero for an empty database`(): Unit = runBlocking {
        val repo = repository()

        assertThat(repo.initialize()).isEqualTo(0)
    }

    @Test
    fun `the visible report list is populated by the time initialize returns`(): Unit = runBlocking {
        // ArbeitsberichtApp calls isReportVisible() right after initialize() to decide
        // whether the last active report can be restored.
        seedRow(5, project = "Mueller")
        val repo = repository()

        repo.initialize()

        assertThat(repo.isReportVisible(5)).isTrue()
        assertThat(repo.amountOfReports).isEqualTo(1)
    }

    @Test
    fun `initialize honours the state filter`(): Unit = runBlocking {
        seedRow(1, state = ReportData.ReportState.IN_WORK)
        seedRow(2, state = ReportData.ReportState.ARCHIVED)
        val repo = repository()

        repo.initialize()

        // The default filter excludes archived reports
        assertThat(repo.isReportVisible(1)).isTrue()
        assertThat(repo.isReportVisible(2)).isFalse()
        assertThat(repo.amountOfReports).isEqualTo(1)
    }

    // ---------------------------------------------------------------- create

    @Test
    fun `creating a report allocates the next id and writes it to the database`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()

        val created = repo.createReportAndActivate(null)
        awaitUntil("the new report to be inserted") { dao.rows.containsKey(1) }

        assertThat(created.cnt).isEqualTo(1)
        assertThat(dao.rows).containsKey(1)
        assertThat(dao.insertCount).isEqualTo(1)
    }

    @Test
    fun `a created report becomes the active report`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()

        val created = repo.createReportAndActivate(null)

        assertThat(repo.activeReport).isSameInstanceAs(created)
        assertThat(prefs.activeReportId.value).isEqualTo(created.cnt)
    }

    @Test
    fun `a created report is immediately visible and counted`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()

        val created = repo.createReportAndActivate(null)

        assertThat(repo.isReportVisible(created.cnt)).isTrue()
        assertThat(repo.amountOfReports).isEqualTo(1)
    }

    @Test
    fun `the onCreated callback receives the new report`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        var seen: ReportData? = null

        val created = repo.createReportAndActivate { seen = it }
        awaitUntil("the onCreated callback") { seen != null }

        assertThat(seen).isSameInstanceAs(created)
    }

    @Test
    fun `a created report is ready to be used straight away`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()

        val created = repo.createReportAndActivate(null)

        assertThat(created.cacheState).isEqualTo(ReportData.CacheState.CACHE_STATE_READY)
    }

    @Test
    fun `consecutive reports get consecutive ids`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()

        val first = repo.createReportAndActivate(null)
        val second = repo.createReportAndActivate(null)

        assertThat(second.cnt).isEqualTo(first.cnt + 1)
        assertThat(repo.amountOfReports).isEqualTo(2)
    }

    // ---------------------------------------------------------------- duplicate

    @Test
    fun `duplicating a report stores a second row with its own id`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val origin = repo.createReportAndActivate(null)
        origin.project.name.value = "Mueller"

        val duplicate = repo.duplicateReportAndActivate(origin, null)
        awaitUntil("the duplicate to be inserted") { dao.rows.size == 2 }

        assertThat(duplicate.cnt).isNotEqualTo(origin.cnt)
        assertThat(duplicate.project.name.value).isEqualTo("Mueller")
        assertThat(dao.rows).containsKey(duplicate.cnt)
        assertThat(repo.amountOfReports).isEqualTo(2)
    }

    @Test
    fun `a duplicate becomes the active report`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val origin = repo.createReportAndActivate(null)

        val duplicate = repo.duplicateReportAndActivate(origin, null)

        assertThat(repo.activeReport).isSameInstanceAs(duplicate)
        assertThat(prefs.activeReportId.value).isEqualTo(duplicate.cnt)
    }

    // ---------------------------------------------------------------- delete

    @Test
    fun `deleting a report removes it from the database and the visible list`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val report = repo.createReportAndActivate(null)
        // Wait for the insert first: the repository dispatches insert and delete
        // independently, so deleting straight away can outrun the insert and leave the
        // row behind.
        awaitUntil("the report to be inserted") { dao.rows.containsKey(report.cnt) }

        repo.deleteReport(report)
        awaitUntil("the report to be deleted") { dao.deleteCount == 1 }

        assertThat(dao.rows).doesNotContainKey(report.cnt)
        assertThat(repo.isReportVisible(report.cnt)).isFalse()
        assertThat(repo.amountOfReports).isEqualTo(0)
        assertThat(dao.deleteCount).isEqualTo(1)
    }

    @Test
    fun `deleting one report leaves the others alone`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val first = repo.createReportAndActivate(null)
        val second = repo.createReportAndActivate(null)
        awaitUntil("both reports to be inserted") { dao.rows.size == 2 }

        repo.deleteReport(first)
        awaitUntil("the first report to be deleted") { dao.deleteCount == 1 }

        assertThat(repo.isReportVisible(second.cnt)).isTrue()
        assertThat(dao.rows).containsKey(second.cnt)
        assertThat(repo.amountOfReports).isEqualTo(1)
    }

    @Test
    fun `a list change event is emitted when a report is added and removed`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val events = mutableListOf<ReportRepository.ReportListChangeEvent>()
        repo.live.observeForever { events.add(it) }
        events.clear()

        val report = repo.createReportAndActivate(null)
        awaitUntil("the add event") { events.isNotEmpty() }
        awaitUntil("the report to be inserted") { dao.rows.containsKey(report.cnt) }
        repo.deleteReport(report)
        awaitUntil("the remove event") { events.size == 2 }

        assertThat(events.map { it.type }).containsExactly(
            ReportRepository.ReportListChangeEvent.Type.LIST_ADD,
            ReportRepository.ReportListChangeEvent.Type.LIST_REMOVE).inOrder()
    }

    // ------------------------------------------------- list event ordering

    @Test
    fun `the add event is emitted before the row is written`(): Unit = runBlocking {
        /* ReportListAdapter.getItemCount() reads amountOfReports directly, so the visible
         * count changes the moment reportUids does. If the event came after the database
         * write, the main thread could lay the list out in between with a count
         * RecyclerView had not been told about, and RecyclerView then throws
         * "Inconsistency detected. Invalid view holder adapter position".
         */
        val repo = repository()
        repo.initialize()
        var insertsWhenAnnounced: Int? = null
        repo.live.observeForever { event ->
            if (event.type == ReportRepository.ReportListChangeEvent.Type.LIST_ADD &&
                insertsWhenAnnounced == null) {
                insertsWhenAnnounced = dao.insertCount
            }
        }

        repo.createReportAndActivate(null)
        awaitUntil("the add event") { insertsWhenAnnounced != null }

        assertThat(insertsWhenAnnounced).isEqualTo(0)
    }

    @Test
    fun `the add event reports the position the report was inserted at`(): Unit = runBlocking {
        seedRow(1)
        val repo = repository()
        repo.initialize()
        var pos: Int? = null
        repo.live.observeForever { event ->
            if (event.type == ReportRepository.ReportListChangeEvent.Type.LIST_ADD) {
                pos = event.pos
            }
        }

        repo.createReportAndActivate(null)
        awaitUntil("the add event") { pos != null }

        // New reports go to the top of the list
        assertThat(pos).isEqualTo(0)
    }

    @Test
    fun `the remove event is emitted before the row is deleted`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val report = repo.createReportAndActivate(null)
        awaitUntil("the report to be inserted") { dao.rows.containsKey(report.cnt) }
        var deletesWhenAnnounced: Int? = null
        repo.live.observeForever { event ->
            if (event.type == ReportRepository.ReportListChangeEvent.Type.LIST_REMOVE &&
                deletesWhenAnnounced == null) {
                deletesWhenAnnounced = dao.deleteCount
            }
        }

        repo.deleteReport(report)
        awaitUntil("the remove event") { deletesWhenAnnounced != null }

        assertThat(deletesWhenAnnounced).isEqualTo(0)
    }

    @Test
    fun `the visible count matches what the remove event announced`(): Unit = runBlocking {
        seedRow(1)
        seedRow(2)
        val repo = repository()
        repo.initialize()
        var countAtEvent: Int? = null
        var removedPos: Int? = null
        repo.live.observeForever { event ->
            if (event.type == ReportRepository.ReportListChangeEvent.Type.LIST_REMOVE) {
                countAtEvent = repo.amountOfReports
                removedPos = event.pos
            }
        }
        var toDelete: ReportData? = null
        repo.getReportByIndex(0) { toDelete = it }
        awaitUntil("the report to load") { toDelete != null }

        repo.deleteReport(toDelete!!)
        awaitUntil("the remove event") { countAtEvent != null }

        // The list has already shrunk by the time the adapter is told, and the announced
        // position is the one the report used to occupy
        assertThat(countAtEvent).isEqualTo(1)
        assertThat(removedPos).isEqualTo(0)
    }

    // ---------------------------------------------------------------- load

    @Test
    fun `a report is loaded from the database on demand`(): Unit = runBlocking {
        seedRow(4, project = "Mueller", extra = "Dach")
        val repo = repository()
        repo.initialize()
        var loaded: ReportData? = null

        repo.setActiveReport(4) { loaded = it }
        awaitUntil("report 4 to be loaded") { loaded != null }

        assertThat(loaded).isNotNull()
        assertThat(loaded!!.cnt).isEqualTo(4)
        assertThat(loaded!!.project.name.value).isEqualTo("Mueller")
        assertThat(loaded!!.project.extra1.value).isEqualTo("Dach")
        assertThat(loaded!!.cacheState).isEqualTo(ReportData.CacheState.CACHE_STATE_READY)
    }

    @Test
    fun `a report already in the cache is handed back without touching the database`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val created = repo.createReportAndActivate(null)

        awaitUntil("the report to be inserted") { dao.rows.isNotEmpty() }
        var loaded: ReportData? = null
        repo.setActiveReport(created.cnt) { loaded = it }

        assertThat(loaded).isSameInstanceAs(created)
    }

    @Test
    fun `getReportByIndex resolves positions in the visible list`(): Unit = runBlocking {
        seedRow(1, project = "A")
        seedRow(2, project = "B")
        val repo = repository()
        repo.initialize()

        var first: ReportData? = null
        repo.getReportByIndex(0) { first = it }
        awaitUntil("the report at index 0 to be loaded") { first != null }

        // getReportUids() and the filtered query both order by cnt descending
        assertThat(first!!.cnt).isEqualTo(2)
    }

    @Test
    fun `asking for a report whose row is gone does not blow up`(): Unit = runBlocking {
        /* The visible list can name a report that is no longer in the database, for
         * instance when a list refresh overlaps a delete that has not been written yet.
         * Room throws on a missing row for a non-null return type, which used to take the
         * whole app down from a coroutine on the main dispatcher.
         */
        seedRow(5, project = "Verschwunden")
        val repo = repository()
        repo.initialize()
        assertThat(repo.isReportVisible(5)).isTrue()

        // The row disappears behind the repository's back
        dao.rows.remove(5)
        var loaded: ReportData? = null
        repo.setActiveReport(5) { loaded = it }

        awaitUntil("the missing report to be dropped from the list") {
            !repo.isReportVisible(5)
        }
        assertThat(loaded).isNull()
        assertThat(repo.amountOfReports).isEqualTo(0)
    }

    @Test
    fun `a vanished report makes the repository announce a list change`(): Unit = runBlocking {
        seedRow(5)
        val repo = repository()
        repo.initialize()
        val events = mutableListOf<ReportRepository.ReportListChangeEvent>()
        repo.live.observeForever { events.add(it) }
        events.clear()

        dao.rows.remove(5)
        repo.setActiveReport(5) { }

        awaitUntil("a list change event") {
            events.any { it.type == ReportRepository.ReportListChangeEvent.Type.LIST_CHANGE }
        }
    }

    // ---------------------------------------------------------------- save

    @Test
    fun `saving a modified report writes it back`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val report = repo.createReportAndActivate(null)
        report.project.name.value = "Changed"
        val updatesBefore = dao.updateCount

        repo.saveReport(report)
        awaitUntil("the report to be updated") { dao.updateCount == updatesBefore + 1 }

        assertThat(dao.updateCount).isEqualTo(updatesBefore + 1)
        assertThat(dao.rows[report.cnt]!!.project.projectName).isEqualTo("Changed")
        assertThat(report.modified).isFalse()
    }

    @Test
    fun `saveReports writes every modified cached report`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val first = repo.createReportAndActivate(null)
        val second = repo.createReportAndActivate(null)
        first.project.name.value = "First"
        second.project.name.value = "Second"

        repo.saveReports()
        awaitUntil("both reports to be written back") {
            dao.rows[first.cnt]?.project?.projectName == "First" &&
                dao.rows[second.cnt]?.project?.projectName == "Second"
        }

        assertThat(dao.rows[first.cnt]!!.project.projectName).isEqualTo("First")
        assertThat(dao.rows[second.cnt]!!.project.projectName).isEqualTo("Second")
    }

    @Test
    fun `saveReports skips reports that were not modified`(): Unit = runBlocking {
        val repo = repository()
        repo.initialize()
        val report = repo.createReportAndActivate(null)
        report.project.name.value = "Saved once"
        repo.saveReport(report)
        awaitUntil("the first save") { !report.modified }
        val updatesBefore = dao.updateCount

        repo.saveReports()
        Thread.sleep(200) // nothing should happen, so give it a chance to happen

        assertThat(dao.updateCount).isEqualTo(updatesBefore)
    }

    @Test
    fun `isJobRunning reflects whether work is outstanding`(): Unit = runBlocking {
        // waitForShutdown() polls this to decide whether pending saves have reached the
        // database, so it must not be a value frozen at construction time.
        val repo = repository()
        repo.initialize()

        // With an unconfined dispatcher every launched job completes eagerly
        assertThat(repo.isJobRunning).isFalse()
    }

    // ---------------------------------------------------------------- filter

    @Test
    fun `changing the filter rebuilds the visible list`(): Unit = runBlocking {
        seedRow(1, project = "Mueller")
        seedRow(2, project = "Schmidt")
        val repo = repository()
        repo.initialize()
        assertThat(repo.amountOfReports).isEqualTo(2)

        repo.filter.projectName = "Mueller"
        awaitUntil("the list to be filtered down") { repo.amountOfReports == 1 }

        assertThat(repo.amountOfReports).isEqualTo(1)
        assertThat(repo.isReportVisible(1)).isTrue()
        assertThat(repo.isReportVisible(2)).isFalse()
    }

    @Test
    fun `the project name filter is case insensitive`(): Unit = runBlocking {
        seedRow(1, project = "Mueller")
        val repo = repository()
        repo.initialize()

        repo.filter.projectName = "mueller"
        awaitUntil("the list to be refiltered") { repo.isReportVisible(1) }

        assertThat(repo.isReportVisible(1)).isTrue()
    }

    @Test
    fun `enabling the archived state brings archived reports back`(): Unit = runBlocking {
        seedRow(1, state = ReportData.ReportState.ARCHIVED)
        val repo = repository()
        repo.initialize()
        assertThat(repo.isReportVisible(1)).isFalse()

        repo.filter.archived = true
        awaitUntil("archived reports to reappear") { repo.isReportVisible(1) }

        assertThat(repo.isReportVisible(1)).isTrue()
    }

    @Test
    fun `a filter change emits a list change event`(): Unit = runBlocking {
        seedRow(1, project = "Mueller")
        val repo = repository()
        repo.initialize()
        val events = mutableListOf<ReportRepository.ReportListChangeEvent>()
        repo.live.observeForever { events.add(it) }
        events.clear()

        repo.filter.projectName = "Mueller"
        awaitUntil("a list change event") { events.isNotEmpty() }

        assertThat(events.map { it.type })
            .contains(ReportRepository.ReportListChangeEvent.Type.LIST_CHANGE)
    }
}
