package com.stemaker.arbeitsbericht.data.preferences

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stemaker.arbeitsbericht.data.report.ReportData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

/* AbPreferences is the app's settings store. Every default here is what a fresh install
 * behaves like, and the report id counter handed out from here must never repeat a value,
 * because report ids are primary keys in the database.
 *
 * This is an instrumented test rather than a local one: the class stores the SFTP password
 * in EncryptedSharedPreferences, whose master key comes from the AndroidKeyStore. That
 * keystore is a device feature with no JVM or Robolectric provider, and AbPreferences
 * builds it in its constructor, so the class cannot be instantiated off-device at all.
 */
@RunWith(AndroidJUnit4::class)
class AbPreferencesTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        // The SFTP password lives in a separate, encrypted preferences file, which would
        // otherwise carry over between tests in this class.
        context.deleteSharedPreferences("ArbeitsberichtEncryptedPrefs")
    }

    private fun prefs() = AbPreferences(context)

    // ---------------------------------------------------------------- defaults

    @Test
    fun aFreshInstallReportsItselfAsNewlyCreated() {
        assertThat(prefs().isNewlyCreated).isTrue()
    }

    @Test
    fun theDocumentedDefaultsApplyOnAFreshInstall() {
        val p = prefs()

        assertThat(p.employeeName.value).isEmpty()
        assertThat(p.deviceName.value).isEmpty()
        assertThat(p.reportIdPattern.value).isEqualTo("%p-%y-%6c")
        assertThat(p.recvMail.value).isEmpty()
        assertThat(p.useOdfOutput.value).isTrue()
        assertThat(p.useXlsxOutput.value).isFalse()
        assertThat(p.selectOutput.value).isFalse()
        assertThat(p.sFtpPort.value).isEqualTo(22)
        assertThat(p.fontSize.value).isEqualTo(12)
        assertThat(p.crashlyticsEnable.value).isFalse()
        assertThat(p.scalePhotos.value).isFalse()
        assertThat(p.photoResolution.value).isEqualTo(1024)
        assertThat(p.useInlinePdfViewer.value).isTrue()
        assertThat(p.currentId).isEqualTo(1)
        assertThat(p.currentClientId).isEqualTo(1)
        assertThat(p.activeReportId.value).isEqualTo(-1)
        assertThat(p.lumpSums.value).isEmpty()
    }

    @Test
    fun theDefaultReportFilterHidesArchivedReportsOnly() {
        val states = prefs().filterStates.value

        fun isSet(s: ReportData.ReportState) = states and (1 shl s.ordinal) != 0
        assertThat(isSet(ReportData.ReportState.IN_WORK)).isTrue()
        assertThat(isSet(ReportData.ReportState.ON_HOLD)).isTrue()
        assertThat(isSet(ReportData.ReportState.DONE)).isTrue()
        assertThat(isSet(ReportData.ReportState.ARCHIVED)).isFalse()
    }

    @Test
    fun alignmentDefaultsToCentre() {
        val p = prefs()

        assertThat(p.pdfLogoAlignment.value).isEqualTo(AbPreferences.Alignment.CENTER)
        assertThat(p.pdfFooterAlignment.value).isEqualTo(AbPreferences.Alignment.CENTER)
        assertThat(p.pdfLogoWidthPercent.value).isEqualTo(100)
        assertThat(p.pdfFooterWidthPercent.value).isEqualTo(100)
    }

    // ---------------------------------------------------------------- persistence

    @Test
    fun aWrittenStringIsReadBackByANewInstance() {
        prefs().employeeName.value = "Hans Mueller"

        assertThat(prefs().employeeName.value).isEqualTo("Hans Mueller")
    }

    @Test
    fun writtenValuesOfEveryTypeAreReadBackByANewInstance() {
        with(prefs()) {
            deviceName.value = "Tablet"
            reportIdPattern.value = "%p-%4c"
            recvMail.value = "a@b.de"
            useOdfOutput.value = false
            sFtpPort.value = 2222
            fontSize.value = 14
            photoResolution.value = 2048
            lumpSums.value = setOf("Anfahrt", "Kleinmaterial")
            logoRatio.value = 1.75
            pdfLogoAlignment.value = AbPreferences.Alignment.RIGHT
        }

        with(prefs()) {
            assertThat(deviceName.value).isEqualTo("Tablet")
            assertThat(reportIdPattern.value).isEqualTo("%p-%4c")
            assertThat(recvMail.value).isEqualTo("a@b.de")
            assertThat(useOdfOutput.value).isFalse()
            assertThat(sFtpPort.value).isEqualTo(2222)
            assertThat(fontSize.value).isEqualTo(14)
            assertThat(photoResolution.value).isEqualTo(2048)
            assertThat(lumpSums.value).containsExactly("Anfahrt", "Kleinmaterial")
            assertThat(pdfLogoAlignment.value).isEqualTo(AbPreferences.Alignment.RIGHT)
        }
    }

    @Test
    fun theLogoRatioSurvivesTheFloatRoundTripItIsStoredAs() {
        // Stored as a Float but exposed as a Double, so some precision is expected to be lost
        prefs().logoRatio.value = 1.75

        assertThat(prefs().logoRatio.value).isWithin(1e-6).of(1.75)
    }

    @Test
    fun anEmptyLumpSumSetIsReadBackAsEmptyNotAsNull() {
        prefs().lumpSums.value = emptySet()

        assertThat(prefs().lumpSums.value).isEmpty()
    }

    // ---------------------------------------------------------------- id allocation

    @Test
    fun allocateReportIdHandsOutConsecutiveIds() {
        val p = prefs()

        val ids = (1..5).map { p.allocateReportId() }

        assertThat(ids).containsExactly(1, 2, 3, 4, 5).inOrder()
        assertThat(p.currentId).isEqualTo(6)
    }

    @Test
    fun allocatedReportIdsAreNotReusedAfterARestart() {
        // Report ids are primary keys, so handing one out twice would overwrite a report
        val first = prefs().allocateReportId()

        val second = prefs().allocateReportId()

        assertThat(second).isEqualTo(first + 1)
    }

    @Test
    fun overrideCurrentIdMovesTheCounterSoExistingReportsAreNotOverwritten() {
        val p = prefs()
        p.allocateReportId()

        p.overrideCurrentId(100)

        assertThat(p.currentId).isEqualTo(100)
        assertThat(p.allocateReportId()).isEqualTo(100)
        assertThat(p.allocateReportId()).isEqualTo(101)
    }

    @Test
    fun allocateClientIdHandsOutConsecutiveIdsIndependentlyOfReportIds() {
        val p = prefs()
        p.allocateReportId()
        p.allocateReportId()

        assertThat(p.allocateClientId()).isEqualTo(1)
        assertThat(p.allocateClientId()).isEqualTo(2)
        assertThat(p.currentClientId).isEqualTo(3)
    }

    // ---------------------------------------------------------------- version tracking

    @Test
    fun aStoredVersionCodeIsReportedBack() {
        prefs().versionCode.value = 33L

        val p = prefs()
        assertThat(p.versionCode.value).isEqualTo(33L)
        assertThat(p.isNewlyCreated).isFalse()
    }

    // ---------------------------------------------------------------- encrypted store

    @Test
    fun theSftpPasswordIsStoredOutsideThePlainPreferences() {
        // It must not end up in the backed-up SharedPreferences file
        prefs().sFtpPassword.value = "s3cret"

        assertThat(prefs().sFtpPassword.value).isEqualTo("s3cret")
        val plain = PreferenceManager.getDefaultSharedPreferences(context).all
        assertThat(plain.values.map { it.toString() }).doesNotContain("s3cret")
        assertThat(plain.keys).doesNotContain("sFtpPassword")
    }

    @Test
    fun theSftpPasswordDefaultsToEmpty() {
        assertThat(prefs().sFtpPassword.value).isEmpty()
    }
}
