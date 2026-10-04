package com.stemaker.arbeitsbericht.helpers

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.support.FakePreferences
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.Calendar
import java.util.GregorianCalendar

/* HtmlReport produces both the preview shown on the summary screen and the source the PDF
 * is printed from, so it is what the customer actually ends up reading.
 */
@RunWith(RobolectricTestRunner::class)
class HtmlReportTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var prefs: FakePreferences
    private lateinit var report: ReportData
    private lateinit var dir: File

    @Before
    fun setUp() {
        prefs = FakePreferences()
        prefs.employeeName.value = "Hans"
        report = ReportData.createReport(7, prefs.mock)
        report.id.observeForever { }
        dir = File(System.getProperty("java.io.tmpdir"), "htmlreport-test").apply { mkdirs() }
    }

    private fun html(inclSignatures: Boolean = true) =
        HtmlReport(prefs.mock, report, dir).encodeReport(inclSignatures)

    // ---------------------------------------------------------------- structure

    @Test
    fun theDocumentIsWellFormedHtml() {
        val out = html()

        assertThat(out).startsWith("<!DOCTYPE html>")
        assertThat(out).endsWith("</body></html>")
        assertThat(out).contains("<html lang=\"de\">")
        assertThat(out).contains("charset=utf-8")
    }

    @Test
    fun theReportNumberIsTheHeadline() {
        report.project.name.value = "Mueller"

        assertThat(html()).contains("<h1>Arbeitsbericht Nr. ${report.id.value}</h1>")
    }

    @Test
    fun theConfiguredFontSizeIsApplied() {
        prefs.fontSize.value = 18

        assertThat(html()).contains("font-size:18px")
    }

    @Test
    fun theProjectAndBillAddressAreAlwaysIncluded() {
        report.project.name.value = "Mueller GmbH"
        report.project.extra1.value = "Dachsanierung"
        report.bill.name.value = "Mueller Holding"
        report.bill.street.value = "Hauptstr. 1"
        report.bill.zip.value = "12345"
        report.bill.city.value = "Musterstadt"

        val out = html()

        assertThat(out).contains("Kunde / Projekt")
        assertThat(out).contains("Rechnungsadresse")
        assertThat(out).contains("Mueller GmbH")
        assertThat(out).contains("Dachsanierung")
        assertThat(out).contains("Mueller Holding")
        assertThat(out).contains("Hauptstr. 1")
        assertThat(out).contains("12345 Musterstadt")
    }

    // ---------------------------------------------------------------- sections

    @Test
    fun anEmptyReportHasNoSectionTablesAtAll() {
        val out = html()

        assertThat(out).doesNotContain("Arbeits-/fahrzeiten")
        assertThat(out).doesNotContain("Durchgeführte Arbeiten")
        assertThat(out).doesNotContain("<h2>Pauschalen</h2>")
        assertThat(out).doesNotContain("<h2>Material</h2>")
        assertThat(out).doesNotContain("<h2>Fotos</h2>")
    }

    @Test
    fun theWorkTimeSectionListsEveryEntryWithItsCalculatedDuration() {
        report.workTimeContainer.addWorkTime().apply {
            workDuration.observeForever { }
            date.value = GregorianCalendar(2024, Calendar.MARCH, 12)
            startTime.value = "08:00"
            endTime.value = "16:30"
            pauseDuration.value = "00:30"
            driveTime.value = "01:15"
            distance.value = 42
        }

        val out = html()

        assertThat(out).contains("Arbeits-/fahrzeiten und Fahrstrecken")
        assertThat(out).contains("12.03.2024")
        assertThat(out).contains("08:00 Uhr")
        assertThat(out).contains("16:30 Uhr")
        assertThat(out).contains("01:15")
        assertThat(out).contains("42")
        // start to end minus the pause
        assertThat(out).contains("08:00")
    }

    @Test
    fun everyEmployeeOnAWorkTimeIsListed() {
        report.workTimeContainer.addWorkTime().apply {
            workDuration.observeForever { }
            addEmployee().value = "Grete"
        }

        val out = html()

        assertThat(out).contains("Hans<br>")
        assertThat(out).contains("Grete<br>")
    }

    @Test
    fun theWorkItemSectionListsEveryItem() {
        report.workItemContainer.addWorkItem().item.value = "Ziegel ersetzt"
        report.workItemContainer.addWorkItem().item.value = "Rinne gereinigt"

        val out = html()

        assertThat(out).contains("Durchgeführte Arbeiten")
        assertThat(out).contains("Ziegel ersetzt")
        assertThat(out).contains("Rinne gereinigt")
    }

    @Test
    fun theLumpSumSectionListsItemCountAndComment() {
        report.lumpSumContainer.addLumpSum().apply {
            item.value = "Anfahrtspauschale"
            amount.value = 3
            comment.value = "drei Fahrten"
        }

        val out = html()

        assertThat(out).contains("<h2>Pauschalen</h2>")
        assertThat(out).contains("Anfahrtspauschale")
        assertThat(out).contains("3")
        assertThat(out).contains("drei Fahrten")
    }

    @Test
    fun theMaterialSectionOmitsTheUnitColumnWhenNoUnitIsSet() {
        report.materialContainer.addMaterial().apply {
            item.value = "Dachziegel"
            amount.value = 12f
            unit.value = ""
        }

        val out = html()

        assertThat(out).contains("<h2>Material</h2>")
        assertThat(out).contains("Dachziegel")
        assertThat(out).doesNotContain("Einheit")
    }

    @Test
    fun theMaterialSectionAddsTheUnitColumnAsSoonAsOneUnitIsSet() {
        report.materialContainer.addMaterial().apply {
            item.value = "Dachziegel"
            amount.value = 12f
            unit.value = "Stück"
        }
        report.materialContainer.addMaterial().apply {
            item.value = "Dichtmasse"
            amount.value = 2f
            unit.value = ""
        }

        val out = html()

        assertThat(out).contains("Einheit")
        assertThat(out).contains("Stück")
        assertThat(out).contains("Dichtmasse")
    }

    @Test
    fun photosAreReferencedThroughTheFileProvider() {
        report.photoContainer.addPhoto().apply {
            file.value = "Arbeitsbericht_20240312_120000.jpg"
            description.value = "Dach vorher"
        }

        val out = html()

        assertThat(out).contains("<h2>Fotos</h2>")
        assertThat(out).contains(
            "content://com.stemaker.arbeitsbericht.fileprovider/ArbeitsberichtPhotos/" +
                "Arbeitsbericht_20240312_120000.jpg")
        assertThat(out).contains("Dach vorher")
    }

    @Test
    fun aPhotoStoredAsAFullPathByAnOlderVersionIsReducedToItsFileName() {
        report.photoContainer.addPhoto().file.value =
            "/storage/emulated/0/Android/data/com.stemaker.arbeitsbericht/files/Pictures/old.jpg"

        val out = html()

        assertThat(out).contains("ArbeitsberichtPhotos/old.jpg")
        assertThat(out).doesNotContain("/storage/emulated")
    }

    // ---------------------------------------------------------------- signatures

    @Test
    fun theSignatureSectionSaysSoWhenThereAreNoSignatures() {
        val out = html(inclSignatures = true)

        assertThat(out).contains("<h2>Unterschriften</h2>")
        assertThat(out).contains("Keine Unterschriften vorhanden")
    }

    @Test
    fun theSignatureSectionIsLeftOutEntirelyWhenNotRequested() {
        // This is the variant used for the mail body
        val out = html(inclSignatures = false)

        assertThat(out).doesNotContain("Unterschriften")
    }

    @Test
    fun aSignatureIsReferencedThroughTheFileProviderOnceItsPngExists() {
        report.signatureData.employeeSignatureSvg.value = "<svg/>"
        report.signatureData.employeeSignaturePngFile = File(dir, "emp_sig.png")
        report.signatureData.clientSignatureSvg.value = "<svg/>"
        report.signatureData.clientSignaturePngFile = File(dir, "client_sig.png")

        val out = html(inclSignatures = true)

        assertThat(out).contains("Auftragnehmer")
        assertThat(out).contains("Auftraggeber")
        assertThat(out).contains("ArbeitsberichtSignatures/emp_sig.png")
        assertThat(out).contains("ArbeitsberichtSignatures/client_sig.png")
        assertThat(out).doesNotContain("Keine Unterschriften vorhanden")
    }

    @Test
    fun aSignatureWithoutARenderedPngFallsBackToAPlaceholder() {
        report.signatureData.employeeSignatureSvg.value = "<svg/>"
        report.signatureData.employeeSignaturePngFile = null

        val out = html(inclSignatures = true)

        assertThat(out).contains("Keine Unterschrift")
    }

    // ---------------------------------------------------------------- logo and footer

    @Test
    fun theLogoIsLeftOutWhenItIsSwitchedOff() {
        prefs.logoFile.value = "logo.jpg"
        prefs.pdfUseLogo.value = false

        assertThat(html()).doesNotContain("data:image/jpg;base64")
    }

    @Test
    fun theLogoIsEmbeddedAsBase64WhenItIsSwitchedOn() {
        File(dir, "logo.jpg").writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        prefs.logoFile.value = "logo.jpg"
        prefs.pdfUseLogo.value = true

        val out = html()

        assertThat(out).contains("data:image/jpg;base64,")
        assertThat(out).contains("class=\"logo\"")
    }

    @Test
    fun theFooterIsEmbeddedAsBase64WhenItIsSwitchedOn() {
        File(dir, "footer.jpg").writeBytes(byteArrayOf(9, 8, 7))
        prefs.footerFile.value = "footer.jpg"
        prefs.pdfUseFooter.value = true

        val out = html()

        assertThat(out).contains("class=\"footer\"")
    }

    @Test
    fun theLogoAlignmentPreferenceReachesTheCss() {
        prefs.pdfLogoAlignment.value = AbPreferences.Alignment.RIGHT
        prefs.pdfLogoWidthPercent.value = 60

        val out = html()

        assertThat(out).contains("margin-left:auto;display:block;")
        assertThat(out).contains("width: 60%")
    }

    @Test
    fun eachAlignmentProducesItsOwnCss() {
        prefs.pdfFooterAlignment.value = AbPreferences.Alignment.CENTER
        assertThat(html()).contains("margin-left:auto;margin-right:auto;display:block;")

        prefs.pdfFooterAlignment.value = AbPreferences.Alignment.LEFT
        assertThat(html()).contains("margin-right:auto;display:block;")
    }

    // ---------------------------------------------------------------- escaping

    @Test
    fun textContainingHtmlMarkupIsEmittedUnescaped() {
        /* Documents a real shortcoming: values are interpolated straight into the markup
         * with no escaping, so a customer name containing < or & corrupts the surrounding
         * document. It cannot be exploited from outside - the only writer is the device's
         * own user - but it does produce a broken report. Fixing it means escaping on
         * output, which changes every generated document and so is left as its own change.
         */
        report.project.name.value = "Mueller & Co <GmbH>"

        val out = html()

        assertThat(out).contains("Mueller & Co <GmbH>")
        assertThat(out).doesNotContain("Mueller &amp; Co &lt;GmbH&gt;")
    }
}
