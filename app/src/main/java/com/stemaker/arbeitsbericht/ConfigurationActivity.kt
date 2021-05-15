package com.stemaker.arbeitsbericht

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.stemaker.arbeitsbericht.data.configuration
import com.stemaker.arbeitsbericht.helpers.SftpProvider
import com.stemaker.arbeitsbericht.helpers.showInfoDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
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
            findViewById<SwitchMaterial>(R.id.crashlog_enable).isChecked = configuration().crashlyticsEnabled
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(configuration().crashlyticsEnabled)
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
            findViewById<SwitchMaterial>(R.id.pdf_use_logo).isChecked = configuration().pdfUseLogo
            findViewById<SwitchMaterial>(R.id.pdf_use_footer).isChecked = configuration().pdfUseFooter
            findViewById<SwitchMaterial>(R.id.xlsx_use_logo).isChecked = configuration().xlsxUseLogo
            findViewById<SwitchMaterial>(R.id.xlsx_use_footer).isChecked = configuration().xlsxUseFooter
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

            // TODO: move the combination of edit text and slider into a custom view
            // PDF font size
            val fontSizeSlider = findViewById<Slider>(R.id.fontsize_slider)
            val fontSizeEdit = findViewById<EditText>(R.id.fontsize_text)
            fontSizeSlider.value = configuration().fontSize.toFloat()
            fontSizeEdit.text.replace(0, 0, configuration().fontSize.toString())
            fontSizeSlider.addOnChangeListener { _, value, _ ->
                fontSizeEdit.text.replace(0, fontSizeEdit.text.length, value.toInt().toString())
            }
            fontSizeEdit.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(text: Editable?) {
                    Log.d(TAG, "${text}")
                    try {
                        val f = text?.toString()?.toFloat()
                        f?.let {
                            when {
                                f == 0.0f && fontSizeSlider.valueFrom > 0.0f -> return
                                f < fontSizeSlider.valueFrom -> fontSizeSlider.value = fontSizeSlider.valueFrom
                                f > fontSizeSlider.valueTo ->  fontSizeSlider.value = fontSizeSlider.valueTo
                                else -> fontSizeSlider.value = f
                            }
                        }
                    } catch (e:NumberFormatException) {
                        fontSizeSlider.value = fontSizeSlider.valueFrom
                    }
                }
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            })

            // XLSX logo width
            val xlsxLogoWidthSlider = findViewById<Slider>(R.id.xlsx_logo_width_slider)
            val xlsxLogoWidthEdit = findViewById<EditText>(R.id.xlsx_logo_width_text)
            xlsxLogoWidthSlider.value = configuration().xlsxLogoWidth.toFloat()
            xlsxLogoWidthEdit.text.replace(0, 0, configuration().xlsxLogoWidth.toString())
            xlsxLogoWidthSlider.addOnChangeListener { _, value, _ ->
                xlsxLogoWidthEdit.text.replace(0, xlsxLogoWidthEdit.text.length, value.toInt().toString())
            }
            xlsxLogoWidthEdit.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(text: Editable?) {
                    try {
                        val f = text?.toString()?.toFloat()
                        f?.let {
                            when {
                                f == 0.0f && xlsxLogoWidthSlider.valueFrom > 0.0f -> return
                                f < xlsxLogoWidthSlider.valueFrom -> xlsxLogoWidthSlider.value = xlsxLogoWidthSlider.valueFrom
                                f > xlsxLogoWidthSlider.valueTo ->  xlsxLogoWidthSlider.value = xlsxLogoWidthSlider.valueTo
                                else -> xlsxLogoWidthSlider.value = f
                            }
                        }
                    } catch (e:NumberFormatException) {
                        xlsxLogoWidthSlider.value = xlsxLogoWidthSlider.valueFrom
                    }
                }
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            })
            // XLSX footer width
            val xlsxFooterWidthSlider = findViewById<Slider>(R.id.xlsx_footer_width_slider)
            val xlsxFooterWidthEdit = findViewById<EditText>(R.id.xlsx_footer_width_text)
            xlsxFooterWidthSlider.value = configuration().xlsxFooterWidth.toFloat()
            xlsxFooterWidthEdit.text.replace(0, 0, configuration().xlsxFooterWidth.toString())
            xlsxFooterWidthSlider.addOnChangeListener { _, value, _ ->
                xlsxFooterWidthEdit.text.replace(0, xlsxFooterWidthEdit.text.length, value.toInt().toString())
            }
            xlsxFooterWidthEdit.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(text: Editable?) {
                    try {
                        val f = text?.toString()?.toFloat()
                        f?.let {
                            when {
                                f == 0.0f && xlsxFooterWidthSlider.valueFrom > 0.0f -> return
                                f < xlsxFooterWidthSlider.valueFrom -> xlsxFooterWidthSlider.value = xlsxFooterWidthSlider.valueFrom
                                f > xlsxFooterWidthSlider.valueTo ->  xlsxFooterWidthSlider.value = xlsxFooterWidthSlider.valueTo
                                else -> xlsxFooterWidthSlider.value = f
                            }
                        }
                    } catch (e:NumberFormatException) {
                        xlsxFooterWidthSlider.value = xlsxFooterWidthSlider.valueFrom
                    }
                }
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            })
            // Photo width
            val scalePhotosSlider = findViewById<Slider>(R.id.scale_photos_slider)
            val scalePhotosEdit = findViewById<EditText>(R.id.scale_photos_value)
            scalePhotosSlider.value = configuration().photoResolution.toFloat()
            scalePhotosEdit.text.replace(0, 0, configuration().photoResolution.toString())
            scalePhotosSlider.addOnChangeListener { _, value, _ ->
                scalePhotosEdit.text.replace(0, scalePhotosEdit.text.length, value.toInt().toString())
            }
            scalePhotosEdit.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(text: Editable?) {
                    try {
                        val f = text?.toString()?.toFloat()
                        f?.let {
                            when {
                                f == 0.0f && scalePhotosSlider.valueFrom > 0.0f -> return
                                f < scalePhotosSlider.valueFrom -> scalePhotosSlider.value = scalePhotosSlider.valueFrom
                                f > scalePhotosSlider.valueTo ->  scalePhotosSlider.value = scalePhotosSlider.valueTo
                                else -> scalePhotosSlider.value = f
                            }
                        }
                    } catch (e:NumberFormatException) {
                        scalePhotosSlider.value = scalePhotosSlider.valueFrom
                    }
                }
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            })
            val scalePhotosSwitch = findViewById<SwitchMaterial>(R.id.scale_photos)
            scalePhotosSwitch.isChecked = configuration().scalePhotos
            scalePhotosSwitch.setOnCheckedChangeListener { button, b ->
                scalePhotosSlider.isEnabled = b
                scalePhotosEdit.isEnabled = b
            }
            if(!configuration().scalePhotos) {
                scalePhotosSlider.isEnabled = false
            }
        }
        findViewById<MaterialToolbar>(R.id.configuration_activity_toolbar).setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.config_save_button -> {
                    save()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
    }

    private fun showFileInImageView(fileName: String, id: Int): Double {
        val imgV = findViewById<ImageView>(id)
        var ratio = 1.0
        if(fileName == "") {
            imgV.setImageResource(R.drawable.ic_clear_black_24dp)
        } else {
            val file = File(this@ConfigurationActivity.filesDir, fileName)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ratio = bitmap.width.toDouble() / bitmap.height.toDouble()
                imgV.setImageBitmap(bitmap)
                return ratio
            } else {
                imgV.setImageResource(R.drawable.ic_clear_black_24dp)
            }
        }
        return ratio
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
        configuration().crashlyticsEnabled = findViewById<SwitchMaterial>(R.id.crashlog_enable).isChecked
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(configuration().crashlyticsEnabled)
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
        configuration().fontSize = findViewById<Slider>(R.id.fontsize_slider).value.toInt()
        configuration().pdfUseLogo = findViewById<SwitchMaterial>(R.id.pdf_use_logo).isChecked
        configuration().pdfUseFooter = findViewById<SwitchMaterial>(R.id.pdf_use_footer).isChecked
        configuration().xlsxUseLogo = findViewById<SwitchMaterial>(R.id.xlsx_use_logo).isChecked
        configuration().xlsxUseFooter = findViewById<SwitchMaterial>(R.id.xlsx_use_footer).isChecked
        configuration().xlsxLogoWidth = findViewById<Slider>(R.id.xlsx_logo_width_slider).value.toInt()
        configuration().xlsxFooterWidth = findViewById<Slider>(R.id.xlsx_footer_width_slider).value.toInt()
        configuration().photoResolution = findViewById<Slider>(R.id.scale_photos_slider).value.toInt()
        configuration().scalePhotos = findViewById<SwitchMaterial>(R.id.scale_photos).isChecked
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
                val host = findViewById<EditText>(R.id.sftp_host).text.toString()
                val port = findViewById<EditText>(R.id.sftp_port).text.toString().toInt()
                val user = findViewById<EditText>(R.id.sftp_user).text.toString()
                var pwd = findViewById<EditText>(R.id.sftp_pwd).text.toString()
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
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_LOAD && resultCode == RESULT_OK) {
            val selectedfile = data?.getData()
            //TODO: We would need to handle the case the app gets destroyed in between, can be easily
            //reproduced by enabling don't keep activities in the dev options
            continuation!!.resume(selectedfile)
        }
    }

    private fun copyUriToFile(uri: Uri, fileName: String) {
        val inStream =  contentResolver.openInputStream(uri)
        if(inStream == null) throw Exception("Could not open input file")
        val outStream = FileOutputStream(File(this@ConfigurationActivity.filesDir, fileName))
        val buf = ByteArray(1024)
        var len: Int = inStream.read(buf)
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
                    val dst = "custom_output_template.ott"
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
                val host = findViewById<EditText>(R.id.sftp_host).text.toString()
                val port = findViewById<EditText>(R.id.sftp_port).text.toString().toInt()
                val user = findViewById<EditText>(R.id.sftp_user).text.toString()
                var pwd = findViewById<EditText>(R.id.sftp_pwd).text.toString()
                try {
                    if (pwd == "") pwd = configuration().sFtpEncryptedPassword
                    val sftpProvider = SftpProvider(this@ConfigurationActivity)
                    sftpProvider.connect(user, pwd, host, port)
                    Log.d(TAG, "Connection success")
                    val src = findViewById<EditText>(R.id.odf_template_ftp_path).text.toString()
                    val dst = "custom_output_template.ott"
                    sftpProvider.copyFile(src, "${filesDir}/$dst")
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
                    val dst = "logo.jpg"
                    copyUriToFile(file, dst)
                    configuration().logoFile = dst
                    configuration().logoRatio = showFileInImageView(dst, R.id.logo_image)
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
                val host = findViewById<EditText>(R.id.sftp_host).text.toString()
                val port = findViewById<EditText>(R.id.sftp_port).text.toString().toInt()
                val user = findViewById<EditText>(R.id.sftp_user).text.toString()
                var pwd = findViewById<EditText>(R.id.sftp_pwd).text.toString()
                val src = findViewById<EditText>(R.id.logo_ftp_path).text.toString()
                if (!src.endsWith(".jpg", true) && !src.endsWith(".jpeg", true)) {
                    showInfoDialog(getString(R.string.only_jpeg_supported), this@ConfigurationActivity)
                } else {
                    try {
                        if (pwd == "") pwd = configuration().sFtpEncryptedPassword
                        val sftpProvider = SftpProvider(this@ConfigurationActivity)
                        sftpProvider.connect(user, pwd, host, port)
                        Log.d(TAG, "Connection success")
                        val dst = "logo.jpg"
                        sftpProvider.copyFile(src, "${filesDir}/$dst")
                        sftpProvider.disconnect()
                        configuration().logoFile = dst
                        configuration().logoRatio = showFileInImageView(dst, R.id.logo_image)
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
                    val dst = "footer.jpg"
                    copyUriToFile(file, dst)
                    configuration().footerFile = dst
                    configuration().footerRatio = showFileInImageView(dst, R.id.footer_image)
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
                val host = findViewById<EditText>(R.id.sftp_host).text.toString()
                val port = findViewById<EditText>(R.id.sftp_port).text.toString().toInt()
                val user = findViewById<EditText>(R.id.sftp_user).text.toString()
                var pwd = findViewById<EditText>(R.id.sftp_pwd).text.toString()
                val src = findViewById<EditText>(R.id.footer_ftp_path).text.toString()
                if(!src.endsWith(".jpg", true) && !src.endsWith(".jpeg", true)) {
                    showInfoDialog(getString(R.string.only_jpeg_supported), this@ConfigurationActivity)
                } else {
                    try {
                        if (pwd == "") pwd = configuration().sFtpEncryptedPassword
                        val sftpProvider = SftpProvider(this@ConfigurationActivity)
                        sftpProvider.connect(user, pwd, host, port)
                        Log.d(TAG, "Connection success")
                        val dst = "footer.jpg"
                        sftpProvider.copyFile(src, "${filesDir}/$dst")
                        sftpProvider.disconnect()
                        configuration().footerFile = "footer.jpg"
                        configuration().footerRatio = showFileInImageView(dst, R.id.footer_image)
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
