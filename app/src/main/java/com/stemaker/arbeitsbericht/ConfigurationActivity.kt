package com.stemaker.arbeitsbericht

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.stemaker.arbeitsbericht.data.preferences.AbPreferences
import com.stemaker.arbeitsbericht.databinding.ActivityConfigurationBinding
import com.stemaker.arbeitsbericht.databinding.ActivityReportEditorBinding
import com.stemaker.arbeitsbericht.helpers.SftpProvider
import com.stemaker.arbeitsbericht.helpers.showInfoDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

private const val TAG = "ConfigurationActivity"
private const val PERMISSION_CODE_REQUEST_INTERNET = 1
private const val REQUEST_LOAD_LOGO = 123
private const val REQUEST_LOAD_FOOTER = 124
private const val REQUEST_LOAD_ODT = 125

class ConfigurationActivity:
    ArbeitsberichtActivity()
{
    private var internetPermissionContinuation: Continuation<Boolean>? = null
    lateinit var dataBinding: ActivityConfigurationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        if(!onCreateWrapper(savedInstanceState))
            return

        // Here we expect that the app initialization is done
        super.onCreate(savedInstanceState)
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_configuration)
        dataBinding.prefs = app.prefs

        requestedOrientation = when(prefs.lockScreenOrientation.value) {
            true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(prefs.crashlyticsEnable.value)

        when {
            prefs.useOdfOutput.value -> {
                findViewById<RadioButton>(R.id.radio_odf_output).toggle()
                findViewById<CardView>(R.id.odf_config_container).visibility = View.VISIBLE
                findViewById<CardView>(R.id.pdf_config_container).visibility = View.GONE
                findViewById<CardView>(R.id.xlsx_config_container).visibility = View.GONE
            }
            prefs.useXlsxOutput.value -> {
                findViewById<RadioButton>(R.id.radio_xlsx_output).toggle()
                findViewById<CardView>(R.id.odf_config_container).visibility = View.GONE
                findViewById<CardView>(R.id.pdf_config_container).visibility = View.GONE
                findViewById<CardView>(R.id.xlsx_config_container).visibility = View.VISIBLE
            }
            prefs.selectOutput.value -> {
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

        val pdfLogoWidthSlider = findViewById<Slider>(R.id.pdf_logo_width_slider)
        pdfLogoWidthSlider.value = prefs.pdfLogoWidthPercent.value.toFloat()
        val pdfLogoWidthText = findViewById<TextView>(R.id.pdf_logo_width_text)
        pdfLogoWidthText.text = "Breite ${prefs.pdfLogoWidthPercent.value.toString()}%"
        pdfLogoWidthSlider.addOnChangeListener { _, value, _ ->
            pdfLogoWidthText.text = "Breite ${value.toInt().toString()}%"
        }
        val pdfFooterWidthSlider = findViewById<Slider>(R.id.pdf_footer_width_slider)
        pdfFooterWidthSlider.value = prefs.pdfFooterWidthPercent.value.toFloat()
        val pdfFooterWidthText = findViewById<TextView>(R.id.pdf_footer_width_text)
        pdfFooterWidthText.text = "Breite ${prefs.pdfFooterWidthPercent.value.toString()}%"
        pdfFooterWidthSlider.addOnChangeListener { _, value, _ ->
            pdfFooterWidthText.text = "Breite ${value.toInt().toString()}%"
        }

        findViewById<MaterialButtonToggleGroup>(R.id.logo_alignment_group).check(when(prefs.pdfLogoAlignment.value) {
            AbPreferences.Alignment.CENTER -> R.id.logo_alignment_center
            AbPreferences.Alignment.RIGHT -> R.id.logo_alignment_right
            else -> R.id.logo_alignment_left
        })
        findViewById<MaterialButtonToggleGroup>(R.id.footer_alignment_group).check(when(prefs.pdfFooterAlignment.value) {
            AbPreferences.Alignment.CENTER -> R.id.footer_alignment_center
            AbPreferences.Alignment.RIGHT -> R.id.footer_alignment_right
            else -> R.id.footer_alignment_left
        })

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
        showFileInImageView(prefs.footerFile.value, R.id.footer_image)
        showFileInImageView(prefs.logoFile.value, R.id.logo_image)

        // TODO: move the combination of edit text and slider into a custom view
        // PDF font size
        val fontSizeSlider = findViewById<Slider>(R.id.fontsize_slider)
        val fontSizeEdit = findViewById<EditText>(R.id.fontsize_text)
        fontSizeSlider.value = prefs.fontSize.value.toFloat()
        fontSizeEdit.text.replace(0, fontSizeEdit.text.length, prefs.fontSize.value.toString())
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
        xlsxLogoWidthSlider.value = prefs.xlsxLogoWidth.value.toFloat()
        xlsxLogoWidthEdit.text.replace(0, xlsxLogoWidthEdit.text.length, prefs.xlsxLogoWidth.value.toString())
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
        xlsxFooterWidthSlider.value = prefs.xlsxFooterWidth.value.toFloat()
        xlsxFooterWidthEdit.text.replace(0, xlsxFooterWidthEdit.text.length, prefs.xlsxFooterWidth.value.toString())
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
        scalePhotosSlider.value = prefs.photoResolution.value.toFloat()
        scalePhotosEdit.text.replace(0, scalePhotosEdit.text.length, prefs.photoResolution.value.toString())
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
        scalePhotosSwitch.setOnCheckedChangeListener { button, b ->
            scalePhotosSlider.isEnabled = b
            scalePhotosEdit.isEnabled = b
        }
        if(!prefs.scalePhotos.value) {
            scalePhotosSlider.isEnabled = false
        }
        findViewById<MaterialToolbar>(R.id.configuration_activity_toolbar).setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.config_save_button -> {
                    save()
                    val intent = Intent(this@ConfigurationActivity, MainActivity::class.java).apply {}
                    startActivity(intent)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        save()
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

    private fun save() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(prefs.crashlyticsEnable.value)

        prefs.useOdfOutput.value = findViewById<RadioButton>(R.id.radio_odf_output).isChecked
        prefs.useXlsxOutput.value = findViewById<RadioButton>(R.id.radio_xlsx_output).isChecked
        prefs.selectOutput.value = findViewById<RadioButton>(R.id.radio_select_output).isChecked
        prefs.fontSize.value = findViewById<Slider>(R.id.fontsize_slider).value.toInt()
        prefs.xlsxLogoWidth.value = findViewById<Slider>(R.id.xlsx_logo_width_slider).value.toInt()
        prefs.xlsxFooterWidth.value = findViewById<Slider>(R.id.xlsx_footer_width_slider).value.toInt()
        prefs.photoResolution.value = findViewById<Slider>(R.id.scale_photos_slider).value.toInt()
        prefs.pdfLogoWidthPercent.value = findViewById<Slider>(R.id.pdf_logo_width_slider).value.toInt()
        prefs.pdfFooterWidthPercent.value = findViewById<Slider>(R.id.pdf_footer_width_slider).value.toInt()
        prefs.pdfLogoAlignment.value = when(findViewById<MaterialButtonToggleGroup>(R.id.logo_alignment_group).checkedButtonId) {
            R.id.logo_alignment_center -> AbPreferences.Alignment.CENTER
            R.id.logo_alignment_right -> AbPreferences.Alignment.RIGHT
            else -> AbPreferences.Alignment.LEFT
        }
        prefs.pdfFooterAlignment.value = when(findViewById<MaterialButtonToggleGroup>(R.id.footer_alignment_group).checkedButtonId) {
            R.id.footer_alignment_center -> AbPreferences.Alignment.CENTER
            R.id.footer_alignment_right -> AbPreferences.Alignment.RIGHT
            else -> AbPreferences.Alignment.LEFT
        }

        findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun onClickConnect(@Suppress("UNUSED_PARAMETER") btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            if(checkAndObtainInternetPermission()) {
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE
                val host = findViewById<EditText>(R.id.sftp_host).text.toString()
                val port = findViewById<EditText>(R.id.sftp_port).text.toString().toInt()
                val user = findViewById<EditText>(R.id.sftp_user).text.toString()
                var pwd = findViewById<EditText>(R.id.sftp_pwd).text.toString()
                if (pwd == "") pwd = prefs.sFtpPassword.value
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
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: ${this}")
        if(requestCode == REQUEST_LOAD_ODT && resultCode == RESULT_OK) {
            odtFileLoaded(data?.data)
        } else if(requestCode == REQUEST_LOAD_LOGO && resultCode == RESULT_OK) {
            logoFileLoaded(data?.data)
        } else if(requestCode == REQUEST_LOAD_FOOTER && resultCode == RESULT_OK) {
            footerFileLoaded(data?.data)
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

    fun onClickLoadOdt(@Suppress("UNUSED_PARAMETER") btn: View) {
        val intent = Intent()
            .setType("application/vnd.oasis.opendocument.text-template")
            .setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, getString(R.string.odf_file_choose)), REQUEST_LOAD_ODT)
    }

    private fun odtFileLoaded(file: Uri?) {
        if(file == null) {
            Log.d(TAG, "No ODF template file was selected")
        } else {
            Log.d(TAG, "Selected ODF template: ${file}")
            try {
                val dst = "custom_output_template.ott"
                copyUriToFile(file, dst)
                prefs.odfTemplateFile.value = dst
            } catch (e: Exception) {
                GlobalScope.launch(Dispatchers.Main) {
                    showInfoDialog(getString(R.string.getfile_error), this@ConfigurationActivity, e.message ?: getString(R.string.unknown))
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
                    if (pwd == "") pwd = prefs.sFtpPassword.value
                    val sftpProvider = SftpProvider(this@ConfigurationActivity)
                    sftpProvider.connect(user, pwd, host, port)
                    Log.d(TAG, "Connection success")
                    val src = findViewById<EditText>(R.id.odf_template_ftp_path).text.toString()
                    val dst = "custom_output_template.ott"
                    sftpProvider.copyFile(src, "${filesDir}/$dst")
                    sftpProvider.disconnect()
                    prefs.odfTemplateFile.value = dst
                } catch (e: Exception) {
                    showInfoDialog(getString(R.string.getfile_error), this@ConfigurationActivity, e.message?:getString(R.string.unknown))
                }
                findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }
    }

    fun onClickLoadLogo(@Suppress("UNUSED_PARAMETER") btn: View) {
        val mimetypes = arrayOf("image/jpeg", "image/png")
        val intent = Intent()
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
            .setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, getString(R.string.logo_file_choose)), REQUEST_LOAD_LOGO)
    }

    private fun logoFileLoaded(file: Uri?) {
        if(file == null) {
            Log.d(TAG, "No logo file was selected")
        } else {
            Log.d(TAG, "Selected logo: ${file}")
            try {
                val dst = "logo.jpg"
                copyUriToFile(file, dst)
                prefs.logoFile.value = dst
                prefs.logoRatio.value = showFileInImageView(dst, R.id.logo_image)
            } catch (e: Exception) {
                GlobalScope.launch(Dispatchers.Main) {
                    showInfoDialog(getString(R.string.getfile_error), this@ConfigurationActivity, e.message ?: getString(R.string.unknown))
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
                        if (pwd == "") pwd = prefs.sFtpPassword.value
                        val sftpProvider = SftpProvider(this@ConfigurationActivity)
                        sftpProvider.connect(user, pwd, host, port)
                        Log.d(TAG, "Connection success")
                        val dst = "logo.jpg"
                        sftpProvider.copyFile(src, "${filesDir}/$dst")
                        sftpProvider.disconnect()
                        prefs.logoFile.value = dst
                        prefs.logoRatio.value = showFileInImageView(dst, R.id.logo_image)
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
        val mimetypes = arrayOf("image/jpeg", "image/png")
        val intent = Intent()
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
            .setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, getString(R.string.logo_file_choose)), REQUEST_LOAD_FOOTER)
    }

    private fun footerFileLoaded(file: Uri?) {
        if (file == null) {
            Log.d(TAG, "No footer file was selected")
        } else {
            Log.d(TAG, "Selected footer: ${file}")
            try {
                val dst = "footer.jpg"
                copyUriToFile(file, dst)
                prefs.footerFile.value = dst
                prefs.footerRatio.value = showFileInImageView(dst, R.id.footer_image)
            } catch (e: Exception) {
                GlobalScope.launch(Dispatchers.Main) {
                    showInfoDialog(getString(R.string.getfile_error), this@ConfigurationActivity, e.message ?: getString(R.string.unknown))
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
                        if (pwd == "") pwd = prefs.sFtpPassword.value
                        val sftpProvider = SftpProvider(this@ConfigurationActivity)
                        sftpProvider.connect(user, pwd, host, port)
                        Log.d(TAG, "Connection success")
                        val dst = "footer.jpg"
                        sftpProvider.copyFile(src, "${filesDir}/$dst")
                        sftpProvider.disconnect()
                        prefs.footerFile.value = "footer.jpg"
                        prefs.footerRatio.value = showFileInImageView(dst, R.id.footer_image)
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
        // TODO: This seems to be buggy and just works because Android
        // removed the need to request internet permissions
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
