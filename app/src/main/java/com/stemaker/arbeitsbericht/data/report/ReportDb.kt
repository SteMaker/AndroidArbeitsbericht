package com.stemaker.arbeitsbericht.data.report

import androidx.room.*
import com.stemaker.arbeitsbericht.data.base.DataContainer
import com.stemaker.arbeitsbericht.data.base.DataElement
import com.stemaker.arbeitsbericht.data.calendarToDateString
import com.stemaker.arbeitsbericht.helpers.ReportFilter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

@Dao
interface ReportDao {
    @Query("SELECT * FROM ReportDb")
    suspend fun getReports(): List<ReportDb>

    @Query("SELECT * FROM ReportDb WHERE cnt = :cnt")
    suspend fun getReportByCnt(cnt: Int): ReportDb

    @Query("SELECT cnt FROM ReportDb ORDER BY cnt DESC")
    suspend fun getReportUids(): List<Int>

    @Query("SELECT cnt FROM ReportDb WHERE state IN (:stateFilter) AND UPPER(projectName) LIKE (:proj) AND UPPER(extra1) LIKE (:extra) ORDER BY cnt DESC")
    suspend fun getFilteredReportIdsString(stateFilter: Set<Int>, proj: String, extra: String): List<Int>

    suspend fun getFilteredReportIds(filter: ReportFilter): List<Int> {
        return getFilteredReportIdsString(filter.remainingStates, "%${filter.projectName.uppercase()}%",
            "%${filter.projectExtra.uppercase()}%")
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reportDb: ReportDb)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(reportDb: ReportDb)

    @Query("DELETE FROM ReportDb WHERE cnt = :cnt")
    suspend fun deleteByCnt(cnt: Int)

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
    @TypeConverter
    fun workItemContainerSerializer(wic: WorkItemContainerDb): ByteArray {
        return Cbor.encodeToByteArray(wic)
    }
    @TypeConverter
    fun workItemContainerDeserializer(s: ByteArray): WorkItemContainerDb {
        return Cbor.decodeFromByteArray<WorkItemContainerDb>(s)
    }
    @TypeConverter
    fun materialContainerSerializer(m: MaterialContainerDb): ByteArray {
        return Cbor.encodeToByteArray(m)
    }
    @TypeConverter
    fun materialContainerDeserializer(s: ByteArray): MaterialContainerDb {
        return Cbor.decodeFromByteArray<MaterialContainerDb>(s)
    }
    @TypeConverter
    fun lumpSumContainerSerializer(l: LumpSumContainerDb): ByteArray {
        return Cbor.encodeToByteArray(l)
    }
    @TypeConverter
    fun lumpSumContainerDeserializer(s: ByteArray): LumpSumContainerDb {
        return Cbor.decodeFromByteArray<LumpSumContainerDb>(s)
    }
    @TypeConverter
    fun photoContainerSerializer(p: PhotoContainerDb): ByteArray {
        return Cbor.encodeToByteArray(p)
    }
    @TypeConverter
    fun photoContainerDeserializer(s: ByteArray): PhotoContainerDb {
        return Cbor.decodeFromByteArray<PhotoContainerDb>(s)
    }
}
@Entity
data class ReportDb(
    @PrimaryKey val cnt: Int,
    val create_date: String,
    val state: Int,
    val change_date: String,
    @Embedded val project: ProjectDb,
    @Embedded val bill: BillDb,
    @Embedded val signatures: SignatureDb,
    val workTimeContainer: WorkTimeContainerDb,
    val workItemContainer: WorkItemContainerDb,
    val materialContainer: MaterialContainerDb,
    val lumpSumContainer: LumpSumContainerDb,
    val photoContainer: PhotoContainerDb,
    @Embedded val defaultValues: DefaultValuesDb
) {

    companion object {
        fun fromReport(r: ReportData): ReportDb = ReportDb(r.cnt, r.create_date.value!!, ReportData.ReportState.toInt(r.state.value!!),
            "", ProjectDb.fromReport(r.project), BillDb.fromReport(r.bill), SignatureDb.fromReport(r.signatureData),
            WorkTimeContainerDb.fromReport(r.workTimeContainer), WorkItemContainerDb.fromReport(r.workItemContainer),
            MaterialContainerDb.fromReport(r.materialContainer), LumpSumContainerDb.fromReport(r.lumpSumContainer),
            PhotoContainerDb.fromReport(r.photoContainer), DefaultValuesDb.fromReport(r.defaultValues) )
    }
}

data class ProjectDb(
    val projectName: String,
    val extra1: String,
    val projectVisibility: Boolean,
    var clientId: Int
) {

    companion object {
        fun fromReport(p: ProjectData): ProjectDb = ProjectDb(p.name.value!!, p.extra1.value!!, p.visibility.value!!, p.clientId)
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
    val wtItems: List<WorkTimeDb>,
    val wtVisibility: Boolean = true
) {
    @Serializable
    data class WorkTimeDb (
        val wtDate: String,
        val wtEmployees: List<String>,
        val wtStartTime: String,
        val wtEndTime: String,
        val wtPauseDuration: String,
        val wtDriveTime: String,
        val wtDistance: Int
    ) {
        companion object {
            fun fromReport(w: WorkTimeData): WorkTimeDb = WorkTimeDb(calendarToDateString(w.date.value), extractEmployees(w.employees), w.startTime.value!!, w.endTime.value!!, w.pauseDuration.value!!, w.driveTime.value!!, w.distance.value!!)
            private fun extractEmployees(e: DataContainer<DataElement<String>>): List<String> {
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

@Serializable
data class WorkItemContainerDb(
    val wiItems: List<WorkItemDb>,
    val wiVisibility: Boolean = true
) {
    @Serializable
    data class WorkItemDb(
    val wiItem: String
    ) {
        companion object {
            fun fromReport(w: WorkItemData): WorkItemDb = WorkItemDb(w.item.value!!)
        }
    }
    companion object {
        fun fromReport(wc: WorkItemContainerData): WorkItemContainerDb {
            val l = mutableListOf<WorkItemDb>()
            for(wi in wc.items) {
                l.add(WorkItemDb.fromReport(wi))
            }
            return WorkItemContainerDb(l, wc.visibility.value!!)
        }
    }
}

@Serializable
data class MaterialContainerDb(
    val mItems: List<MaterialDb>,
    val mVisibility: Boolean = true
) {
    @Serializable
    data class MaterialDb(
        val mItem: String,
        var mAmount: Float,
        var mUnit: String
    ) {
        companion object {
            fun fromReport(m: MaterialData): MaterialDb = MaterialDb(m.item.value!!, m.amount.value!!, m.unit.value!!)
        }
    }

    companion object {
        fun fromReport(mc: MaterialContainerData): MaterialContainerDb {
            val l = mutableListOf<MaterialDb>()
            for(m in mc.items) {
                l.add(MaterialDb.fromReport(m))
            }
            return MaterialContainerDb(l, mc.visibility.value!!)
        }
    }
}

@Serializable
data class LumpSumContainerDb(
    val lItems: List<LumpSumDb>,
    val lVisibility: Boolean = true
) {
    @Serializable
    data class LumpSumDb(
        val lItem: String,
        var lAmount: Int,
        var lComment: String
    ) {
        companion object {
            fun fromReport(l: LumpSumData): LumpSumDb = LumpSumDb(l.item.value!!, l.amount.value!!, l.comment.value!!)
        }
    }

    companion object {
        fun fromReport(lc: LumpSumContainerData): LumpSumContainerDb {
            val l = mutableListOf<LumpSumDb>()
            for(i in lc.items) {
                l.add(LumpSumDb.fromReport(i))
            }
            return LumpSumContainerDb(l, lc.visibility.value!!)
        }
    }
}

@Serializable
data class PhotoContainerDb(
    val pItems: List<PhotoDb>,
    val pVisibility: Boolean = true
) {
    @Serializable
    data class PhotoDb(
        val pFile: String,
        val pDescription: String,
        val pImageWidth: Int,
        val pImageHeight: Int
    ) {
        companion object {
            fun fromReport(p: PhotoData): PhotoDb = PhotoDb(p.file.value!!, p.description.value!!, p.imageWidth.value?:512, p.imageHeight.value?:512)
        }
    }

    companion object {
        fun fromReport(pc: PhotoContainerData): PhotoContainerDb {
            val l = mutableListOf<PhotoDb>()
            for(p in pc.items) {
                l.add(PhotoDb.fromReport(p))
            }
            return PhotoContainerDb(l, pc.visibility.value!!)
        }
    }
}

data class DefaultValuesDb(
    val defaultDriveTime: String,
    val useDefaultDriveTime: Boolean,
    val defaultDistance: Int,
    val useDefaultDistance: Boolean
) {

    companion object {
        fun fromReport(d: DefaultValues) = DefaultValuesDb(d.defaultDriveTime, d.useDefaultDriveTime, d.defaultDistance, d.useDefaultDistance)
    }
}

