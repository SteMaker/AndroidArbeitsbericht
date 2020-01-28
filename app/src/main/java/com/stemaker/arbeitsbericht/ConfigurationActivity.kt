package com.stemaker.arbeitsbericht

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stemaker.arbeitsbericht.helpers.SftpProvider
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import com.stemaker.arbeitsbericht.helpers.showInfoDialog
import kotlinx.android.synthetic.main.activity_lump_sum_definition.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

private const val TAG = "ConfigurationActivity"
private const val PERMISSION_CODE_REQUEST_INTERNET = 1

class ConfigurationActivity : AppCompatActivity() {
    private var internetPermissionContinuation: Continuation<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        findViewById<EditText>(R.id.config_employee_name).setText(configuration().employeeName)
        findViewById<EditText>(R.id.config_next_id).setText(configuration().currentId.toString())
        findViewById<EditText>(R.id.config_mail_receiver).setText(configuration().recvMail)
        findViewById<EditText>(R.id.sftp_host).setText(configuration().sFtpHost)
        findViewById<EditText>(R.id.sftp_port).setText(configuration().sFtpPort.toString())
        findViewById<EditText>(R.id.sftp_user).setText(configuration().sFtpUser)
        if(configuration().useOdfOutput)
            findViewById<RadioButton>(R.id.radio_odf_output).toggle()
        else
            findViewById<RadioButton>(R.id.radio_pdf_output).toggle()
        findViewById<EditText>(R.id.odf_template_ftp_path).setText(configuration().odfTemplateServerPath)
    }

    fun onClickSave(@Suppress("UNUSED_PARAMETER") btn: View) {
        configuration().employeeName = findViewById<EditText>(R.id.config_employee_name).getText().toString()
        configuration().currentId = findViewById<EditText>(R.id.config_next_id).getText().toString().toInt()
        configuration().recvMail = findViewById<EditText>(R.id.config_mail_receiver).getText().toString()
        configuration().useOdfOutput = findViewById<RadioButton>(R.id.radio_odf_output).isChecked
        configuration().sFtpHost = findViewById<EditText>(R.id.sftp_host).text.toString()
        configuration().sFtpPort = findViewById<EditText>(R.id.sftp_port).text.toString().toInt()
        configuration().sFtpUser = findViewById<EditText>(R.id.sftp_user).text.toString()
        val pwd = findViewById<EditText>(R.id.sftp_pwd).text.toString()
        if(pwd != "") // only overwrite if a new one has been set
            configuration().sFtpEncryptedPassword = pwd
        configuration().odfTemplateServerPath = findViewById<EditText>(R.id.odf_template_ftp_path).text.toString()
        storageHandler().saveConfigurationToFile(getApplicationContext())
        val intent = Intent(this, MainActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickConnect(@Suppress("UNUSED_PARAMETER") btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            if(checkAndObtainInternetPermission()) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE
                val host = findViewById<AutoCompleteTextView>(R.id.sftp_host).text.toString()
                val port = findViewById<AutoCompleteTextView>(R.id.sftp_port).text.toString().toInt()
                val user = findViewById<AutoCompleteTextView>(R.id.sftp_user).text.toString()
                var pwd = findViewById<AutoCompleteTextView>(R.id.sftp_pwd).text.toString()
                if (pwd == "") pwd = configuration().sFtpEncryptedPassword
                try {
                    val sftpProvider = SftpProvider(this@ConfigurationActivity)
                    sftpProvider.connect(user, pwd, host, port)
                    Log.d(TAG, "Connection success")
                    sftpProvider.disconnect()
                    showInfoDialog(getString(R.string.sftp_connect_done), this@ConfigurationActivity)
                } catch (e: Exception) {
                    showInfoDialog(getString(R.string.sftp_connect_error, ""), this@ConfigurationActivity, e.message?:getString(R.string.unknown))
                }
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
    }

    fun onClickLoadTemplate(@Suppress("UNUSED_PARAMETER") btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            if(checkAndObtainInternetPermission()) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE
                val host = findViewById<AutoCompleteTextView>(R.id.sftp_host).text.toString()
                val port = findViewById<AutoCompleteTextView>(R.id.sftp_port).text.toString().toInt()
                val user = findViewById<AutoCompleteTextView>(R.id.sftp_user).text.toString()
                var pwd = findViewById<AutoCompleteTextView>(R.id.sftp_pwd).text.toString()
                if (pwd == "") pwd = configuration().sFtpEncryptedPassword
                try {
                    val sftpProvider = SftpProvider(this@ConfigurationActivity)
                    sftpProvider.connect(user, pwd, host, port)
                    Log.d(TAG, "Connection success")
                    val src = findViewById<AutoCompleteTextView>(R.id.odf_template_ftp_path).text.toString()
                    val dst = "${filesDir}/custom_output_template.ott"
                    sftpProvider.copyFile(src, dst)
                    sftpProvider.disconnect()
                    configuration().odfTemplateFile = dst
                } catch (e: Exception) {
                    showInfoDialog(getString(R.string.sftp_getfile_error), this@ConfigurationActivity, e.message?:getString(R.string.unknown))
                }
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
    }

    private suspend fun checkAndObtainInternetPermission(): Boolean {
        var internetPermissionGranted = true
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "No permission for Internet, requesting it")
            /* TODO Add a Dialog that explains why we need network access */
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.INTERNET),
                PERMISSION_CODE_REQUEST_INTERNET
            )
            internetPermissionGranted = suspendCoroutine<Boolean> {
                internetPermissionContinuation = it
            }
        }
        if(internetPermissionGranted) {
            Log.d(TAG, "Permission for Internet is granted")
            return true
        } else {
            val toast =
                Toast.makeText(this, R.string.no_internet_access_abort, Toast.LENGTH_LONG)
            toast.show()
            return false
        }
    }
}
