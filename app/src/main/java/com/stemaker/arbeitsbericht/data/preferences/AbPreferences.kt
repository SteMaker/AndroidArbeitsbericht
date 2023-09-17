package com.stemaker.arbeitsbericht.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.stemaker.arbeitsbericht.data.configuration.Configuration
import com.stemaker.arbeitsbericht.data.report.ReportData

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
    MutableLiveData<T>()
{
    private val defaultValue = initFct()
    override fun postValue(value: T) {
        super.postValue(value)
        setFct(value)
    }

    override fun setValue(value: T) {
        super.setValue(value)
        setFct(value)
    }

    override fun getValue(): T {
        return super.getValue()?:defaultValue
    }

    init {
        super.setValue(defaultValue)
    }
}

class AbPreferences(private val ctx: Context)
{
    enum class Alignment(val a: Int) {
        CENTER(0),
        LEFT(1),
        RIGHT(2)
    }
    // We are using shared preferences as data store and also listen(!) for changes to reflect them in our LiveData.
    // That allows any component in the app to use the shared preferences as well as the LiveData, just what is
    // more appropriate
    private val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
    private val masterKey = MasterKey.Builder(ctx)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // We are not listening on encrypted shared prefs. Need to see how to link it with the config activity
    private val encryptedSharedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        ctx, encryptedSharedPrefsFile, masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

    val versionCode = ConfigElement<Long>(
        { sharedPrefs.getLong("versionCode", VERSIONCODEDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putLong("versionCode", value)
            apply()
        }})

    val employeeName = ConfigElement<String>(
        { sharedPrefs.getString("employeeName", EMPLOYEENAMEDEFAULT)?:EMPLOYEENAMEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("employeeName", value)
            apply()
        } })
    var currentId: Int
        get() = sharedPrefs.getInt("currentId", CURRENTIDDEFAULT)
        private set(value) {
            with(sharedPrefs.edit()) {
                putInt("currentId", value)
                apply()
            }
        }
    fun overrideCurrentId(id: Int) {
        currentId = id
    }
    fun allocateReportId() : Int {
        if(Looper.getMainLooper().thread != Thread.currentThread()) {
            Log.e(TAG, "Error: Allocating ID, not running on the main thread")
        }
        val id = currentId
        currentId = id + 1
        return id
    }
    val deviceName = ConfigElement<String>(
        { sharedPrefs.getString("deviceName", DEVICENAMEDEFAULT)?:DEVICENAMEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("deviceName", value)
            apply()
        } })
    val reportIdPattern = ConfigElement<String>(
        { sharedPrefs.getString("reportIdPattern", REPORTIDPATTERNDEFAULT)?: REPORTIDPATTERNDEFAULT},
        { value -> with(sharedPrefs.edit()) {
            putString("reportIdPattern", value)
            apply()
        } })
    val recvMail = ConfigElement<String>(
        { sharedPrefs.getString("recvMail", RECVMAILDEFAULT)?:RECVMAILDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("recvMail", value)
            apply()
        } })
    val useOdfOutput = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("useOdfOutput", USEODFOUTPUTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("useOdfOutput", value)
            apply()
        } })
    val useXlsxOutput = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("useXlsxOutput", USEXLSXOUTPUTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("useXlsxOutput", value)
            apply()
        } })
    val selectOutput = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("selectOutput", SELECTOUTPUTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("selectOutput", value)
            apply()
        } })
    val sFtpHost = ConfigElement<String>(
        { sharedPrefs.getString("sFtpHost", SFTPHOSTDEFAULT)?:SFTPHOSTDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("sFtpHost", value)
            apply()
        } })
    val sFtpPort = ConfigElement<Int>(
        { sharedPrefs.getInt("sFtpPort", SFTPPORTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt("sFtpPort", value)
            apply()
        } })
    val sFtpUser = ConfigElement<String>(
        { sharedPrefs.getString("sFtpUser", SFTPUSERDEFAULT)?:SFTPUSERDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("sFtpUser", value)
            apply()
        } })
    // Attention: This shall not be included in the backup
    val sFtpPassword = ConfigElement<String>(
        { encryptedSharedPrefs.getString("sFtpPassword", SFTPPASSWORDDEFAULT)?:SFTPPASSWORDDEFAULT},
        { value -> with(encryptedSharedPrefs.edit()) {
            putString("sFtpPassword", value)
            apply()
        } })
    val lumpSumServerPath = ConfigElement<String>(
        { sharedPrefs.getString("lumpSumServerPath", LUMPSUMSERVERPATHDEFAULT)?:LUMPSUMSERVERPATHDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("lumpSumServerPath", value)
            apply()
        } })
    val odfTemplateServerPath = ConfigElement<String>(
        { sharedPrefs.getString("odfTemplateServerPath", ODFTEMPLATESERVERPATHDEFAULT)?:ODFTEMPLATESERVERPATHDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("odfTemplateServerPath", value)
            apply()
        } })
    val logoServerPath = ConfigElement<String>(
        { sharedPrefs.getString("logoServerPath", LOGOSERVERPATHDEFAULT)?:LOGOSERVERPATHDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("logoServerPath", value)
            apply()
        } })
    val footerServerPath = ConfigElement<String>(
        { sharedPrefs.getString("footerServerPath", FOOTERSERVERPATHDEFAULT)?:FOOTERSERVERPATHDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("footerServerPath", value)
            apply()
        } })
    val lumpSums = ConfigElement<Set<String>>(
        {
            val a: Collection<String> = sharedPrefs.getStringSet("lumpSums", LUMPSUMSDEFAULT)?:LUMPSUMSDEFAULT
            a.toSet()
        },
        { value -> with(sharedPrefs.edit()) {
            putStringSet("lumpSums", value)
            apply()
        } })
    val activeReportId = ConfigElement<Int>(
        { sharedPrefs.getInt("activeReportId", ACTIVEREPORTIDDEFAULT)},
        { value -> with(sharedPrefs.edit()) {
            putInt("activeReportId", value)
            apply()
        } })
    val workItemDictionary = ConfigElement<Set<String>>(
        {
            val a: Collection<String> = sharedPrefs.getStringSet("workItemDictionary", WORKITEMDICTIONARYDEFAULT)?:WORKITEMDICTIONARYDEFAULT
            a.toSet()
        },
        { value -> with(sharedPrefs.edit()) {
            putStringSet("workItemDictionary", value)
            apply()
        } })
    val materialDictionary = ConfigElement<Set<String>>(
        {
            val a: Collection<String> = sharedPrefs.getStringSet("materialDictionary", MATERIALDICTIONARYDEFAULT)?: MATERIALDICTIONARYDEFAULT
            a.toSet()
        },
        { value -> with(sharedPrefs.edit()) {
            putStringSet("materialDictionary", value)
            apply()
        } })
    val odfTemplateFile = ConfigElement<String>(
        { sharedPrefs.getString("odfTemplateFile", ODFTEMPLATEFILEDEFAULT)?:ODFTEMPLATEFILEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("odfTemplateFile", value)
            apply()
        } })
    val logoFile = ConfigElement<String>(
        { sharedPrefs.getString("logoFile", LOGOFILEDEFAULT)?:LOGOFILEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("logoFile", value)
            apply()
        } })
    // Attention: stored preference uses Float!
    val logoRatio = ConfigElement<Double>(
        { sharedPrefs.getFloat("logoRatio", LOGORATIODEFAULT).toDouble()},
        { value -> with(sharedPrefs.edit()) {
            putFloat("logoRatio", value.toFloat())
            apply()
        } })
    val footerFile = ConfigElement<String>(
        { sharedPrefs.getString("footerFile", FOOTERFILEDEFAULT)?:FOOTERFILEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("footerFile", value)
            apply()
        } })
    // Attention: stored preference uses Float!
    val footerRatio = ConfigElement<Double>(
        { sharedPrefs.getFloat("footerRatio", FOOTERRATIODEFAULT).toDouble()},
        { value -> with(sharedPrefs.edit()) {
            putFloat("footerRatio", value.toFloat())
            apply()
        } })
    val fontSize = ConfigElement<Int>(
        { sharedPrefs.getInt("fontSize", FONTSIZEDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt("fontSize", value)
            apply()
        } })
    val crashlyticsEnable = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("crashlyticsEnabled", CRASHLYTICSENABLEDDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("crashlyticsEnabled", value)
            apply()
        } })
    val pdfUseLogo = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("pdfUseLogo", PDFUSELOGODEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("pdfUseLogo", value)
            apply()
        } })
    val pdfUseFooter = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("pdfUseFooter", PDFUSEFOOTERDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("pdfUseFooter", value)
            apply()
        } })
    val xlsxUseLogo = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("xlsxUseLogo", XLSXUSELOGODEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("xlsxUseLogo", value)
            apply()
        } })
    val xlsxLogoWidth = ConfigElement<Int>(
        { sharedPrefs.getInt("xlsxLogoWidth", XLSXLOGOWIDTHDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt("xlsxLogoWidth", value)
            apply()
        } })
    val xlsxUseFooter = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("xlsxUseFooter", XLSXUSEFOOTERDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("xlsxUseFooter", value)
            apply()
        } })
    val xlsxFooterWidth = ConfigElement<Int>(
        { sharedPrefs.getInt("xlsxFooterWidth", XLSXFOOTERWIDTHDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt("xlsxFooterWidth", value)
            apply()
        } })
    val scalePhotos = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("scalePhotos", SCALEPHOTOSDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("scalePhotos", value)
            apply()
        } })
    val photoResolution = ConfigElement<Int>(
        { sharedPrefs.getInt("photoResolution", PHOTORESOLUTIONDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt("photoResolution", value)
            apply()
        } })
    var currentClientId: Int
        get() = sharedPrefs.getInt("currentClientId", CURRENTCLIENTIDDEFAULT)
        private set(value) {
            with(sharedPrefs.edit()) {
                putInt("currentClientId", value)
                apply()
            }
        }
    fun allocateClientId() : Int {
        if(Looper.getMainLooper().thread != Thread.currentThread()) {
            Log.e(TAG, "Error: Allocating ID, not running on the main thread")
        }
        val id = currentClientId
        currentClientId = id + 1
        return id
    }
    val useInlinePdfViewer = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("useInlinePdfViewer", USEINLINEPDFVIEWERDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("useInlinePdfViewer", value)
            apply()
        } })
    val filterProjectName = ConfigElement<String>(
        { sharedPrefs.getString("filterProjectName", FILTERPROJECTNAMEDEFAULT)?:FILTERPROJECTNAMEDEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("filterProjectName", value)
            apply()
        } })
    val filterProjectExtra = ConfigElement<String>(
        { sharedPrefs.getString("filterProjectExtra", FILTERPROJECTEXTRADEFAULT)?:FILTERPROJECTEXTRADEFAULT },
        { value -> with(sharedPrefs.edit()) {
            putString("filterProjectExtra", value)
            apply()
        } })
    val filterStates = ConfigElement<Int>(
        { sharedPrefs.getInt("filterStates", FILTERSTATESDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt("filterStates", value)
            apply()
        } })
    val lockScreenOrientation = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("lockScreenOrientation", LOCKSCREENORIENTATIONDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("lockScreenOrientation", value)
            apply()
        } })
    val lockScreenOrientationNoInfo = ConfigElement<Boolean>(
        { sharedPrefs.getBoolean("lockScreenOrientationNoInfo", LOCKSCREENORIENTATIONNOINFODEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putBoolean("lockScreenOrientationNoInfo", value)
            apply()
        } })
    val pdfLogoWidthPercent = ConfigElement<Int>(
        { sharedPrefs.getInt("pdfLogoWidthPercent", PDFLOGOWIDTHPERCENTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt("pdfLogoWidthPercent", value)
            apply()
        } })
    // Attention: stored preference uses Int!
    val pdfLogoAlignment = ConfigElement<Alignment>(
        { Alignment.values()[sharedPrefs.getInt("pdfLogoAlignment", PDFLOGOALIGNMENTDEFAULT)] },
        { value -> with(sharedPrefs.edit()) {
            putInt("pdfLogoAlignment", value.ordinal)
            apply()
        } })
    val pdfFooterWidthPercent = ConfigElement<Int>(
        { sharedPrefs.getInt("pdfFooterWidthPercent", PDFFOOTERWIDTHPERCENTDEFAULT) },
        { value -> with(sharedPrefs.edit()) {
            putInt("pdfFooterWidthPercent", value)
            apply()
        } })
    // Attention: stored preference uses Int!
    val pdfFooterAlignment = ConfigElement<Alignment>(
        { Alignment.values()[sharedPrefs.getInt("pdfFooterAlignment", PDFFOOTERALIGNMENTDEFAULT)] },
        { value -> with(sharedPrefs.edit()) {
            putInt("pdfFooterAlignment", value.ordinal)
            apply()
        } })

    // Only valid directly after construction, afterwards it will deliver wrong info
    val isNewlyCreated = versionCode.value == VERSIONCODEDEFAULT

    fun fromConfig(cfg: Configuration) {
        versionCode.value = if(cfg.versionCode.toLong() > 100) { cfg.versionCode.toLong() - 100 } else { 0 }
        employeeName.value = cfg.employeeName
        currentId = cfg.currentId
        deviceName.value = cfg.deviceName
        reportIdPattern.value = cfg.reportIdPattern.value?:REPORTIDPATTERNDEFAULT
        recvMail.value = cfg.recvMail
        useOdfOutput.value = cfg.useOdfOutput
        useXlsxOutput.value = cfg.useXlsxOutput
        selectOutput.value = cfg.selectOutput
        sFtpHost.value = cfg.sFtpHost
        sFtpPort.value = cfg.sFtpPort
        sFtpUser.value = cfg.sFtpUser
        sFtpPassword.value = cfg.sFtpEncryptedPassword
        lumpSumServerPath.value = cfg.lumpSumServerPath
        odfTemplateServerPath.value = cfg.odfTemplateServerPath
        logoServerPath.value = cfg.logoServerPath
        footerServerPath.value = cfg.footerServerPath
        lumpSums.value = cfg.lumpSums.toSet()
        activeReportId.value = cfg.activeReportId
        workItemDictionary.value = cfg.workItemDictionary
        materialDictionary.value = cfg.materialDictionary
        odfTemplateFile.value = cfg.odfTemplateFile
        logoFile.value = cfg.logoFile
        logoRatio.value = cfg.logoRatio
        footerFile.value = cfg.footerFile
        footerRatio.value = cfg.footerRatio
        fontSize.value = cfg.fontSize
        crashlyticsEnable.value = cfg.crashlyticsEnabled
        pdfUseLogo.value = cfg.pdfUseLogo
        pdfUseFooter.value = cfg.pdfUseFooter
        xlsxUseLogo.value = cfg.xlsxUseLogo
        xlsxLogoWidth.value = cfg.xlsxLogoWidth
        xlsxUseFooter.value = cfg.xlsxUseFooter
        xlsxFooterWidth.value = cfg.xlsxFooterWidth
        scalePhotos.value = cfg.scalePhotos
        photoResolution.value = cfg.photoResolution
        currentClientId = cfg.currentClientId
        useInlinePdfViewer.value = cfg.useInlinePdfViewer
        filterProjectName.value = cfg.filterProjectName
        filterProjectExtra.value = cfg.filterProjectExtra
        filterStates.value = cfg.filterStates
        lockScreenOrientation.value = cfg.lockScreenOrientation
        lockScreenOrientationNoInfo.value = cfg.lockScreenOrientationNoInfo
        pdfLogoWidthPercent.value = cfg.pdfLogoWidthPercent
        pdfLogoAlignment.value = when(cfg.pdfLogoAlignment) {
            Configuration.Alignment.CENTER -> Alignment.CENTER
            Configuration.Alignment.RIGHT -> Alignment.RIGHT
            else -> Alignment.LEFT
        }
        pdfFooterWidthPercent.value = cfg.pdfFooterWidthPercent
        pdfFooterAlignment.value = when(cfg.pdfFooterAlignment) {
            Configuration.Alignment.CENTER -> Alignment.CENTER
            Configuration.Alignment.RIGHT -> Alignment.RIGHT
            else -> Alignment.LEFT
        }
    }
}
