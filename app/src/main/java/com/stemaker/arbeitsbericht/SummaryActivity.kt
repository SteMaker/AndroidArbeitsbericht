package com.stemaker.arbeitsbericht

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.print.*
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.stemaker.arbeitsbericht.data.configuration.configuration
import com.stemaker.arbeitsbericht.data.report.ReportData
import com.stemaker.arbeitsbericht.databinding.ActivitySummaryBinding
import com.stemaker.arbeitsbericht.helpers.*
import com.stemaker.arbeitsbericht.output.OdfGenerator
import com.stemaker.arbeitsbericht.view_models.SignatureViewModel
import com.stemaker.arbeitsbericht.view_models.SignatureViewModelFactory
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "SummaryActivity"


class SummaryActivity:
    ArbeitsberichtActivity()
{
    lateinit var binding: ActivitySummaryBinding
    lateinit var viewModel: SignatureViewModel
    lateinit var report: ReportData

    override fun onCreate(savedInstanceState: Bundle?) {
        if(!onCreateWrapper(savedInstanceState))
            return

        // Here we expect that the app initialization is done
        super.onCreate(savedInstanceState)
        report = app.reportRepo.activeReport
        binding = DataBindingUtil.setContentView(this, R.layout.activity_summary)
        binding.lifecycleOwner = this

        requestedOrientation = when(configuration().lockScreenOrientation) {
            true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        val signatureData = report.signatureData
        viewModel = ViewModelProvider(this, SignatureViewModelFactory(this, signatureData)).get(SignatureViewModel::class.java)

        val eSigPad = binding.employeeSignature
        if (viewModel.isEmployeeSignatureDefined()) {
            eSigPad.setSvg(viewModel.getEmployeeSignatureSvg())
            eSigPad.locked = true
            binding.lockEmployeeSignatureBtn.isEnabled = false
        }

        val cSigPad = binding.clientSignature
        if (viewModel.isClientSignatureDefined()) {
            cSigPad.setSvg(viewModel.getClientSignatureSvg())
            cSigPad.locked = true
            binding.lockClientSignatureBtn.isEnabled = false
        }
        binding.summaryActivityToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.show_preview-> {
                    prepareAndShare(false) {
                            file, type -> file?.also { showReport(file, type) }
                    }
                    true
                }
                R.id.send_report -> {
                    prepareAndShare(true) {
                            file, _ -> sendMail(file, report)
                    }
                    true
                }
                R.id.share_report -> {
                    prepareAndShare(true) {
                            file, _ ->
                        if (file != null) {
                            shareReport(file, report)
                        } else {
                            val toast = Toast.makeText(applicationContext, R.string.send_fail, Toast.LENGTH_LONG)
                            toast.show()
                        }
                    }
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
        binding.summaryActivityToolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val html = HtmlReport.encodeReport(report, this@SummaryActivity.filesDir, false)
        binding.webview.loadDataWithBaseURL("", html, "text/html", "UTF-8", "")

        // In landscape limiting the width of the signature pad to the max width possible in portrait,
        // to make sure the same area is accessible in both
        // @TODO getRealSize was deprecated in Android12, need to find the correct way to handle this
        val size = Point()
        getDisplaySize(size)
        if(size.x > size.y) {
            val percent = size.y.toFloat()/size.x.toFloat()
            binding.guidelineEmployeeSigpadWidth?.setGuidelinePercent(percent)
            binding.guidelineClientSigpadWidth?.setGuidelinePercent(1.0f-percent)
        }
    }

    override fun onBackPressed() {
        Log.d(TAG, "called")
        GlobalScope.launch(Dispatchers.Main) {
            saveSignatures()
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        runBlocking {
            saveSignatures()
        }
    }

    private fun getDisplaySize(size: Point) {
        val wm = this.getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = wm.currentWindowMetrics
            val windowInsets: WindowInsets = windowMetrics.windowInsets

            val insets = windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
            val insetsWidth = insets.right + insets.left
            val insetsHeight = insets.top + insets.bottom

            val b = windowMetrics.bounds
            size.x = b.width() - insetsWidth
            size.y = b.height() - insetsHeight
        } else {
            val display = wm.defaultDisplay // deprecated in API 30
            display?.getSize(size) // deprecated in API 30
        }
    }
    fun onClickClearEmployeeSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        Log.d(TAG, "called")
        val pad = binding.employeeSignature
        if(pad.isEmpty)
            return
        GlobalScope.launch(Dispatchers.Main) {
            val answer =
                showConfirmationDialog(getString(R.string.del_signature), btn.context)
            if (answer == AlertDialog.BUTTON_POSITIVE) {
                pad.clear()
                pad.locked = false
                binding.lockEmployeeSignatureBtn.isEnabled = true
                viewModel.clearEmployeeSignature()
            } else {
                Log.d(TAG, "cancelled deleting")
            }
        }
    }

    private fun lockEmployeeSignature() {
        val pad = binding.employeeSignature
        if(!pad.isEmpty && !pad.locked) {
            viewModel.setEmployeeSignatureSvg(pad.signatureSvg)
            pad.setSvg(viewModel.getEmployeeSignatureSvg())
            binding.lockEmployeeSignatureBtn.isEnabled = false
            pad.locked = true
        }
    }

    fun onClickLockEmployeeSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        lockEmployeeSignature()
    }

    fun onClickShowEmployeeSignature(@Suppress("UNUSED_PARAMETER") b: View) {
        if(binding.employeeSignatureCard.visibility == View.GONE) {
            binding.employeeSignatureCard.visibility = View.VISIBLE
            binding.clientSignatureCard.visibility = View.GONE
        } else {
            binding.employeeSignatureCard.visibility = View.GONE
        }
    }

    fun onClickHideEmployeeSignature(@Suppress("UNUSED_PARAMETER") b: View) {
        binding.employeeSignatureCard.visibility = View.GONE
    }

    fun onClickClearClientSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        Log.d(TAG, "called")
        val pad = binding.clientSignature
        if(pad.isEmpty)
            return
        GlobalScope.launch(Dispatchers.Main) {
            val answer =
                showConfirmationDialog(getString(R.string.del_signature), btn.context)
            if (answer == AlertDialog.BUTTON_POSITIVE) {
                pad.clear()
                pad.locked = false
                binding.lockClientSignatureBtn.isEnabled = true
                viewModel.clearClientSignature()
            } else {
                Log.d(TAG, "cancelled deleting")
            }
        }
    }

    private fun lockClientSignature() {
        val pad = binding.clientSignature
        if(!pad.isEmpty && !pad.locked) {
            viewModel.setClientSignatureSvg(pad.signatureSvg)
            pad.setSvg(viewModel.getClientSignatureSvg())
            binding.lockClientSignatureBtn.isEnabled = false
            pad.locked = true
        }
    }

    fun onClickLockClientSignature(@Suppress("UNUSED_PARAMETER") btn: View) {
        lockClientSignature()
    }

    fun onClickShowClientSignature(@Suppress("UNUSED_PARAMETER") b: View) {
        if(binding.clientSignatureCard.visibility == View.GONE) {
            binding.clientSignatureCard.visibility = View.VISIBLE
            binding.employeeSignatureCard.visibility = View.GONE
        } else {
            binding.clientSignatureCard.visibility = View.GONE
        }
    }

    fun onClickHideClientSignature(@Suppress("UNUSED_PARAMETER") b: View) {
        binding.clientSignatureCard.visibility = View.GONE
    }

    private fun saveSignatures() {
        lockEmployeeSignature()
        lockClientSignature()
        app.reportRepo.saveReport(report)
    }

    var writePermissionContinuation: Continuation<Boolean>? = null
    private fun prepareAndShare(ask: Boolean, distribute: (file: File?, type: OutputType)->Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            saveSignatures()
            if(ask)
                askAndSetDone()

            var permissionGranted = true
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
                permissionGranted = suspendCoroutine<Boolean> {
                    Log.d(TAG, "Coroutine: suspended")
                    writePermissionContinuation = it
                }
            }
            if(permissionGranted) {
                val fileAndType = createReport()
                distribute(fileAndType.first, fileAndType.second)
            }
        }

    }

    private suspend fun askAndSetDone() {
        if(report.state.value == ReportData.ReportState.DONE) return
        val answer =
            showConfirmationDialog("Soll der Bericht auf Erledigt gesetzt werden?", this@SummaryActivity)
        if(answer == AlertDialog.BUTTON_POSITIVE) {
            report.state.value = ReportData.ReportState.DONE
            app.reportRepo.saveReport(report)
        }
    }

    // TODO: Currently only supporting a single output file, currently no other use case
    private suspend fun createReport(): Pair<File?, OutputType> {
        val type = when {
            configuration().useXlsxOutput -> OutputType.XLSX
            configuration().useOdfOutput -> OutputType.ODT
            configuration().selectOutput -> showOutputSelectDialog(this@SummaryActivity)
            else -> OutputType.PDF
        }
        if(type == OutputType.UNKNOWN)
            return Pair(null, type)

        val generator = when(type) {
            // FIXME: Add support for select !!!!
            OutputType.XLSX -> XlsxGenerator(this@SummaryActivity, report, binding.createReportProgressbar,
                binding.createReportProgressText)
            OutputType.ODT -> OdfGenerator(this@SummaryActivity, report, binding.createReportProgressbar,
                binding.createReportProgressText)
            else -> PdfGenerator(this@SummaryActivity, report, binding.createReportProgressbar,
                binding.createReportProgressText)
        }
        val files = generator.getFilesForGeneration()
        val file = if(files != null) {
            if(generator.isDocUpToDate(files)) {
                files[0]
            } else {
                // We need to create the doc then send
                report.signatureData.clientSignaturePngFile = files[1]
                report.signatureData.employeeSignaturePngFile = files[2]
                createSigPngs(files[1], files[2])
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                binding.createReportProgressContainer.visibility = View.VISIBLE
                if(!generator.create(files)) {
                    val toast = Toast.makeText(this, "Entschuldigung, es ist ein Fehler beim Erstellen des Berichts aufgetreten", Toast.LENGTH_LONG)
                    toast.show()
                }
                binding.createReportProgressContainer.visibility = View.GONE
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                files[0]
            }
        } else {
            Log.d(TAG, "dropping odf generation")
            null
        }
        return Pair(file, type)
    }

    private fun createSigPngs(cSigFile: File, eSigFile: File) {
        val sigPadC = binding.clientSignature
        val sigPadCV = sigPadC.visibility
        sigPadC.visibility = View.VISIBLE
        sigPadC.saveBitmapToFile(cSigFile)
        sigPadC.visibility = sigPadCV

        val sigPadE = binding.employeeSignature
        val sigPadEV = sigPadE.visibility
        sigPadE.visibility = View.VISIBLE
        sigPadE.saveBitmapToFile(eSigFile)
        sigPadE.visibility = sigPadEV
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Access to external storage granted")
                writePermissionContinuation?.resume(true)
            } else {
                Log.d(TAG, "Access to external storage denied")
                val toast = Toast.makeText(this, "Berechtigungen abgelehnt, PDF Bericht kann nicht erstellt werden", Toast.LENGTH_LONG)
                toast.show()
                writePermissionContinuation?.resume(false)
            }
        }
    }

    private fun showReportExternal(file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val uri = FileProvider.getUriForFile(this, "com.stemaker.arbeitsbericht.fileprovider", file)
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.toString())
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        intent.setDataAndType(uri, mimeType)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            GlobalScope.launch(Dispatchers.Main) {
                showInfoDialog(getString(R.string.no_xdf_viewer, extension.capitalize()), this@SummaryActivity)
            }
        }
    }

    private fun showReportInternal(file: File, type: OutputType) {
        val pdfPreviewDialog = PdfPreviewDialog()
        pdfPreviewDialog.file = file
        pdfPreviewDialog.show(supportFragmentManager, "PdfPreviewDialog")
    }

    private fun showReport(file: File, type: OutputType) {
        if(configuration().useInlinePdfViewer && type == OutputType.PDF)
            showReportInternal(file, type)
        else
            showReportExternal(file)
    }

    private fun sendMail(xdfFile: File?, report: ReportData) {
        val fileUri: Uri? = xdfFile?.let {
            try {
                FileProvider.getUriForFile(
                    this@SummaryActivity,
                    "com.stemaker.arbeitsbericht.fileprovider",
                    xdfFile
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "The selected file can't be shared: $xdfFile")
                GlobalScope.launch(Dispatchers.Main) {
                    showInfoDialog(getString(R.string.send_fail), this@SummaryActivity)
                }
                return
            }
        }
        val subj = "Arbeitsbericht von ${configuration().employeeName}: Kunde: ${report.project.name.value}, Berichtsnr: ${report.id.value}"
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:" + configuration().recvMail)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, subj)
            fileUri?.let {
                putExtra(Intent.EXTRA_TEXT, "Bericht im Anhang")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                clipData = ClipData.newUri(this@SummaryActivity.contentResolver, xdfFile?.name?:"file", it)
            } ?: run {
                putExtra(Intent.EXTRA_TEXT, HtmlReport.encodeReport(report, this@SummaryActivity.filesDir, true))
            }
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."))
        } catch (ex: android.content.ActivityNotFoundException) {
            GlobalScope.launch(Dispatchers.Main) {
                showInfoDialog(getString(R.string.no_mail_client), this@SummaryActivity)
            }
        }
    }

    private fun shareReport(xdfFile: File, report: ReportData) {
        val fileUri: Uri? = try {
            FileProvider.getUriForFile(
                this@SummaryActivity,
                "com.stemaker.arbeitsbericht.fileprovider",
                xdfFile)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "The selected file can't be shared: $xdfFile")
            GlobalScope.launch(Dispatchers.Main) {
                showInfoDialog(getString(R.string.send_fail), this@SummaryActivity)
            }
            return
        }

        val subj = "Arbeitsbericht von ${configuration().employeeName}: Kunde: ${report.project.name.value}, Berichtsnr: ${report.id.value}"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = when(configuration().useOdfOutput) {
                true -> "application/vnd.oasis.opendocument.text"
                else -> "application/pdf"
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, subj)
            putExtra(Intent.EXTRA_TEXT, "Bericht im Anhang")
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_EMAIL, arrayOf(configuration().recvMail))
        }

        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.sharing)))
        } catch (ex: android.content.ActivityNotFoundException) {
            GlobalScope.launch(Dispatchers.Main) {
                showInfoDialog(getString(R.string.send_fail), this@SummaryActivity)
            }
        }
    }

    companion object {
        const val REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1
    }
}
