package com.stemaker.arbeitsbericht.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Paint.Align
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.configuration.Configuration
import com.stemaker.arbeitsbericht.data.report.ReportData
import javax.crypto.SecretKey

const val TAG = "AbPreferences"
private const val encryptedSharedPrefsFile = "ArbeitsberichtEncryptedPrefs"
private const val VERSIONCODEDEFAULT: Long = 0
private const val EMPLOYEENAMEDEFAULT = ""
private const val CURRENTIDDEFAULT = 1
private const val DEVICENAMEDEFAULT = ""
private const val REPORTIDPATTERNDEFAULT = "%p-%y-%6c"
private const val RECVMAILDEFAULT = ""
private const val USEODFOUTPUTDEFAULT = true
private const val USEXLSXOUTPUTDEFAULT = false
private const val SELECTOUTPUTDEFAULT = false
private const val SFTPHOSTDEFAULT = ""
private const val SFTPPORTDEFAULT = 22
private const val SFTPUSERDEFAULT = ""
private const val LUMPSUMSERVERPATHDEFAULT = ""
private const val ODFTEMPLATESERVERPATHDEFAULT = ""
private const val LOGOSERVERPATHDEFAULT = ""
private const val FOOTERSERVERPATHDEFAULT = ""
private val LUMPSUMSDEFAULT = setOf<String>()
private const val ACTIVEREPORTIDDEFAULT = -1
private val WORKITEMDICTIONARYDEFAULT = setOf<String>()
private val MATERIALDICTIONARYDEFAULT = setOf<String>()
private const val SFTPPASSWORDDEFAULT = ""
private const val ODFTEMPLATEFILEDEFAULT = ""
private const val LOGOFILEDEFAULT = ""
private const val LOGORATIODEFAULT: Float = 1.0F
private const val FOOTERFILEDEFAULT = ""
private const val FOOTERRATIODEFAULT: Float = 1.0F
private const val FONTSIZEDEFAULT = 12
private const val CRASHLYTICSENABLEDDEFAULT = false
private const val PDFUSELOGODEFAULT = false
private const val PDFUSEFOOTERDEFAULT = false
private const val XLSXUSELOGODEFAULT = false
private const val XLSXLOGOWIDTHDEFAULT = 135 // mm
private const val XLSXUSEFOOTERDEFAULT = false
private const val XLSXFOOTERWIDTHDEFAULT = 135 // mm
private const val SCALEPHOTOSDEFAULT = false
private const val PHOTORESOLUTIONDEFAULT = 1024
private const val CURRENTCLIENTIDDEFAULT = 1
private const val USEINLINEPDFVIEWERDEFAULT = true
private const val FILTERPROJECTNAMEDEFAULT = ""
private const val FILTERPROJECTEXTRADEFAULT = ""
private val FILTERSTATESDEFAULT: Int = (1 shl ReportData.ReportState.toInt(ReportData.ReportState.IN_WORK)) or
        (1 shl ReportData.ReportState.toInt(ReportData.ReportState.ON_HOLD)) or
        (1 shl ReportData.ReportState.toInt(ReportData.ReportState.DONE))
private const val LOCKSCREENORIENTATIONDEFAULT = false
private const val LOCKSCREENORIENTATIONNOINFODEFAULT = false
private const val PDFLOGOWIDTHPERCENTDEFAULT = 100
private const val PDFLOGOALIGNMENTDEFAULT = 0
private const val PDFFOOTERWIDTHPERCENTDEFAULT = 100
private const val PDFFOOTERALIGNMENTDEFAULT = 0

class ConfigElement<T>(initFct: (() -> T), val setFct: ((T) -> Unit)):
    LiveData<T>()
{
    public override fun postValue(value: T) {
        super.postValue(value)
        setFct(value)
    }

    public override fun setValue(value: T) {
        super.setValue(value)
        setFct(value)
    }

    init {
        super.setValue(initFct())
    }
}
// Note: It is OK to use non-null assertion on the value of ConfigElement since they are set in init (.value!!)
class AbPreferences(val ctx: Context) {
    enum class Alignment(val a: Int) {
        CENTER(0),
        LEFT(1),
        RIGHT(2)
    }
    private val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    private val masterKey = MasterKey.Builder(ctx)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build();

    private val encryptedSharedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        ctx, encryptedSharedPrefsFile, masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

    val versionCode = ConfigElement<Long>(
        { sharedPrefs.getLong(ctx.getString(R.string.versionCode), VERSIONCODEDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putLong(ctx.getString(R.string.versionCode), value)
            apply()
        }})
    val previousVersionCode = sharedPrefs.getLong(ctx.getString(R.string.versionCode), VERSIONCODEDEFAULT)

    var employeeName = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.employeeName), EMPLOYEENAMEDEFAULT)?:EMPLOYEENAMEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.employeeName), value)
            apply()
        } })
    var currentId: Int
        get() = sharedPrefs.getInt(ctx.getString(R.string.currentId), CURRENTIDDEFAULT)
        private set(value) {
            with(sharedPrefs.edit()) {
                putInt(ctx.getString(R.string.currentId), value)
                apply()
            }
        }
    fun allocateReportId() : Int {
        if(Looper.getMainLooper().thread != Thread.currentThread()) {
            Log.e(TAG, "Error: Allocating ID, not running on the main thread")
        }
        val id = currentId
        currentId = id + 1
        return id
    }
    var deviceName = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.deviceName), DEVICENAMEDEFAULT)?:DEVICENAMEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.deviceName), value)
            apply()
        } })
    var reportIdPattern = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.reportIdPattern), REPORTIDPATTERNDEFAULT)?: REPORTIDPATTERNDEFAULT},
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.reportIdPattern), value)
            apply()
        } })
    var recvMail = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.recvMail), RECVMAILDEFAULT)?:RECVMAILDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.recvMail), value)
            apply()
        } })
    var useOdfOutput = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.useOdfOutput), USEODFOUTPUTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.useOdfOutput), value)
            apply()
        } })
    var useXlsxOutput = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.useXlsxOutput), USEXLSXOUTPUTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.useXlsxOutput), value)
            apply()
        } })
    var selectOutput = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.selectOutput), SELECTOUTPUTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.selectOutput), value)
            apply()
        } })
    var sFtpHost = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.sFtpHost), SFTPHOSTDEFAULT)?:SFTPHOSTDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.sFtpHost), value)
            apply()
        } })
    var sFtpPort = ConfigElement<Int>(
        { sharedPrefs.getInt(ctx.getString(R.string.sFtpPort), SFTPPORTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.sFtpPort), value)
            apply()
        } })
    var sFtpUser = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.sFtpUser), SFTPUSERDEFAULT)?:SFTPUSERDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.sFtpUser), value)
            apply()
        } })
    // Attention: This shall not be included in the backup
    var sFtpPassword = ConfigElement<String>(
        { encryptedSharedPrefs.getString(ctx.getString(R.string.sFtpPassword), SFTPPASSWORDDEFAULT)?:SFTPPASSWORDDEFAULT},
        { value -> with(encryptedSharedPrefs.edit()) {
            putString(ctx.getString(R.string.sFtpPassword), value)
            apply()
        } })
    var lumpSumServerPath = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.lumpSumServerPath), LUMPSUMSERVERPATHDEFAULT)?:LUMPSUMSERVERPATHDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.lumpSumServerPath), value)
            apply()
        } })
    var odfTemplateServerPath = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.odfTemplateServerPath), ODFTEMPLATESERVERPATHDEFAULT)?:ODFTEMPLATESERVERPATHDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.odfTemplateServerPath), value)
            apply()
        } })
    var logoServerPath = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.logoServerPath), LOGOSERVERPATHDEFAULT)?:LOGOSERVERPATHDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.logoServerPath), value)
            apply()
        } })
    var footerServerPath = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.footerServerPath), FOOTERSERVERPATHDEFAULT)?:FOOTERSERVERPATHDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.footerServerPath), value)
            apply()
        } })
    var lumpSums = ConfigElement<Set<String>>(
        {
            val a: Collection<String> = sharedPrefs.getStringSet(ctx.getString(R.string.lumpSums), LUMPSUMSDEFAULT)?:LUMPSUMSDEFAULT
            a.toSet()
        },
        { value -> with(sharedPrefs.edit()) {
            putStringSet(ctx.getString(R.string.footerServerPath), value)
            apply()
        } })
    var activeReportId = ConfigElement<Int>(
        { sharedPrefs.getInt(ctx.getString(R.string.activeReportId), ACTIVEREPORTIDDEFAULT)?:ACTIVEREPORTIDDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.activeReportId), value)
            apply()
        } })
    var workItemDictionary = ConfigElement<Set<String>>(
        {
            val a: Collection<String> = sharedPrefs.getStringSet(ctx.getString(R.string.workItemDictionary), WORKITEMDICTIONARYDEFAULT)?:WORKITEMDICTIONARYDEFAULT
            a.toSet()
        },
        { value -> with(sharedPrefs.edit()) {
            putStringSet(ctx.getString(R.string.workItemDictionary), value)
            apply()
        } })
    var materialDictionary = ConfigElement<Set<String>>(
        {
            val a: Collection<String> = sharedPrefs.getStringSet(ctx.getString(R.string.materialDictionary), MATERIALDICTIONARYDEFAULT)?: MATERIALDICTIONARYDEFAULT
            a.toSet()
        },
        { value -> with(sharedPrefs.edit()) {
            putStringSet(ctx.getString(R.string.materialDictionary), value)
            apply()
        } })
    var odfTemplateFile = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.odfTemplateFile), ODFTEMPLATEFILEDEFAULT)?:ODFTEMPLATEFILEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.odfTemplateFile), value)
            apply()
        } })
    var logoFile = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.logoFile), LOGOFILEDEFAULT)?:LOGOFILEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.logoFile), value)
            apply()
        } })
    // Attention: stored preference uses Float!
    var logoRatio = ConfigElement<Double>(
        { sharedPrefs.getFloat(ctx.getString(R.string.logoRatio), LOGORATIODEFAULT).toDouble()},
        { value -> with(sharedPrefs.edit()) {
            putFloat(ctx.getString(R.string.logoRatio), value.toFloat())
            apply()
        } })
    var footerFile = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.footerFile), FOOTERFILEDEFAULT)?:FOOTERFILEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.footerFile), value)
            apply()
        } })
    // Attention: stored preference uses Float!
    var footerRatio = ConfigElement<Double>(
        { sharedPrefs.getFloat(ctx.getString(R.string.footerRatio), FOOTERRATIODEFAULT).toDouble()},
        { value -> with(sharedPrefs.edit()) {
            putFloat(ctx.getString(R.string.footerRatio), value.toFloat())
            apply()
        } })
    var fontSize = ConfigElement<Int>(
        { sharedPrefs.getInt(ctx.getString(R.string.fontSize), FONTSIZEDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.fontSize), value)
            apply()
        } })
    var crashlyticsEnable = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.crashlyticsEnabled), CRASHLYTICSENABLEDDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.crashlyticsEnabled), value)
            apply()
        } })
    var pdfUseLogo = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.pdfUseLogo), PDFUSELOGODEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.pdfUseLogo), value)
            apply()
        } })
    var pdfUseFooter = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.pdfUseFooter), PDFUSEFOOTERDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.pdfUseFooter), value)
            apply()
        } })
    var xlsxUseLogo = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.xlsxUseLogo), XLSXUSELOGODEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.xlsxUseLogo), value)
            apply()
        } })
    var xlsxLogoWidth = ConfigElement<Int>(
        { sharedPrefs.getInt(ctx.getString(R.string.xlsxLogoWidth), XLSXLOGOWIDTHDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.xlsxLogoWidth), value)
            apply()
        } })
    var xlsxUseFooter = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.xlsxUseFooter), XLSXUSEFOOTERDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.xlsxUseFooter), value)
            apply()
        } })
    var xlsxFooterWidth = ConfigElement<Int>(
        { sharedPrefs.getInt(ctx.getString(R.string.xlsxFooterWidth), XLSXFOOTERWIDTHDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.xlsxFooterWidth), value)
            apply()
        } })
    var scalePhotos = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.scalePhotos), SCALEPHOTOSDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.scalePhotos), value)
            apply()
        } })
    var photoResolution = ConfigElement<Int>(
        { sharedPrefs.getInt(ctx.getString(R.string.photoResolution), PHOTORESOLUTIONDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.photoResolution), value)
            apply()
        } })
    var currentClientId = ConfigElement<Int>(
        { sharedPrefs.getInt(ctx.getString(R.string.currentClientId), CURRENTCLIENTIDDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.currentClientId), value)
            apply()
        } })
    var useInlinePdfViewer = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.useInlinePdfViewer), USEINLINEPDFVIEWERDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.useInlinePdfViewer), value)
            apply()
        } })
    var filterProjectName = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.filterProjectName), FILTERPROJECTNAMEDEFAULT)?:FILTERPROJECTNAMEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.filterProjectName), value)
            apply()
        } })
    var filterProjectExtra = ConfigElement<String>(
        { sharedPrefs.getString(ctx.getString(R.string.filterProjectExtra), FILTERPROJECTEXTRADEFAULT)?:FILTERPROJECTEXTRADEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString(ctx.getString(R.string.filterProjectExtra), value)
            apply()
        } })
    var filterStates = ConfigElement<Int>(
        { sharedPrefs.getInt(ctx.getString(R.string.filterStates), FILTERSTATESDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.filterStates), value)
            apply()
        } })
    var lockScreenOrientation = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.lockScreenOrientation), LOCKSCREENORIENTATIONDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.lockScreenOrientation), value)
            apply()
        } })
    var lockScreenOrientationNoInfo = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean(ctx.getString(R.string.lockScreenOrientationNoInfo), LOCKSCREENORIENTATIONNOINFODEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean(ctx.getString(R.string.lockScreenOrientationNoInfo), value)
            apply()
        } })
    var pdfLogoWidthPercent = ConfigElement<Int>(
        { sharedPrefs.getInt(ctx.getString(R.string.pdfLogoWidthPercent), PDFLOGOWIDTHPERCENTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.pdfLogoWidthPercent), value)
            apply()
        } })
    // Attention: stored preference uses Int!
    var pdfLogoAlignment = ConfigElement<Alignment>(
        { Alignment.values()[sharedPrefs.getInt(ctx.getString(R.string.pdfLogoAlignment), PDFLOGOALIGNMENTDEFAULT)] },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.pdfLogoAlignment), value.ordinal)
            apply()
        } })
    var pdfFooterWidthPercent = ConfigElement<Int>(
        { sharedPrefs.getInt(ctx.getString(R.string.pdfFooterWidthPercent), PDFFOOTERWIDTHPERCENTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.pdfFooterWidthPercent), value)
            apply()
        } })
    // Attention: stored preference uses Int!
    var pdfFooterAlignment = ConfigElement<Alignment>(
        { Alignment.values()[sharedPrefs.getInt(ctx.getString(R.string.pdfFooterAlignment), PDFFOOTERALIGNMENTDEFAULT)] },
        { value -> with(sharedPrefs.edit()) {
            putInt(ctx.getString(R.string.pdfFooterAlignment), value.ordinal)
            apply()
        } })

    fun fromConfig(cfg: Configuration) {
        versionCode.setValue(cfg.versionCode.toLong())
        employeeName.setValue(cfg.employeeName)
        currentId = cfg.currentId
        deviceName.setValue(cfg.deviceName)
        reportIdPattern.setValue(cfg.reportIdPattern.value?:REPORTIDPATTERNDEFAULT)
        recvMail.setValue(cfg.recvMail)
        useOdfOutput.setValue(cfg.useOdfOutput)
        useXlsxOutput.setValue(cfg.useXlsxOutput)
        selectOutput.setValue(cfg.selectOutput)
        sFtpHost.setValue(cfg.sFtpHost)
        sFtpPort.setValue(cfg.sFtpPort)
        sFtpUser.setValue(cfg.sFtpUser)
        sFtpPassword.setValue(cfg.sFtpEncryptedPassword)
        lumpSumServerPath.setValue(cfg.lumpSumServerPath)
        odfTemplateServerPath.setValue(cfg.odfTemplateServerPath)
        logoServerPath.setValue(cfg.logoServerPath)
        footerServerPath.setValue(cfg.footerServerPath)
        lumpSums.setValue(cfg.lumpSums.toSet())
        activeReportId.setValue(cfg.activeReportId)
        workItemDictionary.setValue(cfg.workItemDictionary)
        materialDictionary.setValue(cfg.materialDictionary)
        odfTemplateFile.setValue(cfg.odfTemplateFile)
        logoFile.setValue(cfg.logoFile)
        logoRatio.setValue(cfg.logoRatio)
        footerFile.setValue(cfg.footerFile)
        footerRatio.setValue(cfg.footerRatio)
        fontSize.setValue(cfg.fontSize)
        crashlyticsEnable.setValue(cfg.crashlyticsEnabled)
        pdfUseLogo.setValue(cfg.pdfUseLogo)
        pdfUseFooter.setValue(cfg.pdfUseFooter)
        xlsxUseLogo.setValue(cfg.xlsxUseLogo)
        xlsxLogoWidth.setValue(cfg.xlsxLogoWidth)
        xlsxUseFooter.setValue(cfg.xlsxUseFooter)
        xlsxFooterWidth.setValue(cfg.xlsxFooterWidth)
        scalePhotos.setValue(cfg.scalePhotos)
        photoResolution.setValue(cfg.photoResolution)
        currentClientId.setValue(cfg.currentClientId)
        useInlinePdfViewer.setValue(cfg.useInlinePdfViewer)
        filterProjectName.setValue(cfg.filterProjectName)
        filterProjectExtra.setValue(cfg.filterProjectExtra)
        filterStates.setValue(cfg.filterStates)
        lockScreenOrientation.setValue(cfg.lockScreenOrientation)
        lockScreenOrientationNoInfo.setValue(cfg.lockScreenOrientationNoInfo)
        pdfLogoWidthPercent.setValue(cfg.pdfLogoWidthPercent)
        pdfLogoAlignment.setValue(when(cfg.pdfFooterAlignment) {
            Configuration.Alignment.CENTER -> Alignment.CENTER
            Configuration.Alignment.RIGHT -> Alignment.RIGHT
            else -> Alignment.LEFT
        })
        pdfFooterWidthPercent.setValue(cfg.pdfFooterWidthPercent)
        pdfFooterAlignment.setValue(when(cfg.pdfFooterAlignment) {
            Configuration.Alignment.CENTER -> Alignment.CENTER
            Configuration.Alignment.RIGHT -> Alignment.RIGHT
            else -> Alignment.LEFT
        })
    }
}
