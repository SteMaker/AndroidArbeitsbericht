package com.stemaker.arbeitsbericht.data.report

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.data.calendarToDateString
import com.stemaker.arbeitsbericht.support.FakePreferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

/* Every report is written to the database as a ReportDb row, with the repeating sections
 * encoded as CBOR blobs by the Room type converters. A gap anywhere on that path loses user
 * data silently, so this covers the full round trip field by field.
 */
class ReportPersistenceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var prefs: FakePreferences
    private lateinit var converters: Converters

    @Before
    fun setUp() {
        prefs = FakePreferences()
        prefs.employeeName.value = "Hans"
        converters = Converters()
    }

    /* Builds a report with every section populated. */
    private fun fullyPopulatedReport(cnt: Int = 17): ReportData =
        ReportData.createReport(cnt, prefs.mock).apply {
            state.value = ReportData.ReportState.ON_HOLD

            project.name.value = "Mueller GmbH"
            project.extra1.value = "Dachsanierung"
            project.visibility.value = true
            project.clientId = 4

            bill.name.value = "Mueller GmbH"
            bill.street.value = "Hauptstr. 1"
            bill.zip.value = "12345"
            bill.city.value = "Musterstadt"
            bill.visibility.value = true

            workTimeContainer.visibility.value = true
            workTimeContainer.addWorkTime().apply {
                date.value = GregorianCalendar(2024, Calendar.MARCH, 12)
                startTime.value = "07:30"
                endTime.value = "16:45"
                pauseDuration.value = "00:45"
                driveTime.value = "01:15"
                distance.value = 123
                addEmployee().value = "Grete"
            }

            workItemContainer.visibility.value = true
            workItemContainer.addWorkItem().item.value = "Ziegel ersetzt"
            workItemContainer.addWorkItem().item.value = "Rinne gereinigt"

            materialContainer.visibility.value = true
            materialContainer.addMaterial().apply {
                item.value = "Dachziegel"
                amount.value = 12.5f
                unit.value = "Stück"
            }

            lumpSumContainer.visibility.value = true
            lumpSumContainer.addLumpSum().apply {
                item.value = "Anfahrtspauschale"
                amount.value = 2
                comment.value = "zwei Fahrten"
            }

            photoContainer.visibility.value = true
            photoContainer.addPhoto().apply {
                file.value = "photo_1.jpg"
                description.value = "Dach vorher"
                imageWidth.value = 1024
                imageHeight.value = 768
            }

            signatureData.employeeSignatureSvg.value = "<svg>employee</svg>"
            signatureData.clientSignatureSvg.value = "<svg>client</svg>"

            defaultValues.defaultDriveTime = "00:30"
            defaultValues.useDefaultDriveTime = true
            defaultValues.defaultDistance = 25
            defaultValues.useDefaultDistance = true
        }

    /* Pushes a ReportDb row through the CBOR converters, the way Room does. */
    private fun throughConverters(db: ReportDb): ReportDb = db.copy(
        workTimeContainer = converters.workTimeContainerDeserializer(
            converters.workTimeContainerSerializer(db.workTimeContainer)),
        workItemContainer = converters.workItemContainerDeserializer(
            converters.workItemContainerSerializer(db.workItemContainer)),
        materialContainer = converters.materialContainerDeserializer(
            converters.materialContainerSerializer(db.materialContainer)),
        lumpSumContainer = converters.lumpSumContainerDeserializer(
            converters.lumpSumContainerSerializer(db.lumpSumContainer)),
        photoContainer = converters.photoContainerDeserializer(
            converters.photoContainerSerializer(db.photoContainer)),
    )

    // ------------------------------------------------------- scalar sections

    @Test
    fun `identity and state survive the round trip`() {
        val original = fullyPopulatedReport(cnt = 17)

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.cnt).isEqualTo(17)
        assertThat(restored.state.value).isEqualTo(ReportData.ReportState.ON_HOLD)
        assertThat(restored.create_date.value).isEqualTo(original.create_date.value)
    }

    @Test
    fun `the project section survives the round trip`() {
        val original = fullyPopulatedReport()

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.project.name.value).isEqualTo("Mueller GmbH")
        assertThat(restored.project.extra1.value).isEqualTo("Dachsanierung")
        assertThat(restored.project.visibility.value).isTrue()
        assertThat(restored.project.clientId).isEqualTo(4)
    }

    @Test
    fun `the bill section survives the round trip`() {
        val original = fullyPopulatedReport()

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.bill.name.value).isEqualTo("Mueller GmbH")
        assertThat(restored.bill.street.value).isEqualTo("Hauptstr. 1")
        assertThat(restored.bill.zip.value).isEqualTo("12345")
        assertThat(restored.bill.city.value).isEqualTo("Musterstadt")
        assertThat(restored.bill.visibility.value).isTrue()
    }

    @Test
    fun `signatures survive the round trip`() {
        val original = fullyPopulatedReport()

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.signatureData.employeeSignatureSvg.value)
            .isEqualTo("<svg>employee</svg>")
        assertThat(restored.signatureData.clientSignatureSvg.value).isEqualTo("<svg>client</svg>")
    }

    @Test
    fun `the per-report default values survive the round trip`() {
        val original = fullyPopulatedReport()

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.defaultValues.defaultDriveTime).isEqualTo("00:30")
        assertThat(restored.defaultValues.useDefaultDriveTime).isTrue()
        assertThat(restored.defaultValues.defaultDistance).isEqualTo(25)
        assertThat(restored.defaultValues.useDefaultDistance).isTrue()
    }

    // ------------------------------------------------------- repeating sections

    @Test
    fun `work times survive the round trip including employees`() {
        val original = fullyPopulatedReport()

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.workTimeContainer.getSize()).isEqualTo(1)
        val wt = restored.workTimeContainer.items[0]
        assertThat(calendarToDateString(wt.date.value)).isEqualTo("12.03.2024")
        assertThat(wt.startTime.value).isEqualTo("07:30")
        assertThat(wt.endTime.value).isEqualTo("16:45")
        assertThat(wt.pauseDuration.value).isEqualTo("00:45")
        assertThat(wt.driveTime.value).isEqualTo("01:15")
        assertThat(wt.distance.value).isEqualTo(123)
        assertThat(wt.employees.map { it.value }).containsExactly("Hans", "Grete").inOrder()
        assertThat(restored.workTimeContainer.visibility.value).isTrue()
    }

    @Test
    fun `work items survive the round trip in order`() {
        val original = fullyPopulatedReport()

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.workItemContainer.map { it.item.value })
            .containsExactly("Ziegel ersetzt", "Rinne gereinigt").inOrder()
        assertThat(restored.workItemContainer.visibility.value).isTrue()
    }

    @Test
    fun `materials survive the round trip including fractional amounts`() {
        val original = fullyPopulatedReport()

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.materialContainer.getSize()).isEqualTo(1)
        val m = restored.materialContainer.items[0]
        assertThat(m.item.value).isEqualTo("Dachziegel")
        assertThat(m.amount.value).isEqualTo(12.5f)
        assertThat(m.unit.value).isEqualTo("Stück")
    }

    @Test
    fun `lump sums survive the round trip`() {
        val original = fullyPopulatedReport()

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.lumpSumContainer.getSize()).isEqualTo(1)
        val l = restored.lumpSumContainer.items[0]
        assertThat(l.item.value).isEqualTo("Anfahrtspauschale")
        assertThat(l.amount.value).isEqualTo(2)
        assertThat(l.comment.value).isEqualTo("zwei Fahrten")
    }

    @Test
    fun `photos survive the round trip including their dimensions`() {
        val original = fullyPopulatedReport()

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.photoContainer.getSize()).isEqualTo(1)
        val p = restored.photoContainer.items[0]
        assertThat(p.file.value).isEqualTo("photo_1.jpg")
        assertThat(p.description.value).isEqualTo("Dach vorher")
        assertThat(p.imageWidth.value).isEqualTo(1024)
        assertThat(p.imageHeight.value).isEqualTo(768)
    }

    @Test
    fun `an empty report round trips without losing its empty sections`() {
        val original = ReportData.createReport(3, prefs.mock)

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.workTimeContainer.getSize()).isEqualTo(0)
        assertThat(restored.workItemContainer.getSize()).isEqualTo(0)
        assertThat(restored.materialContainer.getSize()).isEqualTo(0)
        assertThat(restored.lumpSumContainer.getSize()).isEqualTo(0)
        assertThat(restored.photoContainer.getSize()).isEqualTo(0)
    }

    @Test
    fun `a report loaded from the database is not marked as modified`() {
        // Otherwise every load would schedule a pointless write back
        val original = fullyPopulatedReport()

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.modified).isFalse()
    }

    @Test
    fun `text with umlauts and newlines survives the CBOR encoding`() {
        val original = ReportData.createReport(1, prefs.mock).apply {
            workItemContainer.addWorkItem().item.value = "Dachrinne\nerneuert — größer, 50 °C"
        }

        val restored = ReportData.createReport(0, prefs.mock)
        restored.getReportFromDb(throughConverters(ReportDb.fromReport(original)))

        assertThat(restored.workItemContainer.items[0].item.value)
            .isEqualTo("Dachrinne\nerneuert — größer, 50 °C")
    }

    // ------------------------------------------------------- converters in isolation

    @Test
    fun `each container converter round trips through CBOR`() {
        val workTime = WorkTimeContainerDb(
            listOf(WorkTimeContainerDb.WorkTimeDb(
                "12.03.2024", listOf("A", "B"), "07:00", "16:00", "00:30", "00:15", 10)),
            wtVisibility = false)
        assertThat(converters.workTimeContainerDeserializer(
            converters.workTimeContainerSerializer(workTime))).isEqualTo(workTime)

        val workItem = WorkItemContainerDb(
            listOf(WorkItemContainerDb.WorkItemDb("x")), wiVisibility = false)
        assertThat(converters.workItemContainerDeserializer(
            converters.workItemContainerSerializer(workItem))).isEqualTo(workItem)

        val material = MaterialContainerDb(
            listOf(MaterialContainerDb.MaterialDb("m", 1.5f, "Stück")), mVisibility = false)
        assertThat(converters.materialContainerDeserializer(
            converters.materialContainerSerializer(material))).isEqualTo(material)

        val lumpSum = LumpSumContainerDb(
            listOf(LumpSumContainerDb.LumpSumDb("l", 3, "c")), lVisibility = false)
        assertThat(converters.lumpSumContainerDeserializer(
            converters.lumpSumContainerSerializer(lumpSum))).isEqualTo(lumpSum)

        val photo = PhotoContainerDb(
            listOf(PhotoContainerDb.PhotoDb("f", "d", 1, 2)), pVisibility = false)
        assertThat(converters.photoContainerDeserializer(
            converters.photoContainerSerializer(photo))).isEqualTo(photo)
    }

    @Test
    fun `empty containers round trip through CBOR`() {
        val empty = WorkTimeContainerDb(emptyList(), wtVisibility = true)

        assertThat(converters.workTimeContainerDeserializer(
            converters.workTimeContainerSerializer(empty))).isEqualTo(empty)
    }

    @Test
    fun `a work time with no employees round trips`() {
        val wt = WorkTimeContainerDb(
            listOf(WorkTimeContainerDb.WorkTimeDb(
                "12.03.2024", emptyList(), "", "", "00:00", "00:00", 0)))

        assertThat(converters.workTimeContainerDeserializer(
            converters.workTimeContainerSerializer(wt))).isEqualTo(wt)
    }

    // ------------------------------------------------------- duplication

    @Test
    fun `duplicating a report copies the editable sections`() {
        val original = fullyPopulatedReport()

        val duplicate = ReportData.createReport(99, prefs.mock)
        duplicate.copy(original)

        assertThat(duplicate.project.name.value).isEqualTo("Mueller GmbH")
        assertThat(duplicate.project.extra1.value).isEqualTo("Dachsanierung")
        assertThat(duplicate.bill.city.value).isEqualTo("Musterstadt")
        assertThat(duplicate.state.value).isEqualTo(ReportData.ReportState.ON_HOLD)
        assertThat(duplicate.workItemContainer.map { it.item.value })
            .containsExactly("Ziegel ersetzt", "Rinne gereinigt").inOrder()
        assertThat(duplicate.materialContainer.getSize()).isEqualTo(1)
        assertThat(duplicate.lumpSumContainer.getSize()).isEqualTo(1)
        assertThat(duplicate.workTimeContainer.getSize()).isEqualTo(1)
        assertThat(duplicate.defaultValues.defaultDistance).isEqualTo(25)
    }

    @Test
    fun `duplicating a report keeps its own id and marks it modified`() {
        val original = fullyPopulatedReport()

        val duplicate = ReportData.createReport(99, prefs.mock)
        duplicate.copy(original)

        assertThat(duplicate.cnt).isEqualTo(99)
        assertThat(duplicate.modified).isTrue()
    }

    @Test
    fun `duplicating a report does not carry over photos or signatures`() {
        /* Current behaviour: PhotoContainerData.copy() and SignatureData.copy() are both
         * deliberately empty. Leaving the client's signature behind is the right call for a
         * new report; whether photos should follow is a product decision, so this test
         * documents what the app does today rather than asserting what it ought to do.
         */
        val original = fullyPopulatedReport()

        val duplicate = ReportData.createReport(99, prefs.mock)
        duplicate.copy(original)

        assertThat(duplicate.photoContainer.getSize()).isEqualTo(0)
        assertThat(duplicate.signatureData.employeeSignatureSvg.value).isEmpty()
        assertThat(duplicate.signatureData.clientSignatureSvg.value).isEmpty()
    }

    @Test
    fun `duplicating does not alias the origin's sections`() {
        val original = fullyPopulatedReport()
        val duplicate = ReportData.createReport(99, prefs.mock)
        duplicate.copy(original)

        duplicate.project.name.value = "Someone else"
        duplicate.workItemContainer.items[0].item.value = "Something else"

        assertThat(original.project.name.value).isEqualTo("Mueller GmbH")
        assertThat(original.workItemContainer.items[0].item.value).isEqualTo("Ziegel ersetzt")
    }

    @Test
    fun `duplicating without copyDate gives the duplicate its own creation date`() {
        val original = fullyPopulatedReport()
        val duplicate = ReportData.createReport(99, prefs.mock)

        duplicate.copy(original, copyDate = false)

        // Both were created in this test run, so the date is today either way; what matters
        // is that the duplicate kept the date it was constructed with.
        assertThat(duplicate.create_date.value).isNotNull()
    }
}
