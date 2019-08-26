package com.stemaker.arbeitsbericht

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.R.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.WebView
import com.github.gcacace.signaturepad.views.SignaturePad
import android.graphics.drawable.PictureDrawable
import android.os.CancellationSignal
import android.widget.ImageView
import com.caverock.androidsvg.SVG
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.print.*
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import java.io.File
import java.io.IOException


class SummaryActivity : AppCompatActivity(), PdfPrint.PdfPrintFinishedCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val html = HtmlReport.encodeReport(StorageHandler.getReport(), false)
        val wv = findViewById(R.id.webview) as WebView
        wv.loadDataWithBaseURL("", html, "text/html", "UTF-8", "")

        if(StorageHandler.getReport().employee_signature != "") {
            val svg = SVG.getFromString(StorageHandler.getReport().employee_signature)
            val pd = PictureDrawable(svg.renderToPicture())
            val sigIV = findViewById<ImageView>(R.id.employee_signature_view)
            sigIV.setImageDrawable(pd)
            sigIV.visibility = View.VISIBLE
        } else {
            val sigPad = findViewById<SignaturePad>(R.id.employee_signature)
            sigPad.visibility = View.VISIBLE
        }
        if(StorageHandler.getReport().client_signature != "") {
            val svg = SVG.getFromString(StorageHandler.getReport().client_signature)
            val pd = PictureDrawable(svg.renderToPicture())
            val sigIV = findViewById<ImageView>(R.id.client_signature_view)
            sigIV.setImageDrawable(pd)
            sigIV.visibility = View.VISIBLE
        } else {
            val sigPad = findViewById<SignaturePad>(R.id.client_signature)
            sigPad.visibility = View.VISIBLE
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

    fun onClickBack(@Suppress("UNUSED_PARAMETER") backButton: View) {
        Log.d("Arbeitsbericht.ReportEditorActivity.onClickBack", "called")
        saveAndBackToMain()
    }

    fun onClickClearEmployeeSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        Log.d("Arbeitsbericht.SummaryActivity.onClickClearEmployeeSignature", "called")
        val sigPad = findViewById<SignaturePad>(R.id.employee_signature)
        if(sigPad.visibility == View.VISIBLE) {
            sigPad.clear()
        } else {
            StorageHandler.getReport().employee_signature = ""
            findViewById<ImageView>(R.id.employee_signature_view).visibility = View.GONE
            sigPad.visibility = View.VISIBLE
        }
    }

    fun onClickClearClientSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        Log.d("Arbeitsbericht.SummaryActivity.onClickClearClientSignature", "called")
        val sigPad = findViewById<SignaturePad>(R.id.client_signature)
        if(sigPad.visibility == View.VISIBLE) {
            Log.d("Arbeitsbericht.SummaryActivity.onClickClearClientSignature", "Clearing pad")
            sigPad.clear()
        } else {
            Log.d("Arbeitsbericht.SummaryActivity.onClickClearClientSignature", "Re-enabling pad")
            StorageHandler.getReport().client_signature = ""
            findViewById<ImageView>(R.id.client_signature_view).visibility = View.GONE
            sigPad.visibility = View.VISIBLE
        }
    }

    fun saveSignatures() {
        val cSig = findViewById(R.id.client_signature) as SignaturePad
        if(!cSig.isEmpty) {
            StorageHandler.getReport().client_signature = cSig.getSignatureSvg()
            Log.d("Arbeitsbericht", "Saving client signature")
        }
        val eSig = findViewById(R.id.employee_signature) as SignaturePad
        if(!eSig.isEmpty) {
            StorageHandler.getReport().employee_signature = eSig.getSignatureSvg()
            Log.d("Arbeitsbericht", "Saving employee signature")
        }
    }

    fun onClickSend(@Suppress("UNUSED_PARAMETER") sendButton: View) {
        saveSignatures()

        val writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // If do not grant write external storage permission.
        if(writeExternalStoragePermission!= PackageManager.PERMISSION_GRANTED)
        {
            // Request user to grant write external storage permission.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        } else {
            Log.d("Arbeitsbericht", "Permissions granted")
            createAndSendReport(true)
        }
    }

    fun createAndSendReport(withPdf: Boolean) {
        val report = StorageHandler.getReport()
        if (withPdf) {
            val pdfPrint = PdfPrint(this, report)
            if(pdfPrint.print()) {
                Log.d("Arbeitsbericht", "pdf")
            } else {
                Log.d("Arbeitsbericht", "no pdf")
                sendMail(null, report)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Arbeitsbericht", "Access to external storage granted")
                createAndSendReport(true)
            } else {
                Log.d("Arbeitsbericht", "Access to external storage denied")
                val toast = Toast.makeText(this, "Berechtigungen abgelehnt, PDF Bericht kann nicht erstellt werden", Toast.LENGTH_LONG)
                toast.show()
                createAndSendReport(false)
            }
        }
    }

    fun sendMail(pdfFile: File?, report: Report) {
        val subj = "Arbeitsbericht von ${StorageHandler.configuration.employeeName}: Kunde: ${report.client_name}, Berichtsnr: ${report.id}"
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:" + StorageHandler.configuration.recvMail)
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
        sendMail(pdfFile, StorageHandler.getReport())
    }

    companion object {
        const val REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1
    }
}
