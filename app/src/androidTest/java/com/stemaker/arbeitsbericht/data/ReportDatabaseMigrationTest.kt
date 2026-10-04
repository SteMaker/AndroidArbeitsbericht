package com.stemaker.arbeitsbericht.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.data.client.ClientDb
import com.stemaker.arbeitsbericht.data.report.Converters
import com.stemaker.arbeitsbericht.data.report.LumpSumContainerDb
import com.stemaker.arbeitsbericht.data.report.MaterialContainerDb
import com.stemaker.arbeitsbericht.data.report.PhotoContainerDb
import com.stemaker.arbeitsbericht.data.report.WorkItemContainerDb
import com.stemaker.arbeitsbericht.data.report.WorkTimeContainerDb
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/* The app is being upgraded after several years, so existing installs will open their
 * version 2 database with the new build. ReportDatabase ships migr_2_3 for that, and
 * fallbackToDestructiveMigration behind it, which wipes every report if the migration is
 * ever wrong. These tests therefore check that a version 2 database really does migrate and
 * keeps its reports.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ReportDatabaseMigrationTest {

    private val dbName = "migration-test.db"
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /* The version 2 ReportDb, i.e. the version 3 table without the five columns migr_2_3
     * adds, and with no ClientDb table at all.
     */
    private val createReportDbV2 = """
        CREATE TABLE IF NOT EXISTS `ReportDb` (
            `cnt` INTEGER NOT NULL, `create_date` TEXT NOT NULL, `state` INTEGER NOT NULL,
            `change_date` TEXT NOT NULL, `workTimeContainer` BLOB NOT NULL,
            `workItemContainer` BLOB NOT NULL, `materialContainer` BLOB NOT NULL,
            `lumpSumContainer` BLOB NOT NULL, `photoContainer` BLOB NOT NULL,
            `projectName` TEXT NOT NULL, `extra1` TEXT NOT NULL,
            `projectVisibility` INTEGER NOT NULL, `billName` TEXT NOT NULL,
            `street` TEXT NOT NULL, `zip` TEXT NOT NULL, `city` TEXT NOT NULL,
            `billVisibility` INTEGER NOT NULL, `employeeSignatureSvg` TEXT NOT NULL,
            `clientSignatureSvg` TEXT NOT NULL, PRIMARY KEY(`cnt`))
    """.trimIndent()

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    /* Creates a version 2 database holding one report and closes it again. */
    private fun createVersion2Database(reportCnt: Int, projectName: String) {
        val callback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(createReportDbV2)
                db.execSQL(
                    "INSERT INTO ReportDb (cnt, create_date, state, change_date, " +
                        "workTimeContainer, workItemContainer, materialContainer, " +
                        "lumpSumContainer, photoContainer, projectName, extra1, " +
                        "projectVisibility, billName, street, zip, city, billVisibility, " +
                        "employeeSignatureSvg, clientSignatureSvg) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any>(
                        reportCnt, "12.03.2024", 0, "",
                        emptyWorkTime, emptyWorkItem, emptyMaterial, emptyLumpSum,
                        emptyPhoto,
                        projectName, "Dachsanierung", 1,
                        "Mueller Holding", "Hauptstr. 1", "12345", "Musterstadt", 1,
                        "", ""))
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                Unit
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase.close()
    }

    /* Each container column is decoded with its own serializer, so every blob has to be
     * encoded from the matching empty container rather than from a single generic one.
     */
    private val converters = Converters()
    private val emptyWorkTime get() =
        converters.workTimeContainerSerializer(WorkTimeContainerDb(emptyList()))
    private val emptyWorkItem get() =
        converters.workItemContainerSerializer(WorkItemContainerDb(emptyList()))
    private val emptyMaterial get() =
        converters.materialContainerSerializer(MaterialContainerDb(emptyList()))
    private val emptyLumpSum get() =
        converters.lumpSumContainerSerializer(LumpSumContainerDb(emptyList()))
    private val emptyPhoto get() =
        converters.photoContainerSerializer(PhotoContainerDb(emptyList()))

    private fun openWithMigration(): ReportDatabase =
        Room.databaseBuilder(context, ReportDatabase::class.java, dbName)
            .addMigrations(ReportDatabase.migr_2_3)
            .build()

    @Test
    fun aVersion2DatabaseMigratesAndKeepsItsReports() {
        createVersion2Database(reportCnt = 42, projectName = "Mueller GmbH")

        val db = openWithMigration()
        try {
            val reports = runBlocking { db.reportDao().getReports() }

            assertThat(reports).hasSize(1)
            val report = reports.single()
            assertThat(report.cnt).isEqualTo(42)
            assertThat(report.project.projectName).isEqualTo("Mueller GmbH")
            assertThat(report.project.extra1).isEqualTo("Dachsanierung")
            assertThat(report.bill.city).isEqualTo("Musterstadt")
            assertThat(report.create_date).isEqualTo("12.03.2024")
        } finally {
            db.close()
        }
    }

    @Test
    fun theMigrationFillsTheNewDefaultValueColumns() {
        createVersion2Database(reportCnt = 1, projectName = "Mueller")

        val db = openWithMigration()
        try {
            val report = runBlocking { db.reportDao().getReportByCnt(1)!! }

            assertThat(report.defaultValues.defaultDriveTime).isEqualTo("00:00")
            assertThat(report.defaultValues.useDefaultDriveTime).isFalse()
            assertThat(report.defaultValues.defaultDistance).isEqualTo(0)
            assertThat(report.defaultValues.useDefaultDistance).isFalse()
        } finally {
            db.close()
        }
    }

    @Test
    fun theMigrationLeavesMigratedReportsWithoutAClient() {
        createVersion2Database(reportCnt = 1, projectName = "Mueller")

        val db = openWithMigration()
        try {
            val report = runBlocking { db.reportDao().getReportByCnt(1)!! }

            // Int.MAX_VALUE is the "no client assigned" marker
            assertThat(report.project.clientId).isEqualTo(Int.MAX_VALUE)
        } finally {
            db.close()
        }
    }

    @Test
    fun theMigrationCreatesAnEmptyClientTableThatCanBeUsed() {
        createVersion2Database(reportCnt = 1, projectName = "Mueller")

        val db = openWithMigration()
        try {
            val clientDao = db.clientDao()
            assertThat(runBlocking { clientDao.getClients() }).isEmpty()

            runBlocking {
                clientDao.insert(ClientDb(1, "Neu angelegt"))
            }

            assertThat(runBlocking { clientDao.getClients() }.single().name)
                .isEqualTo("Neu angelegt")
        } finally {
            db.close()
        }
    }

    @Test
    fun aMigratedDatabaseStillAcceptsNewReports() {
        createVersion2Database(reportCnt = 7, projectName = "Mueller")

        val db = openWithMigration()
        try {
            val dao = db.reportDao()
            val existing = runBlocking { dao.getReportByCnt(7)!! }
            runBlocking { dao.insert(existing.copy(cnt = 8)) }

            assertThat(runBlocking { dao.getReportUids() }).containsExactly(8, 7).inOrder()
        } finally {
            db.close()
        }
    }
}
