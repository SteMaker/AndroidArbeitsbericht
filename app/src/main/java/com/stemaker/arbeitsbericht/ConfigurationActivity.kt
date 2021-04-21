package com.stemaker.arbeitsbericht

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.stemaker.arbeitsbericht.data.configuration
import com.stemaker.arbeitsbericht.helpers.SftpProvider
import com.stemaker.arbeitsbericht.helpers.showInfoDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.Exception
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "ConfigurationActivity"
private const val PERMISSION_CODE_REQUEST_INTERNET = 1
private const val REQUEST_LOAD = 123

class ConfigurationActivity : AppCompatActivity() {
    private var internetPermissionContinuation: Continuation<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        setSupportActionBar(findViewById(R.id.configuration_activity_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings)

        val storageInitJob = storageHandler().initialize()

        GlobalScope.launch(Dispatchers.Main) {
            storageInitJob?.let {
                if (!it.isCompleted) {
                    findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE
                    it.join()
                    findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
                }
            } ?: run { Log.e(TAG, "storageHandler job was null :(") }
            findViewById<EditText>(R.id.config_employee_name).setText(configuration().employeeName)
            findViewById<EditText>(R.id.config_device_name).setText(configuration().deviceName)
            findViewById<EditText>(R.id.config_report_id_pattern).setText(configuration().reportIdPattern)
            findViewById<EditText>(R.id.config_mail_receiver).setText(configuration().recvMail)
            findViewById<Switch>(R.id.crashlog_enable).isChecked = configuration().crashlyticsEnabled
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(configuration().crashlyticsEnabled);
            findViewById<EditText>(R.id.sftp_host).setText(configuration().sFtpHost)
            findViewById<EditText>(R.id.sftp_port).setText(configuration().sFtpPort.toString())
            findViewById<EditText>(R.id.sftp_user).setText(configuration().sFtpUser)
            when {
                configuration().useOdfOutput -> {
                    findViewById<RadioButton>(R.id.radio_odf_output).toggle()
                    findViewById<CardView>(R.id.odf_config_container).visibility = View.VISIBLE
                    findViewById<CardView>(R.id.pdf_config_container).visibility = View.GONE
                    findViewById<CardView>(R.id.xlsx_config_container).visibility = View.GONE
                }
                configuration().useXlsxOutput -> {
                    findViewById<RadioButton>(R.id.radio_xlsx_output).toggle()
                    findViewById<CardView>(R.id.odf_config_container).visibility = View.GONE
                    findViewById<CardView>(R.id.pdf_config_container).visibility = View.GONE
                    findViewById<CardView>(R.id.xlsx_config_container).visibility = View.VISIBLE
                }
                configuration().selectOutput -> {
                    findViewById<RadioButton>(R.id.radio_select_output).toggle()
                    findViewById<CardView>(R.id.odf_config_container).visibility = View.VISIBLE
                    findViewById<CardView>(R.id.pdf_config_container).visibility = View.VISIBLE
                    findViewById<CardView>(R.id.xlsx_config_container).visibility = View.VISIBLE
                }
                else -> {
                    findViewById<RadioButton>(R.id.radio_pdf_output).toggle()
                    findViewById<CardView>(R.id.odf_config_container).visibility = View.GONE
                    findViewById<CardView>(R.id.pdf_config_container).visibility = View.VISIBLE
                    findViewById<CardView>(R.id.xlsx_config_container).visibility = View.GONE
                }
            }
            findViewById<EditText>(R.id.odf_template_ftp_path).setText(configuration().odfTemplateServerPath)
            findViewById<EditText>(R.id.logo_ftp_path).setText(configuration().logoServerPath)
            findViewById<EditText>(R.id.footer_ftp_path).setText(configuration().footerServerPath)
            findViewById<CheckBox>(R.id.pdf_use_logo).isChecked = configuration().pdfUseLogo
            findViewById<CheckBox>(R.id.pdf_use_footer).isChecked = configuration().pdfUseFooter
            findViewById<CheckBox>(R.id.xlsx_use_logo).isChecked = configuration().xlsxUseLogo
            findViewById<CheckBox>(R.id.xlsx_use_footer).isChecked = configuration().xlsxUseFooter
            val radioGroup = findViewById<RadioGroup>(R.id.output_type_select_radiogroup)
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radio_odf_output -> {
                        findViewById<CardView>(R.id.odf_config_container).visibility = View.VISIBLE
                        findViewById<CardView>(R.id.pdf_config_container).visibility = View.GONE
                        findViewById<CardView>(R.id.xlsx_config_container).visibility = View.GONE
                    }
                    R.id.radio_pdf_output -> {
                        findViewById<CardView>(R.id.odf_config_container).visibility = View.GONE
                        findViewById<CardView>(R.id.pdf_config_container).visibility = View.VISIBLE
                        findViewById<CardView>(R.id.xlsx_config_container).visibility = View.GONE
                    }
                    R.id.radio_xlsx_output -> {
                        findViewById<CardView>(R.id.odf_config_container).visibility = View.GONE
                        findViewById<CardView>(R.id.pdf_config_container).visibility = View.GONE
                        findViewById<CardView>(R.id.xlsx_config_container).visibility = View.VISIBLE
                    }
                    R.id.radio_select_output -> {
                        findViewById<CardView>(R.id.odf_config_container).visibility = View.VISIBLE
                        findViewById<CardView>(R.id.pdf_config_container).visibility = View.VISIBLE
                        findViewById<CardView>(R.id.xlsx_config_container).visibility = View.VISIBLE
                    }
                }
            }
            showFileInImageView(configuration().footerFile, R.id.footer_image)
            showFileInImageView(configuration().logoFile, R.id.logo_image)
            val fontsb = findViewById<SeekBar>(R.id.fontsize_seekbar)
            val fontst = findViewById<TextView>(R.id.fontsize_text)
            fontsb.progress = configuration().fontSize
            fontst.text = "${getString(R.string.fontsize)}: ${fontsb.progress}"
            fontsb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    fontst.text = "${getString(R.string.fontsize)}: ${fontsb.progress}"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) { }
                override fun onStopTrackingTouch(seekBar: SeekBar?) { }
            })
            val xlsxLogoWidthSb = findViewById<SeekBar>(R.id.xlsx_logo_width_seekbar)
            val xlsxLogoWidthSt = findViewById<TextView>(R.id.xlsx_logo_width_text)
            xlsxLogoWidthSb.progress = configuration().xlsxLogoWidth
            xlsxLogoWidthSt.text = "${getString(R.string.logowidth)}: ${xlsxLogoWidthSb.progress}"
            xlsxLogoWidthSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    xlsxLogoWidthSt.text = "${getString(R.string.logowidth)}: ${xlsxLogoWidthSb.progress}"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) { }
                override fun onStopTrackingTouch(seekBar: SeekBar?) { }
            })
            val xlsxFooterWidthSb = findViewById<SeekBar>(R.id.xlsx_footer_width_seekbar)
            val xlsxFooterWidthSt = findViewById<TextView>(R.id.xlsx_footer_width_text)
            xlsxFooterWidthSb.progress = configuration().xlsxFooterWidth
            xlsxFooterWidthSt.text = "${getString(R.string.footerwidth)}: ${xlsxFooterWidthSb.progress}"
            xlsxFooterWidthSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    xlsxFooterWidthSt.text = "${getString(R.string.footerwidth)}: ${xlsxFooterWidthSb.progress}"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) { }
                override fun onStopTrackingTouch(seekBar: SeekBar?) { }
            })
        }
    }

    fun showFileInImageView(fileName: String, id: Int) {
        val imgV = findViewById<ImageView>(id)
        if(fileName == "") {
            imgV.setImageResource(R.drawable.ic_clear_black_24dp)
        } else {
            val file = File(fileName)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                configuration().logoRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
                imgV.setImageBitmap(bitmap)
            } else {
                imgV.setImageResource(R.drawable.ic_clear_black_24dp)
            }
        }
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

    private fun save() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE

        //val rename = checkForIdChange()
        configuration().employeeName = findViewById<EditText>(R.id.config_employee_name).getText().toString()
        configuration().deviceName = findViewById<EditText>(R.id.config_device_name).getText().toString()
        configuration().reportIdPattern = findViewById<EditText>(R.id.config_report_id_pattern).getText().toString()

        configuration().recvMail = findViewById<EditText>(R.id.config_mail_receiver).getText().toString()
        configuration().crashlyticsEnabled = findViewById<Switch>(R.id.crashlog_enable).isChecked
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(configuration().crashlyticsEnabled);
        configuration().useOdfOutput = findViewById<RadioButton>(R.id.radio_odf_output).isChecked
        configuration().useXlsxOutput = findViewById<RadioButton>(R.id.radio_xlsx_output).isChecked
        configuration().selectOutput = findViewById<RadioButton>(R.id.radio_select_output).isChecked
        configuration().sFtpHost = findViewById<EditText>(R.id.sftp_host).text.toString()
        configuration().sFtpPort = findViewById<EditText>(R.id.sftp_port).text.toString().toInt()
        configuration().sFtpUser = findViewById<EditText>(R.id.sftp_user).text.toString()
        val pwd = findViewById<EditText>(R.id.sftp_pwd).text.toString()
        if (pwd != "") // only overwrite if a new one has been set
            configuration().sFtpEncryptedPassword = pwd
        configuration().odfTemplateServerPath = findViewById<EditText>(R.id.odf_template_ftp_path).text.toString()
        configuration().logoServerPath = findViewById<EditText>(R.id.logo_ftp_path).text.toString()
        configuration().footerServerPath = findViewById<EditText>(R.id.footer_ftp_path).text.toString()
        configuration().fontSize = findViewById<SeekBar>(R.id.fontsize_seekbar).progress
        configuration().pdfUseLogo = findViewById<CheckBox>(R.id.pdf_use_logo).isChecked
        configuration().pdfUseFooter = findViewById<CheckBox>(R.id.pdf_use_footer).isChecked
        configuration().xlsxUseLogo = findViewById<CheckBox>(R.id.xlsx_use_logo).isChecked
        configuration().xlsxLogoWidth = findViewById<SeekBar>(R.id.xlsx_logo_width_seekbar).progress
        configuration().xlsxUseFooter = findViewById<CheckBox>(R.id.xlsx_use_footer).isChecked
        configuration().xlsxFooterWidth = findViewById<SeekBar>(R.id.xlsx_footer_width_seekbar).progress
        configuration().save()

        findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        val intent = Intent(this@ConfigurationActivity, MainActivity::class.java).apply {}
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

    private var continuation: Continuation<Uri?>? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_LOAD && resultCode == RESULT_OK) {
            val selectedfile = data?.getData()
            //TODO: We would need to handle the case the app gets destroyed in between, can be easily
            //reproduced by enabling don't keep activities in the dev options
            continuation!!.resume(selectedfile)
        }
    }

    private fun copyUriToFile(uri: Uri, filePath: String) {
        val inStream =  contentResolver.openInputStream(uri);
        if(inStream == null) throw Exception("Could not open input file")
        val outStream = FileOutputStream(File(filePath));
        val buf = ByteArray(1024)
        var len: Int = inStream.read(buf);
        while(len > 0) {
            outStream.write(buf,0,len)
            len = inStream.read(buf)
        }
        outStream.close()
        inStream.close()
    }

    fun onClickLoadTemplate(@Suppress("UNUSED_PARAMETER") btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val intent = Intent()
                .setType("application/vnd.oasis.opendocument.text-template")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, getString(R.string.odf_file_choose)), REQUEST_LOAD)
            val file = suspendCoroutine<Uri?> {
                continuation = it
            }
            if(file == null) {
                Log.d(TAG, "No ODF template file was selected")
            } else {
                Log.d(TAG, "Selected ODF template: ${file}")
                try {
                    val dst = "${filesDir}/custom_output_template.ott"
                    copyUriToFile(file, dst)
                    configuration().odfTemplateFile = dst
                } catch (e: Exception) {
                    showInfoDialog(getString(R.string.getfile_error), this@ConfigurationActivity, e.message?:getString(R.string.unknown))
                }
            }
        }
    }

    fun onClickSftpLoadTemplate(@Suppress("UNUSED_PARAMETER") btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            if(checkAndObtainInternetPermission()) {
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE
                val host = findViewById<AutoCompleteTextView>(R.id.sftp_host).text.toString()
                val port = findViewById<AutoCompleteTextView>(R.id.sftp_port).text.toString().toInt()
                val user = findViewById<AutoCompleteTextView>(R.id.sftp_user).text.toString()
                var pwd = findViewById<AutoCompleteTextView>(R.id.sftp_pwd).text.toString()
                try {
                    if (pwd == "") pwd = configuration().sFtpEncryptedPassword
                    val sftpProvider = SftpProvider(this@ConfigurationActivity)
                    sftpProvider.connect(user, pwd, host, port)
                    Log.d(TAG, "Connection success")
                    val src = findViewById<AutoCompleteTextView>(R.id.odf_template_ftp_path).text.toString()
                    val dst = "${filesDir}/custom_output_template.ott"
                    sftpProvider.copyFile(src, dst)
                    sftpProvider.disconnect()
                    configuration().odfTemplateFile = dst
                } catch (e: Exception) {
                    showInfoDialog(getString(R.string.getfile_error), this@ConfigurationActivity, e.message?:getString(R.string.unknown))
                }
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
    }

    fun onClickLoadLogo(@Suppress("UNUSED_PARAMETER") btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val mimetypes = arrayOf("image/jpeg", "image/png")
            val intent = Intent()
                .setType("*/*")
                .putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, getString(R.string.logo_file_choose)), REQUEST_LOAD)
            val file = suspendCoroutine<Uri?> {
                continuation = it
            }
            if(file == null) {
                Log.d(TAG, "No logo file was selected")
            } else {
                Log.d(TAG, "Selected logo: ${file}")
                try {
                    val dst = "${filesDir}/logo.jpg"
                    copyUriToFile(file, dst)
                    configuration().logoFile = dst
                    showFileInImageView(dst, R.id.logo_image)
                } catch (e: Exception) {
                    showInfoDialog(getString(R.string.getfile_error), this@ConfigurationActivity, e.message?:getString(R.string.unknown))
                }
            }
        }
    }

    fun onClickSftpLoadLogo(@Suppress("UNUSED_PARAMETER") btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            if(checkAndObtainInternetPermission()) {
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE
                val host = findViewById<AutoCompleteTextView>(R.id.sftp_host).text.toString()
                val port = findViewById<AutoCompleteTextView>(R.id.sftp_port).text.toString().toInt()
                val user = findViewById<AutoCompleteTextView>(R.id.sftp_user).text.toString()
                var pwd = findViewById<AutoCompleteTextView>(R.id.sftp_pwd).text.toString()
                val src = findViewById<AutoCompleteTextView>(R.id.logo_ftp_path).text.toString()
                if (!src.endsWith(".jpg", true) && !src.endsWith(".jpeg", true)) {
                    showInfoDialog(getString(R.string.only_jpeg_supported), this@ConfigurationActivity)
                } else {
                    try {
                        if (pwd == "") pwd = configuration().sFtpEncryptedPassword
                        val sftpProvider = SftpProvider(this@ConfigurationActivity)
                        sftpProvider.connect(user, pwd, host, port)
                        Log.d(TAG, "Connection success")
                        val dst = "${filesDir}/logo.jpg"
                        sftpProvider.copyFile(src, dst)
                        sftpProvider.disconnect()
                        configuration().logoFile = dst
                        showFileInImageView(dst, R.id.logo_image)
                    } catch (e: Exception) {
                        showInfoDialog(getString(R.string.getfile_error), this@ConfigurationActivity, e.message ?: getString(R.string.unknown))
                    }
                }
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
    }

    fun onClickLoadFooter(@Suppress("UNUSED_PARAMETER") btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val mimetypes = arrayOf("image/jpeg", "image/png")
            val intent = Intent()
                .setType("*/*")
                .putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, getString(R.string.logo_file_choose)), REQUEST_LOAD)
            val file = suspendCoroutine<Uri?> {
                continuation = it
            }
            if(file == null) {
                Log.d(TAG, "No footer file was selected")
            } else {
                Log.d(TAG, "Selected footer: ${file}")
                try {
                    val dst = "${filesDir}/footer.jpg"
                    copyUriToFile(file, dst)
                    configuration().footerFile = dst
                    showFileInImageView(dst, R.id.footer_image)
                } catch (e: Exception) {
                    showInfoDialog(getString(R.string.getfile_error), this@ConfigurationActivity, e.message?:getString(R.string.unknown))
                }
            }
        }
    }

    fun onClickSftpLoadFooter(@Suppress("UNUSED_PARAMETER") btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            if(checkAndObtainInternetPermission()) {
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE
                val host = findViewById<AutoCompleteTextView>(R.id.sftp_host).text.toString()
                val port = findViewById<AutoCompleteTextView>(R.id.sftp_port).text.toString().toInt()
                val user = findViewById<AutoCompleteTextView>(R.id.sftp_user).text.toString()
                var pwd = findViewById<AutoCompleteTextView>(R.id.sftp_pwd).text.toString()
                val src = findViewById<AutoCompleteTextView>(R.id.footer_ftp_path).text.toString()
                if(!src.endsWith(".jpg", true) && !src.endsWith(".jpeg", true)) {
                    showInfoDialog(getString(R.string.only_jpeg_supported), this@ConfigurationActivity)
                } else {
                    try {
                        if (pwd == "") pwd = configuration().sFtpEncryptedPassword
                        val sftpProvider = SftpProvider(this@ConfigurationActivity)
                        sftpProvider.connect(user, pwd, host, port)
                        Log.d(TAG, "Connection success")
                        val dst = "${filesDir}/footer.jpg"
                        sftpProvider.copyFile(src, dst)
                        sftpProvider.disconnect()
                        configuration().footerFile = dst
                        showFileInImageView(dst, R.id.footer_image)
                    } catch (e: Exception) {
                        showInfoDialog(getString(R.string.getfile_error), this@ConfigurationActivity, e.message ?: getString(R.string.unknown))
                    }
                }
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
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
