package com.stemaker.arbeitsbericht.data

import androidx.lifecycle.MutableLiveData
import androidx.room.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

@Dao
interface ReportDao {
    @Query("SELECT * FROM ReportDb")
    suspend fun getReports(): List<ReportDb>

    @Query("SELECT * FROM ReportDb WHERE id = :id")
    suspend fun getReportById(id: String): ReportDb

    @Query("SELECT id FROM ReportDb ORDER BY id DESC")
    suspend fun getReportIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reportDb: ReportDb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(reportDb: ReportDb)

    @Query("DELETE FROM ReportDb WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ReportDb")
    fun deleteTable()
}

class Converters {
    @TypeConverter
    fun workTimeContainerSerializer(wtc: WorkTimeContainerDb): ByteArray {
        return Cbor.encodeToByteArray(wtc)
    }

    @TypeConverter
    fun workTimeContainerDeserializer(s: ByteArray): WorkTimeContainerDb {
        return Cbor.decodeFromByteArray<WorkTimeContainerDb>(s)
    }
}
@Entity
data class ReportDb(
    @PrimaryKey val id: String,
    val cnt: Int,
    val create_date: String,
    val state: Int,
    val change_date: String,
    @Embedded val project: ProjectDb,
    @Embedded val bill: BillDb,
    @Embedded val signatures: SignatureDb,
    val workTimeContainerDb: WorkTimeContainerDb
) {

    companion object {
        fun fromReport(r: ReportData): ReportDb = ReportDb(r.id, r.cnt, r.create_date.value!!, ReportData.ReportState.toInt(r.state.value!!),
            "", ProjectDb.fromReport(r.project), BillDb.fromReport(r.bill), SignatureDb.fromReport(r.signatureData),
            WorkTimeContainerDb.fromReport(r.workTimeContainer) )
    }
}

data class ProjectDb(
    val projectName: String,
    val extra1: String,
    val projectVisibility: Boolean
) {

    companion object {
        fun fromReport(p: ProjectData): ProjectDb = ProjectDb(p.name.value!!, p.extra1.value!!, p.visibility.value!!)
    }
}

data class BillDb(
    val billName: String,
    val street: String,
    val zip: String,
    val city: String,
    val billVisibility: Boolean
) {

    companion object {
        fun fromReport(b: BillData) = BillDb(b.name.value!!, b.street.value!!, b.zip.value!!, b.city.value!!, b.visibility.value!!)
    }
}

data class SignatureDb(
    val employeeSignatureSvg: String,
    val clientSignatureSvg: String
) {

    companion object {
        fun fromReport(s: SignatureData) = SignatureDb(s.employeeSignatureSvg.value!!, s.clientSignatureSvg.value!!)
    }
}


@Serializable
data class WorkTimeContainerDb (
    val items: List<WorkTimeDb>,
    var visibility: Boolean = true
) {
    @Serializable
    data class WorkTimeDb (
        val date: String,
        val employees: List<String>,
        val startTime: String,
        val endTime: String,
        val pauseDuration: String,
        val driveTime: String,
        val distance: Int
    ) {
        companion object {
            fun fromReport(w: WorkTimeData): WorkTimeDb = WorkTimeDb(w.date.value!!, extractEmployees(w.employees), w.startTime.value!!, w.endTime.value!!, w.pauseDuration.value!!, w.driveTime.value!!, w.distance.value!!)
            private fun extractEmployees(e: List<MutableLiveData<String>>): List<String> {
                val l = mutableListOf<String>()
                for(s in e) {
                    l.add(s.value!!)
                }
                return l
            }
        }
    }

    companion object {
        fun fromReport(wc: WorkTimeContainerData): WorkTimeContainerDb {
            val l = mutableListOf<WorkTimeDb>()
            for(wt in wc.items) {
                l.add(WorkTimeDb.fromReport(wt))
            }
            return WorkTimeContainerDb(l, wc.visibility.value!!)
        }
    }
}

@Database(entities = [ReportDb::class], version = 1)
@TypeConverters(Converters::class)
abstract class ReportDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
}
