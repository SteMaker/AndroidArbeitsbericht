package com.stemaker.arbeitsbericht

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import com.github.gcacace.signaturepad.views.SignaturePad
import android.print.*
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import com.caverock.androidsvg.SVG
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.databinding.ActivitySummaryBinding
import com.stemaker.arbeitsbericht.helpers.HtmlReport
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
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

        val eSigPad = findViewById<LockableSignaturePad>(R.id.employee_signature)
        if(signatureData.employeeSignatureSvg.value!! != "") {
            Log.d(TAG, "create eSig svg ${signatureData.employeeSignatureSvg.value!!.length}")
            eSigPad.setSvg(signatureData.employeeSignatureSvg.value!!)
            eSigPad.locked = true
            val lockBtn = findViewById<ImageButton>(R.id.lock_employee_signature_btn)
            lockBtn!!.setEnabled(false)
            lockBtn.setImageResource(R.drawable.ic_lock_grey_24)
        }
        val cSigPad = findViewById<LockableSignaturePad>(R.id.client_signature)
        if(signatureData.clientSignatureSvg.value!! != "") {
            cSigPad.setSvg(signatureData.clientSignatureSvg.value!!)
            cSigPad.locked = true
            val lockBtn = findViewById<ImageButton>(R.id.lock_client_signature_btn)
            lockBtn!!.setEnabled(false)
            lockBtn.setImageResource(R.drawable.ic_lock_grey_24)
        }
        /* Make headline text area of signature also clickable */
        val employeeSigText = findViewById<TextView>(R.id.employee_signature_text)
        employeeSigText!!.setOnClickListener(object: View.OnClickListener {
            override fun onClick(cV: View) {
                onClickHideShowEmployeeSignature(findViewById<ImageButton>(R.id.hide_employee_signature_btn))
            }
        })
        val clientSigText = findViewById<TextView>(R.id.client_signature_text)
        clientSigText!!.setOnClickListener(object: View.OnClickListener {
            override fun onClick(cV: View) {
                onClickHideShowClientSignature(findViewById<ImageButton>(R.id.hide_client_signature_btn))
            }
        })

        val html = HtmlReport.encodeReport(storageHandler().getReport(), false)
        val wv = findViewById<WebView>(R.id.webview)
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
        val pad = findViewById<LockableSignaturePad>(R.id.employee_signature)
        if(pad.isEmpty())
            return
        GlobalScope.launch(Dispatchers.Main) {
            val answer =
                showConfirmationDialog(getString(R.string.del_signature), btn.context)
            if (answer == AlertDialog.BUTTON_POSITIVE) {
                pad.clear()
                pad.locked = false
                val lockBtn = findViewById<ImageButton>(R.id.lock_employee_signature_btn)
                lockBtn!!.setEnabled(true)
                lockBtn.setImageResource(R.drawable.ic_lock_black_24)
                signatureData.employeeSignatureSvg.value = ""
            } else {
                Log.d("Arbeitsbericht.SummaryActivity.onClickClearEmployeeSignature", "cancelled deleting")
            }
        }
    }

    fun lockEmployeeSignature() {
        val pad = findViewById<LockableSignaturePad>(R.id.employee_signature)
        if(!pad.isEmpty && !pad.locked) {
            signatureData.employeeSignatureSvg.value = pad.signatureSvg
            pad.setSvg(signatureData.employeeSignatureSvg.value!!)
            val lockBtn = findViewById<ImageButton>(R.id.lock_employee_signature_btn)
            lockBtn!!.setEnabled(false)
            lockBtn.setImageResource(R.drawable.ic_lock_grey_24)
            pad.locked = true
        }
    }

    fun onClickLockEmployeeSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        lockEmployeeSignature()
    }

    fun onClickHideShowEmployeeSignature(@Suppress("UNUSED_PARAMETER") b: View) {
        val btn = b as ImageButton
        val sigPad = findViewById<LockableSignaturePad>(R.id.employee_signature)
        if(sigPad!!.visibility == View.VISIBLE) {
            sigPad.visibility = View.GONE
            btn.rotation = 180F
        } else {
            sigPad.visibility = View.VISIBLE
            btn.rotation = 0F
        }
    }

    fun onClickClearClientSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        Log.d("Arbeitsbericht.SummaryActivity.onClickClearClientSignature", "called")
        val pad = findViewById<LockableSignaturePad>(R.id.client_signature)
        if(pad.isEmpty())
            return
        GlobalScope.launch(Dispatchers.Main) {
            val answer =
                showConfirmationDialog(getString(R.string.del_signature), btn.context)
            if (answer == AlertDialog.BUTTON_POSITIVE) {
                pad.clear()
                pad.locked = false
                val lockBtn = findViewById<ImageButton>(R.id.lock_client_signature_btn)
                lockBtn.setEnabled(true)
                lockBtn!!.setImageResource(R.drawable.ic_lock_black_24)
                signatureData.clientSignatureSvg.value = ""
            } else {
                Log.d("Arbeitsbericht.SummaryActivity.onClickClearClientSignature", "cancelled deleting")
            }
        }
    }

    fun lockClientSignature() {
        val pad = findViewById<LockableSignaturePad>(R.id.client_signature)
        if(!pad.isEmpty && !pad.locked) {
            signatureData.clientSignatureSvg.value = pad.signatureSvg
            pad.setSvg(signatureData.clientSignatureSvg.value!!)
            val lockBtn = findViewById<ImageButton>(R.id.lock_client_signature_btn)
            lockBtn!!.setEnabled(false)
            lockBtn.setImageResource(R.drawable.ic_lock_grey_24)
            pad.locked = true
        }
    }

    fun onClickLockClientSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        lockClientSignature()
    }

    fun onClickHideShowClientSignature(@Suppress("UNUSED_PARAMETER") b: View) {
        val btn = b as ImageButton
        val sigPad = findViewById<LockableSignaturePad>(R.id.client_signature)
        if(sigPad!!.visibility == View.VISIBLE) {
            sigPad.visibility = View.GONE
            btn.rotation = 180F
        } else {
            sigPad.visibility = View.VISIBLE
            btn.rotation = 0F
        }
    }

    fun saveSignatures() {
        lockEmployeeSignature()
        lockClientSignature()
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
        val sigPadC = findViewById<LockableSignaturePad>(R.id.client_signature)
        val sigPadCV = sigPadC!!.visibility
        sigPadC.visibility = View.VISIBLE
        client_signature.saveBitmapToFile(cSigFile)
        sigPadC.visibility = sigPadCV

        val sigPadE = findViewById<LockableSignaturePad>(R.id.employee_signature)
        val sigPadEV = sigPadE!!.visibility
        sigPadE.visibility = View.VISIBLE
        employee_signature.saveBitmapToFile(eSigFile)
        sigPadE.visibility = sigPadEV
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
