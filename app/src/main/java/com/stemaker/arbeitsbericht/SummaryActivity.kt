package com.stemaker.arbeitsbericht

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import com.github.gcacace.signaturepad.views.SignaturePad
import android.print.*
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.databinding.ActivitySummaryBinding
import com.stemaker.arbeitsbericht.helpers.HtmlReport
import com.stemaker.arbeitsbericht.helpers.showInfoDialog
import kotlinx.android.synthetic.main.activity_summary.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "SummaryActivity"

class SummaryActivity : AppCompatActivity() {
    lateinit var binding: ActivitySummaryBinding
    val signatureData = storageHandler().getReport().signatureData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_summary)
        binding.lifecycleOwner = this
        binding.signature = signatureData

        setSupportActionBar(findViewById(R.id.summary_activity_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.summary)

        if(signatureData.employeeSignatureSvg.value == "") {
            findViewById<SignaturePad>(R.id.employee_signature).visibility = View.VISIBLE
        } else {
            findViewById<ImageView>(R.id.employee_signature_view).visibility = View.VISIBLE
        }
        if(signatureData.clientSignatureSvg.value == "") {
            findViewById<SignaturePad>(R.id.client_signature).visibility = View.VISIBLE
        } else {
            findViewById<ImageView>(R.id.client_signature_view).visibility = View.VISIBLE
        }

        val html = HtmlReport.encodeReport(storageHandler().getReport(), false)
        val wv = findViewById(R.id.webview) as WebView
        wv.loadDataWithBaseURL("", html, "text/html", "UTF-8", "")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.summary_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.show_preview-> {
                preview()
                true
            }
            R.id.send_report -> {
                send()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun saveAndBackToMain() {
        saveSignatures()
        val intent = Intent(this, ReportEditorActivity::class.java).apply {}
        Log.d("Arbeitsbericht", "Switching to report editor activity")
        startActivity(intent)
    }

    override fun onBackPressed() {
        Log.d("Arbeitsbericht.SummaryActivity.onBackPressed", "called")
        saveAndBackToMain()
    }

    override fun onPause() {
        super.onPause()
        saveSignatures()
    }

    fun onClickClearEmployeeSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        Log.d("Arbeitsbericht.SummaryActivity.onClickClearEmployeeSignature", "called")
        val pad = findViewById<SignaturePad>(R.id.employee_signature)
        pad.visibility = View.VISIBLE
        pad.clear()
        findViewById<ImageView>(R.id.employee_signature_view).visibility = View.GONE
    }

    fun onClickClearClientSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        Log.d("Arbeitsbericht.SummaryActivity.onClickClearClientSignature", "called")
        val pad = findViewById<SignaturePad>(R.id.client_signature)
        pad.visibility = View.VISIBLE
        pad.clear()
        findViewById<ImageView>(R.id.client_signature_view).visibility = View.GONE
    }

    fun saveSignatures() {
        val eSig = findViewById<SignaturePad>(R.id.employee_signature)
        if(!eSig.isEmpty) {
            signatureData.employeeSignatureSvg.value = eSig.getSignatureSvg()
            Log.d("Arbeitsbericht", "Saving employee signature")
        }
        val cSig = findViewById<SignaturePad>(R.id.client_signature)
        if(!cSig.isEmpty) {
            signatureData.clientSignatureSvg.value = cSig.getSignatureSvg()
            Log.d("Arbeitsbericht", "Saving client signature")
        }
        storageHandler().saveActiveReportToFile(getApplicationContext())
    }

    fun preview() {
        Log.d(TAG, "Preview")
        saveSignatures()

        GlobalScope.launch(Dispatchers.Main) {
            var withAttachment = true
            val writeExternalStoragePermission = ContextCompat.checkSelfPermission(this@SummaryActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            // If we do not have external storage write permissions
            if(writeExternalStoragePermission!= PackageManager.PERMISSION_GRANTED) {
                // Request user to grant write external storage permission.
                Log.d(TAG, "Need to query user for permissions, starting coroutine")
                ActivityCompat.requestPermissions(
                    this@SummaryActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION
                )
                withAttachment = suspendCoroutine<Boolean> {
                    Log.d(TAG, "Coroutine: suspended")
                    pdfWritePermissionContinuation = it
                }
            }
            if(withAttachment) {
                val file = createReport()
                file?.also { showReport(it) }
            }
        }
    }

    var pdfWritePermissionContinuation: Continuation<Boolean>? = null
    fun send() {
        Log.d(TAG, "Entry")
        saveSignatures()

        GlobalScope.launch(Dispatchers.Main) {
            var withAttachment = true
            val writeExternalStoragePermission = ContextCompat.checkSelfPermission(this@SummaryActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            // If we do not have external storage write permissions
            if(writeExternalStoragePermission!= PackageManager.PERMISSION_GRANTED) {
                // Request user to grant write external storage permission.
                Log.d(TAG, "Need to query user for permissions, starting coroutine")
                ActivityCompat.requestPermissions(
                    this@SummaryActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION
                )
                withAttachment = suspendCoroutine<Boolean> {
                    Log.d(TAG, "Coroutine: suspended")
                    pdfWritePermissionContinuation = it
                }
            }
            var file: File? = null
            if(withAttachment)
                file = createReport()
            sendMail(file, storageHandler().getReport())
        }
    }

    suspend fun createReport(): File? {
        Log.d(TAG, "Creating report")
        var ret: File? = null
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        findViewById<LinearLayout>(R.id.create_report_progress_container).visibility = View.VISIBLE
        try {
            when {
                configuration().useOdfOutput -> ret = createOdfReport()
                else -> ret = createPdfReport()
            }
        } catch (e:Exception) {
            showInfoDialog(getString(R.string.report_create_fail), this@SummaryActivity, e.message?:getString(R.string.unknown))
        }
        findViewById<LinearLayout>(R.id.create_report_progress_container).visibility = View.GONE
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        return ret
    }

    suspend fun createOdfReport(): File? {
        val report = storageHandler().getReport()
        val odfGenerator = OdfGenerator(this@SummaryActivity, report, create_report_progressbar, create_report_progress_text)
        val files = odfGenerator.getFilesForOdfGeneration()
        if(files != null) {
            if(odfGenerator.isOdfUpToDate(files[0], report)) {
                return files[0]
            } else {
                // We need to create the ODF then send
                Log.d(TAG, "creating odf")
                report.signatureData.clientSignaturePngFile = files[1]
                report.signatureData.employeeSignaturePngFile = files[2]
                createSigPngs(files[1], files[2])
                odfGenerator.create(files[0], files[1], files[2])
                return files[0]
            }
        } else {
            Log.d(TAG, "dropping odf generation")
            return null
        }
    }

    suspend fun createPdfReport(): File? {
        val report = storageHandler().getReport()
        val pdfPrint = PdfPrint(this, report)
        val files = pdfPrint.getFilesForPdfGeneration(this@SummaryActivity)
        if(files != null) {
            val pdfFile = files[0]
            Log.d(TAG, "creating pdf")
            report.signatureData.clientSignaturePngFile = files[1]
            report.signatureData.employeeSignaturePngFile = files[2]
            createSigPngs(files[1], files[2])
            pdfPrint.print(pdfFile)
            return files[0]
        } else {
            Log.d(TAG, "dropping pdf generation")
            return null
        }
    }

    fun createSigPngs(cSigFile: File, eSigFile: File) {
        val cSigPad = findViewById<SignaturePad>(R.id.client_signature)
        val eSigPad = findViewById<SignaturePad>(R.id.employee_signature)
        val cSigV = findViewById<ImageView>(R.id.client_signature_view)
        val eSigV = findViewById<ImageView>(R.id.employee_signature_view)

        val vis = arrayOf(cSigPad.visibility, eSigPad.visibility, cSigV.visibility, eSigV.visibility)
        cSigPad.visibility = View.GONE
        eSigPad.visibility = View.GONE
        cSigV.visibility = View.VISIBLE
        eSigV.visibility = View.VISIBLE

        cSigV.drawable?.toBitmap().apply {
            val cSigfOut = FileOutputStream(cSigFile)
            this?.compress(Bitmap.CompressFormat.PNG, 85, cSigfOut)
            cSigfOut.flush()
            cSigfOut.close()
        }

        eSigV.drawable?.toBitmap().apply {
            val eSigfOut = FileOutputStream(eSigFile)
            this?.compress(Bitmap.CompressFormat.PNG, 85, eSigfOut)
            eSigfOut.flush()
            eSigfOut.close()
        }
        cSigPad.visibility = vis[0]
        eSigPad.visibility = vis[1]
        cSigV.visibility = vis[2]
        eSigV.visibility = vis[3]
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Arbeitsbericht", "Access to external storage granted")
                pdfWritePermissionContinuation!!.resume(true)
            } else {
                Log.d("Arbeitsbericht", "Access to external storage denied")
                val toast = Toast.makeText(this, "Berechtigungen abgelehnt, PDF Bericht kann nicht erstellt werden", Toast.LENGTH_LONG)
                toast.show()
                pdfWritePermissionContinuation!!.resume(false)
            }
        }
    }

    fun showReport(file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val uri = FileProvider.getUriForFile(this, "com.android.stemaker.arbeitsbericht.fileprovider", file)
        val extension = file.name.substring(file.name.lastIndexOf(".")+1)
        when(extension) {
            "odt" -> intent.setDataAndType(uri, "application/vnd.oasis.opendocument.text")
            "pdf" -> intent.setDataAndType(uri, "application/pdf")
            else -> return
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            GlobalScope.launch(Dispatchers.Main) {
                showInfoDialog(getString(R.string.no_xdf_viewer, extension.capitalize()), this@SummaryActivity)
            }
        }
    }

    fun sendMail(xdfFile: File?, report: ReportData) {
        val subj = "Arbeitsbericht von ${configuration().employeeName}: Kunde: ${report.project.name.value}, Berichtsnr: ${report.id}"
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:" + configuration().recvMail)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subj)
        if(xdfFile != null) {
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Bericht im Anhang")
            emailIntent .putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+xdfFile.path))
            Log.d("Arbeitsbericht", "Added attachement file://${xdfFile.path}")
        } else {
            emailIntent.putExtra(Intent.EXTRA_TEXT, HtmlReport.encodeReport(report, true))
            Log.d("Arbeitsbericht", "No attachement")
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."))
        } catch (ex: android.content.ActivityNotFoundException) {
            GlobalScope.launch(Dispatchers.Main) {
                showInfoDialog(getString(R.string.no_mail_client), this@SummaryActivity)
            }
        }
    }

    companion object {
        const val REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1
    }
}
