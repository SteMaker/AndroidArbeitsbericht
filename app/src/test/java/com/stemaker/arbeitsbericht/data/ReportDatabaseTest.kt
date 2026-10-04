package com.stemaker.arbeitsbericht.data

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.data.client.ClientDb
import com.stemaker.arbeitsbericht.data.report.ReportDao
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.data.report.ReportDb
import com.stemaker.arbeitsbericht.helpers.ReportFilter
import com.stemaker.arbeitsbericht.support.FakePreferences
import com.stemaker.arbeitsbericht.support.configElementOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/* Exercises the real Room database rather than a stand-in, so the SQL itself is covered:
 * the report list query, the state/name filter the report list relies on, and the CBOR
 * type converters Room applies to the repeating sections.
 */
@RunWith(RobolectricTestRunner::class)
class ReportDatabaseTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: ReportDatabase
    private lateinit var dao: ReportDao
    private lateinit var prefs: FakePreferences

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), ReportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.reportDao()
        prefs = FakePreferences()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun filter(
        name: String = "",
        extra: String = "",
        states: Set<ReportData.ReportState> = setOf(
            ReportData.ReportState.IN_WORK,
            ReportData.ReportState.ON_HOLD,
            ReportData.ReportState.DONE),
    ) = ReportFilter(
        configElementOf(name),
        configElementOf(extra),
        configElementOf(states.fold(0) { acc, s -> acc or (1 shl s.ordinal) }))

    private fun report(
        cnt: Int,
        project: String = "",
        extra: String = "",
        state: ReportData.ReportState = ReportData.ReportState.IN_WORK,
    ): ReportDb = ReportDb.fromReport(
        ReportData.createReport(cnt, prefs.mock).apply {
            this.state.value = state
            this.project.name.value = project
            this.project.extra1.value = extra
        })

    // ---------------------------------------------------------------- basics

    @Test
    fun `an inserted report can be read back by its id`(): Unit = runBlocking {
        dao.insert(report(1, project = "Mueller", extra = "Dach"))

        val row = dao.getReportByCnt(1)!!

        assertThat(row.cnt).isEqualTo(1)
        assertThat(row.project.projectName).isEqualTo("Mueller")
        assertThat(row.project.extra1).isEqualTo("Dach")
    }

    @Test
    fun `report ids come back newest first`(): Unit = runBlocking {
        dao.insert(report(1))
        dao.insert(report(7))
        dao.insert(report(3))

        assertThat(dao.getReportUids()).containsExactly(7, 3, 1).inOrder()
    }

    @Test
    fun `getReportUids is empty for an empty database`(): Unit = runBlocking {
        assertThat(dao.getReportUids()).isEmpty()
    }

    @Test
    fun `inserting the same id again replaces the row`(): Unit = runBlocking {
        dao.insert(report(1, project = "First"))
        dao.insert(report(1, project = "Second"))

        assertThat(dao.getReports()).hasSize(1)
        assertThat(dao.getReportByCnt(1)!!.project.projectName).isEqualTo("Second")
    }

    @Test
    fun `update replaces the stored row`(): Unit = runBlocking {
        dao.insert(report(1, project = "First"))

        dao.update(report(1, project = "Updated"))

        assertThat(dao.getReportByCnt(1)!!.project.projectName).isEqualTo("Updated")
    }

    @Test
    fun `deleteByCnt removes only that report`(): Unit = runBlocking {
        dao.insert(report(1))
        dao.insert(report(2))

        dao.deleteByCnt(1)

        assertThat(dao.getReportUids()).containsExactly(2)
    }

    @Test
    fun `looking up a report that does not exist returns null`(): Unit = runBlocking {
        dao.insert(report(1))

        assertThat(dao.getReportByCnt(99)).isNull()
    }

    @Test
    fun `deleting a report that does not exist is harmless`(): Unit = runBlocking {
        dao.insert(report(1))

        dao.deleteByCnt(99)

        assertThat(dao.getReportUids()).containsExactly(1)
    }

    // ---------------------------------------------------------------- filtering

    @Test
    fun `the filter keeps only the selected states`(): Unit = runBlocking {
        dao.insert(report(1, state = ReportData.ReportState.IN_WORK))
        dao.insert(report(2, state = ReportData.ReportState.ON_HOLD))
        dao.insert(report(3, state = ReportData.ReportState.DONE))
        dao.insert(report(4, state = ReportData.ReportState.ARCHIVED))

        assertThat(dao.getFilteredReportIds(filter())).containsExactly(3, 2, 1).inOrder()
        assertThat(dao.getFilteredReportIds(
            filter(states = setOf(ReportData.ReportState.ARCHIVED)))).containsExactly(4)
    }

    @Test
    fun `an empty state filter matches nothing`(): Unit = runBlocking {
        dao.insert(report(1))

        assertThat(dao.getFilteredReportIds(filter(states = emptySet()))).isEmpty()
    }

    @Test
    fun `the project name filter matches a substring`(): Unit = runBlocking {
        dao.insert(report(1, project = "Mueller GmbH"))
        dao.insert(report(2, project = "Schmidt AG"))

        assertThat(dao.getFilteredReportIds(filter(name = "ueller"))).containsExactly(1)
    }

    @Test
    fun `the project name filter ignores case`(): Unit = runBlocking {
        dao.insert(report(1, project = "Mueller GmbH"))

        assertThat(dao.getFilteredReportIds(filter(name = "mueller"))).containsExactly(1)
        assertThat(dao.getFilteredReportIds(filter(name = "MUELLER"))).containsExactly(1)
    }

    @Test
    fun `an empty name filter matches every report`(): Unit = runBlocking {
        dao.insert(report(1, project = "Mueller"))
        dao.insert(report(2, project = ""))

        assertThat(dao.getFilteredReportIds(filter(name = ""))).containsExactly(2, 1).inOrder()
    }

    @Test
    fun `the extra filter matches a substring`(): Unit = runBlocking {
        dao.insert(report(1, project = "Mueller", extra = "Dachsanierung"))
        dao.insert(report(2, project = "Mueller", extra = "Fassade"))

        assertThat(dao.getFilteredReportIds(filter(extra = "dach"))).containsExactly(1)
    }

    @Test
    fun `name and extra filters are combined`(): Unit = runBlocking {
        dao.insert(report(1, project = "Mueller", extra = "Dach"))
        dao.insert(report(2, project = "Mueller", extra = "Fassade"))
        dao.insert(report(3, project = "Schmidt", extra = "Dach"))

        assertThat(dao.getFilteredReportIds(filter(name = "Mueller", extra = "Dach")))
            .containsExactly(1)
    }

    @Test
    fun `filtered ids come back newest first`(): Unit = runBlocking {
        dao.insert(report(1, project = "Mueller"))
        dao.insert(report(5, project = "Mueller"))
        dao.insert(report(3, project = "Mueller"))

        assertThat(dao.getFilteredReportIds(filter(name = "Mueller")))
            .containsExactly(5, 3, 1).inOrder()
    }

    @Test
    fun `umlauts in the project name are matched`(): Unit = runBlocking {
        dao.insert(report(1, project = "Müller"))

        // SQLite's UPPER() is ASCII-only, so an exact-case needle has to keep working
        assertThat(dao.getFilteredReportIds(filter(name = "Müller"))).containsExactly(1)
    }

    // ---------------------------------------------------------------- converters

    @Test
    fun `the repeating sections survive a real database round trip`(): Unit = runBlocking {
        val original = ReportData.createReport(1, prefs.mock).apply {
            workItemContainer.addWorkItem().item.value = "Ziegel ersetzt"
            materialContainer.addMaterial().apply {
                item.value = "Dachziegel"
                amount.value = 12.5f
                unit.value = "Stück"
            }
            lumpSumContainer.addLumpSum().apply {
                item.value = "Anfahrt"
                amount.value = 2
            }
            photoContainer.addPhoto().apply {
                file.value = "p.jpg"
                imageWidth.value = 800
                imageHeight.value = 600
            }
            workTimeContainer.addWorkTime().apply {
                startTime.value = "07:00"
                endTime.value = "16:00"
            }
        }
        dao.insert(ReportDb.fromReport(original))

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(dao.getReportByCnt(1)!!)

        assertThat(restored.workItemContainer.map { it.item.value })
            .containsExactly("Ziegel ersetzt")
        assertThat(restored.materialContainer.items[0].amount.value).isEqualTo(12.5f)
        assertThat(restored.materialContainer.items[0].unit.value).isEqualTo("Stück")
        assertThat(restored.lumpSumContainer.items[0].amount.value).isEqualTo(2)
        assertThat(restored.photoContainer.items[0].imageWidth.value).isEqualTo(800)
        assertThat(restored.workTimeContainer.items[0].startTime.value).isEqualTo("07:00")
    }

    // ---------------------------------------------------------------- clients

    @Test
    fun `clients come back ordered by name`(): Unit = runBlocking {
        val clientDao = db.clientDao()
        clientDao.insert(ClientDb(1, "Zimmermann"))
        clientDao.insert(ClientDb(2, "Albrecht"))
        clientDao.insert(ClientDb(3, "Mueller"))

        assertThat(clientDao.getClients().map { it.name })
            .containsExactly("Albrecht", "Mueller", "Zimmermann").inOrder()
    }

    @Test
    fun `a client round trips through the database`(): Unit = runBlocking {
        val clientDao = db.clientDao()
        val client = ClientDb(1, "Mueller", "Hauptstr. 1", "12345", "Musterstadt",
            42, true, "01:30", true, "Notizen")

        clientDao.insert(client)

        assertThat(clientDao.getClients().single()).isEqualTo(client)
    }

    @Test
    fun `deleting a client by id removes only that client`(): Unit = runBlocking {
        val clientDao = db.clientDao()
        clientDao.insert(ClientDb(1, "A"))
        clientDao.insert(ClientDb(2, "B"))

        clientDao.deleteById(1)

        assertThat(clientDao.getClients().map { it.id }).containsExactly(2)
    }

    @Test
    fun `report and client tables are independent`(): Unit = runBlocking {
        dao.insert(report(1))
        db.clientDao().insert(ClientDb(1, "Mueller"))

        dao.deleteByCnt(1)

        assertThat(db.clientDao().getClients()).hasSize(1)
    }
}
