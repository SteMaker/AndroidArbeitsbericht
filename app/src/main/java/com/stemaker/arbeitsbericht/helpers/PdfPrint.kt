package android.print

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.helpers.HtmlReport
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import java.io.File
import java.io.IOException

class PdfPrint(val activity: Activity, val report: ReportData) {

    interface PdfPrintFinishedCallback {
        fun pdfPrintFinishedCallback(pdfFile: File)
    }

    val jobName = "pdf_print_" + report.id.toString()
    val attributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()

    fun print(file: File): Boolean {

        val html = HtmlReport.encodeReport(report, true)
        // Generate a webview including signatures and then print it to pdf
        val wv = WebView(activity)

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

            override fun onPageFinished(view: WebView, url: String) {
                val printAdapter = wv.createPrintDocumentAdapter(jobName)
                printAdapter.onLayout(null, attributes, null, object : PrintDocumentAdapter.LayoutResultCallback() {
                    override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                        printAdapter.onWrite(arrayOf(PageRange.ALL_PAGES), ParcelFileDescriptor.open(
                            file,
                            ParcelFileDescriptor.MODE_READ_WRITE
                        ),
                            CancellationSignal(),
                            object : PrintDocumentAdapter.WriteResultCallback() {
                                override fun onWriteFinished(pages: Array<PageRange>) {
                                    super.onWriteFinished(pages)
                                    Log.d("Arbeitsbericht", "PDF generation finished")
                                    val cb = activity as PdfPrintFinishedCallback
                                    cb.pdfPrintFinishedCallback(file)
                                }
                            })
                    }
                },
                    null
                )
            }
        }
        wv.loadDataWithBaseURL("", html, "text/html", "UTF-8", "")
        return true
    }

    suspend fun getFilesForPdfGeneration(ctx: Context): Array<File>? {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath

        /* Create the Documents folder if it doesn't exist */
        val folder = File(path)
        if(!folder.exists()) {
            if (!folder.mkdirs()) {
                Log.d("Arbeitsbericht", "Could not create documents directory")
                val toast = Toast.makeText(activity, "Konnte erforderlichen Ordner für die Berichtsdatei nicht erstellen", Toast.LENGTH_LONG)
                toast.show()
                return null
            }
        }

        val pdfFileName = "report_${report.id.value}.pdf"
        val pdfFile = File(path, pdfFileName)
        val clientSigFileName = "report_client_sig_${report.id.value}.png"
        val clientSigFile = File(path, clientSigFileName)
        val employeeSigFileName = "report_employee_sig_${report.id.value}.png"
        val employeeSigFile = File(path, employeeSigFileName)

        try {
            // PDF file
            if(pdfFile.exists()) {
                Log.d("Arbeitsbericht", "The report did already exist")
                val answer =
                    showConfirmationDialog("Der Bericht exisitiert bereits als PDF, soll er überschrieben werden?", ctx)
                if(answer != AlertDialog.BUTTON_POSITIVE) {
                    return null
                }
            } else {
                pdfFile.createNewFile()
            }

            // client signature bitmap file
            if(!clientSigFile.exists())
                clientSigFile.createNewFile()

            // employee signature bitmap file
            if(!employeeSigFile.exists())
                employeeSigFile.createNewFile()
        } catch(e:SecurityException) {
            Log.d("Arbeitsbericht", "Permission denied on file ${pdfFile.toString()}")
            val toast = Toast.makeText(activity, "Konnte Berichtsdatei nicht erstellen wegen fehlender Berechtigungen", Toast.LENGTH_LONG)
            toast.show()
            return null
        } catch(e: IOException) {
            Log.d("Arbeitsbericht", "IOException: Could not create report file ${pdfFile.toString()}")
            val toast = Toast.makeText(activity, "Konnte Berichtsdatei nicht erstellen. Grund unbekannt", Toast.LENGTH_LONG)
            toast.show()
            return null
        }

        return arrayOf(pdfFile, clientSigFile, employeeSigFile)
    }

}