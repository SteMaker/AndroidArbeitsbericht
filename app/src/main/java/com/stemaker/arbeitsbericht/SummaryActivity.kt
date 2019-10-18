package com.stemaker.arbeitsbericht

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.WebView
import com.github.gcacace.signaturepad.views.SignaturePad
import android.print.*
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.databinding.ActivitySummaryBinding
import com.stemaker.arbeitsbericht.helpers.HtmlReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class SummaryActivity : AppCompatActivity(), PdfPrint.PdfPrintFinishedCallback {
    lateinit var binding: ActivitySummaryBinding
    val signatureData = storageHandler().getReport().signatureData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_summary)
        binding.lifecycleOwner = this
        binding.signature = signatureData

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

    fun onClickBack(@Suppress("UNUSED_PARAMETER") backButton: View) {
        Log.d("Arbeitsbericht.ReportEditorActivity.onClickBack", "called")
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
    }

    var pdfWritePermissionContinuation: Continuation<Boolean>? = null
    fun onClickSend(@Suppress("UNUSED_PARAMETER") sendButton: View) {
        Log.d("Arbeitsbericht.SummaryActivity.onClickSend", "Entry")
        saveSignatures()

        GlobalScope.launch(Dispatchers.Main) {
            var withPdf = true
            val writeExternalStoragePermission = ContextCompat.checkSelfPermission(this@SummaryActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            // If we do not have external storage write permissions
            if(writeExternalStoragePermission!= PackageManager.PERMISSION_GRANTED) {
                // Request user to grant write external storage permission.
                Log.d("Arbeitsbericht.SummaryActivity.onClickSend", "Need to query user for permissions, starting coroutine")
                ActivityCompat.requestPermissions(
                    this@SummaryActivity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION
                );
                withPdf = suspendCoroutine<Boolean> {
                    Log.d("Arbeitsbericht.SummaryActivity.onClickSend", "Coroutine: suspended")
                    pdfWritePermissionContinuation = it
                }
            }
            createAndSendReport(withPdf)
        }
    }

    suspend fun createAndSendReport(withPdf: Boolean) {
        Log.d("Arbeitsbericht.SummaryActivity.createAndSendReport", "Creating report ${if(withPdf) "with PDF" else "without PDF"}")
        val report = storageHandler().getReport()
        if (withPdf) {
            val pdfPrint = PdfPrint(this, report)
            val pdfFile = pdfPrint.getFileForPdfGeneration(this@SummaryActivity)
            if(pdfFile != null) {
                Log.d("Arbeitsbericht.SummaryActivity.createAndSendReport", "creating pdf")
                pdfPrint.print(pdfFile)
                sendMail(pdfFile, report)
            } else {
                Log.d("Arbeitsbericht.SummaryActivity.createAndSendReport", "dropping pdf generation")
                sendMail(null, report)
            }
        } else {
            sendMail(null, report)
        }
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

    fun sendMail(pdfFile: File?, report: ReportData) {
        val subj = "Arbeitsbericht von ${storageHandler().configuration.employeeName}: Kunde: ${report.project.name}, Berichtsnr: ${report.id}"
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:" + storageHandler().configuration.recvMail)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subj)
        emailIntent.putExtra(Intent.EXTRA_TEXT, HtmlReport.encodeReport(report, true))
        if(pdfFile != null) {
            emailIntent .putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+pdfFile.path))
            Log.d("Arbeitsbericht", "Added attachement file://${pdfFile.path}")
        } else {
            Log.d("Arbeitsbericht", "No attachement")
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."))
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(this@SummaryActivity, "No email clients installed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun pdfPrintFinishedCallback(pdfFile: File) {
        Log.d("Arbeitsbericht", "Print finished")
        sendMail(pdfFile, storageHandler().getReport())
    }

    companion object {
        const val REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1
    }
}
