package com.stemaker.arbeitsbericht

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.*
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "Configuration"
@Serializable
class ConfigurationStore {
    var vers: Int = 0
    var employeeName: String = ""
    var deviceName: String = ""
    var reportIdPattern: String = "%p-%y-%6c"
    var currentId: Int = 1
    var recvMail: String = ""
    var useOdfOutput: Boolean = true
    var sFtpHost: String = ""
    var sFtpPort: Int = 22
    var sFtpUser: String = ""
    var sFtpEncryptedPassword: String = ""
    var sFtpUsedIV: String = ""
    var sFtpTagLength: Int = 0
    var lumpSumServerPath: String = ""
    var odfTemplateServerPath: String = ""
    var logoServerPath: String = ""
    var footerServerPath: String = ""
    var lumpSums = listOf<String>()
    var activeReportId: Int = -1 // Now used again, was deprecated
    var activeReportId2: String = "" // Deprecated
    var workItemDictionary = setOf<String>()
    var materialDictionary = setOf<String>()
    var odfTemplateFile: String = ""
    var logoFile: String = ""
    var footerFile: String = ""
    var fontSize: Int = 12
    var crashlyticsEnabled: Boolean = false
}

fun configuration(): Configuration{
    if(!Configuration.inited) {
        Configuration.initialize()
    }
    return Configuration
}

object Configuration {
    var inited = false
    var store = ConfigurationStore()
    val KEY_ALIAS = "ArbeitsberichtPasswordEncryptionKey"
    var _appUpdateWasDone = false
    val appUpdateWasDone: Boolean
        get(): Boolean {
            if(_appUpdateWasDone) {
                _appUpdateWasDone = false
                return true
            } else {
                return false
            }
        }

    val mutex = Mutex()

    suspend fun lock() = mutex.lock()
    suspend fun unlock() = mutex.unlock()

    fun initialize() {
        if(!inited) {
            inited = true
            storageHandler()
            // Had to add 100 because this is the value I used previously
            val versionCode = ArbeitsberichtApp.getVersionCode()+100
            if(store.vers < versionCode) {
                _appUpdateWasDone = true
                updateConfiguration(store.vers)
            }
        }
    }

    private fun updateConfiguration(oldVers: Int) {
        store.vers = ArbeitsberichtApp.getVersionCode()+100
    }

    var employeeName: String
        get(): String = store.employeeName
        set(value) {store.employeeName = value}

    var currentId: Int
        get(): Int = store.currentId
        set(value) {store.currentId = value}

    var deviceName: String
        get(): String = store.deviceName
        set(value) {store.deviceName = value}

    var reportIdPattern: String
        get(): String = store.reportIdPattern
        set(value) {store.reportIdPattern = value}

    var recvMail: String
        get(): String = store.recvMail
        set(value) {store.recvMail = value}

    var useOdfOutput: Boolean
        get(): Boolean = store.useOdfOutput
        set(value) {store.useOdfOutput = value}

    var sFtpHost: String
        get(): String = store.sFtpHost
            set(value) {store.sFtpHost = value}

    var sFtpPort: Int
        get(): Int = store.sFtpPort
            set(value) {store.sFtpPort = value}

    var lumpSumServerPath: String
        get(): String = store.lumpSumServerPath
        set(value) {store.lumpSumServerPath = value}

    var odfTemplateServerPath: String
        get(): String = store.odfTemplateServerPath
        set(value) {store.odfTemplateServerPath = value}

    var logoServerPath: String
        get(): String = store.logoServerPath
        set(value) {store.logoServerPath = value}

    var footerServerPath: String
        get(): String = store.footerServerPath
            set(value) {store.footerServerPath = value}

    var sFtpUser: String
        get(): String = store.sFtpUser
            set(value) {store.sFtpUser = value}

    var lumpSums: List<String>
        get(): List<String> = store.lumpSums
        set(value) {store.lumpSums = value}

    var activeReportId: Int
        get(): Int = store.activeReportId
            set(value) {store.activeReportId = value}

    var workItemDictionary: Set<String>
        get(): Set<String> = store.workItemDictionary
        set(value) {store.workItemDictionary = value}

    var materialDictionary: Set<String>
        get(): Set<String> = store.materialDictionary
            set(value) {store.materialDictionary = value}

    var sFtpEncryptedPassword: String
        set(value) {
            if(value != "") {
                store.sFtpEncryptedPassword = encryptPassword(value)
            }
        }
        get() {
            if (store.sFtpEncryptedPassword == "" || store.sFtpUsedIV == "") return ""
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val secretKey: SecretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            return decryptPassword(store.sFtpEncryptedPassword, secretKey)
        }

    var odfTemplateFile: String
        get(): String = store.odfTemplateFile
        set(value) {store.odfTemplateFile = value}

    var logoFile: String
        get(): String = store.logoFile
        set(value) {store.logoFile = value}

    var footerFile: String
        get(): String = store.footerFile
        set(value) {store.footerFile = value}

    var fontSize: Int
        get(): Int = store.fontSize
        set(value) {store.fontSize = value}

    var crashlyticsEnabled:Boolean
        get(): Boolean = store.crashlyticsEnabled
        set(value) {store.crashlyticsEnabled = value}

    private fun encryptPassword(pwd: String): String {
        /* Now we try to store the password in an encrypted way. First we retrieve a key
           from the Android key store */
        lateinit var keyStore: KeyStore
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                createPasswordEncryptionKey()
            } else {
                val secretKey: SecretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
                Log.d(TAG, "Encryption key already exists")
            }
        } catch(e:UnrecoverableKeyException) {
            // Try to recreate the key
            createPasswordEncryptionKey()
        } catch(e: Exception) {
            throw UnsupportedOperationException("Verschlüsseln des Passworts wird von ihrem Gerät nicht unterstützt. Das Passwort kann nicht gespeichert werden")
        }

        val secretKey: SecretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        val ciph: Cipher? =
            try {
                Cipher.getInstance("AES/GCM/NoPadding")
            } catch(e: Exception) {
                Log.d("Configuration::encryptPasswordHash", "Cannot encrypt password since Cipher cannot be found")
                throw UnsupportedOperationException("Verschlüsseln des Passworts wird von ihrem Telefon nicht unterstützt. Das Passwort kann nicht gespeichert werden")
            }
        ciph?: return ""
        ciph.init(Cipher.ENCRYPT_MODE, secretKey)
        val decryptedByteArray = pwd.toByteArray(Charsets.UTF_8)
        val encryptedByteArray = ciph.doFinal(decryptedByteArray)
        val encryptedBase64Encoded = Base64.encodeToString(encryptedByteArray, Base64.NO_WRAP)
        store.sFtpUsedIV = Base64.encodeToString(ciph.iv, Base64.NO_WRAP)
        store.sFtpTagLength = ciph.parameters.getParameterSpec(GCMParameterSpec::class.java).tLen
        return encryptedBase64Encoded
    }

    private fun createPasswordEncryptionKey() {
        Log.d("Configuration::createPasswordEncryptionKey", "Creating encryption key")
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val paramSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .run {
                setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                setRandomizedEncryptionRequired(true)
                setUserAuthenticationRequired(false)
                build()
            }
        keyGenerator.init(paramSpec)
        keyGenerator.generateKey()
    }

    private fun decryptPassword(encryptedBase64Encoded: String, key: SecretKey): String {
        Log.d("Configuration::decryptPasswordHash", "called")
        val ciph: Cipher? =
            try {
                Cipher.getInstance("AES/GCM/NoPadding")
            } catch(e: Exception) {
                Log.d("Configuration::decryptPasswordHash", "Cannot decrypt password since Cipher cannot be found")
                throw UnsupportedOperationException("Entschlüsseln des Passworts wird von ihrem Telefon nicht unterstützt. Das Passwort kann nicht gespeichert werden")
            }
        ciph?: return ""
        val iv = Base64.decode(store.sFtpUsedIV, Base64.NO_WRAP)
        val gcmParameterSpec = GCMParameterSpec(store.sFtpTagLength, iv)
        ciph.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec)
        val encryptedByteArray = Base64.decode(encryptedBase64Encoded, Base64.NO_WRAP)
        val decryptedByteArray = ciph.doFinal(encryptedByteArray)
        val decrypted = String(decryptedByteArray, Charsets.UTF_8)
        return decrypted
    }

    fun save() {
        store.vers = ArbeitsberichtApp.getVersionCode()+100
        storageHandler().saveConfigurationToFile(ArbeitsberichtApp.appContext)
    }
}