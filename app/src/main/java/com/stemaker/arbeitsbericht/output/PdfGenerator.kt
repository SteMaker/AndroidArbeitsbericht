package android.print

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.helpers.HtmlReport
import com.stemaker.arbeitsbericht.output.ReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "PdfPrint"

class PdfGenerator(activity: Activity, report: ReportData, progressBar: ProgressBar?, textView: TextView?) :
    ReportGenerator(activity, report, progressBar, textView, true){

    val jobName = "pdf_print_" + report.id
    var webView: WebView? = null
    var html: String? = null
    val attributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()
    override fun createDoc(files: Array<File>, done: (success:Boolean) -> Unit) {
        // Generate a webview including signatures and then print it to pdf
        html = HtmlReport.encodeReport(report, true)
        webView = WebView(activity)

        webView?.let {
            it.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

                override fun onPageFinished(view: WebView, url: String) {
                    Log.d(TAG, "onPageFinished")
                    val printAdapter = it.createPrintDocumentAdapter(jobName)
                    printAdapter.onLayout(null, attributes, null, object : PrintDocumentAdapter.LayoutResultCallback() {
                        override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                            Log.d(TAG, "onLayoutFinished")
                            printAdapter.onWrite(arrayOf(PageRange.ALL_PAGES),
                                ParcelFileDescriptor.open(files[0], ParcelFileDescriptor.MODE_READ_WRITE),
                                CancellationSignal(),
                                object : PrintDocumentAdapter.WriteResultCallback() {
                                    override fun onWriteFinished(pages: Array<PageRange>) {
                                        super.onWriteFinished(pages)
                                        done(true)
                                    }
                                })
                        }
                    },
                        null
                    )
                }
            }
            it.loadDataWithBaseURL("", html, "text/html", "UTF-8", "")
        }?: run {
           done(false)
        }
    }

    override val filePostFixExt: Array<Pair<String, String>>
        get() = arrayOf(Pair("", "pdf"))

    override fun getHash(files: Array<File>): String? = null
}
