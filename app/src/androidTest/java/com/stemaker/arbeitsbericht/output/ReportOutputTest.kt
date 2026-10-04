package com.stemaker.arbeitsbericht.output

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.print.PdfGenerator
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.MainActivity
// XlsxGenerator sits in the root package even though its file is under output/
import com.stemaker.arbeitsbericht.XlsxGenerator
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.support.UiTestSupport.app
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitAppInitialised
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitUntil
import com.stemaker.arbeitsbericht.support.UiTestSupport.awaitView
import com.stemaker.arbeitsbericht.support.UiTestSupport.dismissVersionDialogIfShown
import com.stemaker.arbeitsbericht.support.UiTestSupport.onMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Calendar
import java.util.GregorianCalendar
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipFile

/* Producing a document is the whole point of the app, and all three writers run on
 * third-party stacks that the SDK upgrade could easily have broken: Apache POI for XLSX,
 * the ODF Toolkit for ODT, and the platform print framework plus a WebView for PDF. These
 * tests generate a real document from a populated report and check what came out.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ReportOutputTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var report: ReportData
    private val generated = mutableListOf<File>()

    @Before
    fun setUp() {
        awaitAppInitialised()
        dismissVersionDialogIfShown()
        awaitView(withId(R.id.report_list_rv))
        report = createPopulatedReport()
    }

    @After
    fun tearDown() {
        generated.forEach { it.delete() }
        onMain { app.reportRepo.deleteReport(report) }
    }

    private fun createPopulatedReport(): ReportData {
        val created = onMain { app.reportRepo.createReportAndActivate(null) }
        awaitUntil("the report to be stored") { app.reportRepo.isReportVisible(created.cnt) }
        onMain {
            created.id.observeForever { }
            created.project.name.value = "Mueller GmbH"
            created.project.extra1.value = "Dachsanierung"
            created.bill.name.value = "Mueller Holding"
            created.bill.street.value = "Hauptstr. 1"
            created.bill.zip.value = "12345"
            created.bill.city.value = "Musterstadt"
            created.workTimeContainer.addWorkTime().apply {
                workDuration.observeForever { }
                date.value = GregorianCalendar(2024, Calendar.MARCH, 12)
                startTime.value = "08:00"
                endTime.value = "16:30"
                pauseDuration.value = "00:30"
                driveTime.value = "01:15"
                distance.value = 42
            }
            created.workItemContainer.addWorkItem().item.value = "Ziegel ersetzt"
            created.materialContainer.addMaterial().apply {
                item.value = "Dachziegel"
                amount.value = 12.5f
                unit.value = "Stück"
            }
            created.lumpSumContainer.addLumpSum().apply {
                item.value = "Anfahrtspauschale"
                amount.value = 2
                comment.value = "zwei Fahrten"
            }
        }
        return created
    }

    private fun activity(): Activity {
        var result: Activity? = null
        activityRule.scenario.onActivity { result = it }
        awaitUntil("the activity to be handed over") { result != null }
        return result!!
    }

    /* Mirrors ReportGenerator.getFilesForGeneration(), which creates every target file
     * before handing the array to createDoc. PdfGenerator opens its target with
     * ParcelFileDescriptor.MODE_READ_WRITE, which does not create the file, and
     * XlsxGenerator reads the signature files with FileInputStream, so they all have to
     * exist up front.
     */
    private fun outputFile(name: String): File {
        val dir = File("${activity().filesDir.canonicalPath}/Arbeitsbericht").apply { mkdirs() }
        return File(dir, name).also {
            it.delete()
            it.createNewFile()
            generated.add(it)
        }
    }

    /* SummaryActivity renders the signature pads into these before generating, and an empty
     * pad still produces a white image, so the generators always see a real PNG.
     */
    private fun signatureFile(name: String): File = outputFile(name).also { file ->
        val bitmap = Bitmap.createBitmap(120, 40, Bitmap.Config.RGB_565)
        bitmap.eraseColor(Color.WHITE)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 85, it) }
    }

    /* Runs a generator's createDoc and waits for its completion callback. */
    private fun generate(
        files: Array<File>,
        onMainThread: Boolean,
        build: (Activity) -> ReportGenerator,
    ): Boolean {
        val done = AtomicBoolean(false)
        val success = AtomicBoolean(false)
        val act = activity()
        val run = {
            build(act).createDoc(files) { ok ->
                success.set(ok)
                done.set(true)
            }
        }
        if (onMainThread) onMain { run() } else run()
        awaitUntil("document generation to finish", timeoutMs = 90_000) { done.get() }
        return success.get()
    }

    @Test
    fun anXlsxWorkbookIsGeneratedFromAPopulatedReport() {
        val xlsx = outputFile("test_report.xlsx")
        val clientSig = signatureFile("test_client_sig.png")
        val employeeSig = signatureFile("test_employee_sig.png")

        val ok = generate(arrayOf(xlsx, clientSig, employeeSig), onMainThread = false) { act ->
            XlsxGenerator(act, report, app.prefs, null, null)
        }

        assertThat(ok).isTrue()
        assertThat(xlsx.exists()).isTrue()
        assertThat(xlsx.length()).isGreaterThan(0L)
        // XLSX is a zip; confirm it is readable and carries the sheets the generator makes
        ZipFile(xlsx).use { zip ->
            val names = zip.entries().toList().map { it.name }
            assertThat(names).contains("[Content_Types].xml")
            assertThat(names.any { it.startsWith("xl/worksheets/") }).isTrue()
        }
    }

    @Test
    fun anOdfTextDocumentIsGeneratedFromAPopulatedReport() {
        val odt = outputFile("test_report.odt")
        val clientSig = signatureFile("test_client_sig.png")
        val employeeSig = signatureFile("test_employee_sig.png")

        val ok = generate(arrayOf(odt, clientSig, employeeSig), onMainThread = false) { act ->
            OdfGenerator(act, report, app.prefs, null, null)
        }

        assertThat(ok).isTrue()
        assertThat(odt.exists()).isTrue()
        assertThat(odt.length()).isGreaterThan(0L)
        ZipFile(odt).use { zip ->
            val names = zip.entries().toList().map { it.name }
            assertThat(names).contains("content.xml")
            assertThat(names).contains("mimetype")
            val content = zip.getInputStream(zip.getEntry("content.xml")).reader().readText()
            // The report data must have made it into the document body
            assertThat(content).contains("Mueller GmbH")
        }
    }

    @Test
    fun aPdfIsGeneratedFromAPopulatedReport() {
        val pdf = outputFile("test_report.pdf")

        // PdfGenerator drives a WebView and the print framework, both main-thread only
        val ok = generate(arrayOf(pdf), onMainThread = true) { act ->
            PdfGenerator(act, report, app.prefs, null, null)
        }

        assertThat(ok).isTrue()
        assertThat(pdf.exists()).isTrue()
        assertThat(pdf.length()).isGreaterThan(0L)
        assertThat(pdf.readBytes().take(4).toByteArray().decodeToString()).isEqualTo("%PDF")
    }

    /* A generator that answers immediately with whatever it is told to answer. */
    private inner class StubGenerator(
        act: Activity,
        private val result: Boolean,
    ) : ReportGenerator(act, report, app.prefs, null, null, renderInUiThread = true) {
        override val filePostFixExt = arrayOf(Pair("", "stub"))
        override fun getHash(files: Array<File>): String? = null
        override fun createDoc(files: Array<File>, done: (success: Boolean) -> Unit) =
            done(result)
    }

    @Test
    fun aFailedGenerationIsReportedAsAFailure() {
        /* create() used to return a hardcoded true on the render-in-UI-thread path, so a
         * failed PDF was reported as a success and the caller went on to share an empty
         * file. The same path also resumed a continuation that had not been assigned yet,
         * which a synchronous failure like this one triggers.
         */
        val files = arrayOf(outputFile("test_stub_fail.stub"))

        var result: Boolean? = null
        onMain {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result = StubGenerator(activity(), result = false).create(files)
            }
        }
        awaitUntil("the generator to report back") { result != null }

        assertThat(result).isFalse()
    }

    @Test
    fun aSuccessfulGenerationIsReportedAsASuccess() {
        val files = arrayOf(outputFile("test_stub_ok.stub"))

        var result: Boolean? = null
        onMain {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                result = StubGenerator(activity(), result = true).create(files)
            }
        }
        awaitUntil("the generator to report back") { result != null }

        assertThat(result).isTrue()
    }

    @Test
    fun anEmptyReportStillProducesAValidWorkbook() {
        val empty = onMain { app.reportRepo.createReportAndActivate(null) }
        awaitUntil("the empty report to be stored") { app.reportRepo.isReportVisible(empty.cnt) }
        onMain { empty.id.observeForever { } }
        val xlsx = outputFile("test_empty.xlsx")
        val clientSig = signatureFile("test_empty_client_sig.png")
        val employeeSig = signatureFile("test_empty_employee_sig.png")

        val ok = generate(arrayOf(xlsx, clientSig, employeeSig), onMainThread = false) { act ->
            XlsxGenerator(act, empty, app.prefs, null, null)
        }

        assertThat(ok).isTrue()
        assertThat(xlsx.length()).isGreaterThan(0L)
        onMain { app.reportRepo.deleteReport(empty) }
    }
}
