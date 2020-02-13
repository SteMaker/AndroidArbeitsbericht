package com.stemaker.arbeitsbericht.configuration

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stemaker.arbeitsbericht.workreport.WorkReportMainActivity
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.StorageHandler
import com.stemaker.arbeitsbericht.data.configuration.configuration
import com.stemaker.arbeitsbericht.helpers.SftpProvider
import com.stemaker.arbeitsbericht.helpers.showInfoDialog
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

        setSupportActionBar(findViewById(R.id.configuration_activity_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings)

        findViewById<EditText>(R.id.config_employee_name).setText(configuration().employeeName)
        findViewById<EditText>(R.id.config_device_name).setText(configuration().deviceName)
        findViewById<EditText>(R.id.config_report_id_pattern).setText(configuration().reportIdPattern)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.configuration_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.config_save_button -> {
                save()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun checkForIdChange(): Boolean {
        val pat = findViewById<EditText>(R.id.config_report_id_pattern).getText().toString()
        val en = findViewById<EditText>(R.id.config_employee_name).getText().toString()
        val dn = findViewById<EditText>(R.id.config_device_name).getText().toString()
        if(pat != configuration().reportIdPattern ||
            en != configuration().employeeName ||
            dn != configuration().deviceName) {
            return true
        }
        return false
    }

    fun save() {
        GlobalScope.launch(Dispatchers.Main) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE

            val rename = checkForIdChange()
            configuration().employeeName = findViewById<EditText>(R.id.config_employee_name).getText().toString()
            configuration().deviceName = findViewById<EditText>(R.id.config_device_name).getText().toString()
            configuration().reportIdPattern = findViewById<EditText>(R.id.config_report_id_pattern).getText().toString()

            if(rename)
                StorageHandler.renameReportsIfNeeded()

            configuration().recvMail = findViewById<EditText>(R.id.config_mail_receiver).getText().toString()
            configuration().useOdfOutput = findViewById<RadioButton>(R.id.radio_odf_output).isChecked
            configuration().sFtpHost = findViewById<EditText>(R.id.sftp_host).text.toString()
            configuration().sFtpPort = findViewById<EditText>(R.id.sftp_port).text.toString().toInt()
            configuration().sFtpUser = findViewById<EditText>(R.id.sftp_user).text.toString()
            val pwd = findViewById<EditText>(R.id.sftp_pwd).text.toString()
            if (pwd != "") // only overwrite if a new one has been set
                configuration().sFtpEncryptedPassword = pwd
            configuration().odfTemplateServerPath = findViewById<EditText>(R.id.odf_template_ftp_path).text.toString()
            StorageHandler.saveConfigurationToFile(getApplicationContext())

            findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            val intent = Intent(this@ConfigurationActivity, WorkReportMainActivity::class.java).apply {}
            startActivity(intent)
        }
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
                    showInfoDialog(getString(R.string.sftp_connect_error, ""), this@ConfigurationActivity, e.message?:getString(
                        R.string.unknown
                    ))
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
                    showInfoDialog(getString(R.string.sftp_getfile_error), this@ConfigurationActivity, e.message?:getString(
                        R.string.unknown
                    ))
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
