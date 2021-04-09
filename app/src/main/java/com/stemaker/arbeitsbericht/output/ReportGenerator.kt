package com.stemaker.arbeitsbericht.output

import android.app.Activity
import android.os.*
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.ReportData
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import com.stemaker.arbeitsbericht.helpers.showInfoDialog
import java.io.File
import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "ReportGenerator"

private class Create(val files: Array<File>, val cont: Continuation<CreateReturn>)
private class CreateReturn(val success: Boolean, val error: String = "")
private class ProgressInfo(val progress: Int, val status: String)
private const val MSG_CREATE = 0x01
private const val MSG_PROGRESS_INFO = 0x02

abstract class ReportGenerator(val activity: Activity, val report: ReportData, private val progressBar: ProgressBar?, private val textView: TextView?, val renderInUiThread: Boolean = false) {

    // Make sure that every postfix-extension combination only exists once
    abstract val filePostFixExt: Array<Pair<String, String>>

    abstract fun getHash(files: Array<File>): String?

    abstract fun createDoc(files: Array<File>, done: (success: Boolean) -> Unit)

    suspend fun getFilesForGeneration(): Array<File>? {
        val publicPath = "${Environment.getExternalStorageDirectory().absolutePath}/Arbeitsbericht"
        val privatePath = "${activity.filesDir.absolutePath}/Arbeitsbericht"

        /* Create the Documents folder if it doesn't exist */
        for(path in arrayOf(publicPath, privatePath)) {
            val folder = File(path)
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    Log.d(TAG, "Could not create documents directory")
                    showInfoDialog(activity.getString(R.string.cannot_createdocs_dir), activity)
                    return null
                }
            }
        }

        val docFiles = mutableListOf<File>()
        for (postfixExt in filePostFixExt)
            docFiles.add(File(publicPath, "report_${report.id}${postfixExt.first}.${postfixExt.second}"))

        val clientSigFile = File(privatePath, "report_client_sig_${report.id}.png")
        val employeeSigFile = File(privatePath, "report_employee_sig_${report.id}.png")

        try {
            // Document files
            var overwriteChecked = false
            for(f in docFiles) {
                if (f.exists()) {
                    if(!overwriteChecked) {
                        val answer =
                            showConfirmationDialog(activity.getString(R.string.report_exists_overwrite), activity)
                        if (answer != AlertDialog.BUTTON_POSITIVE) {
                            return null
                        }
                        overwriteChecked = true
                    }
                } else {
                    f.createNewFile()
                }
            }

            // client signature bitmap file
            if(!clientSigFile.exists())
                clientSigFile.createNewFile()

            // employee signature bitmap file
            if(!employeeSigFile.exists())
                employeeSigFile.createNewFile()
        } catch(e:SecurityException) {
            showInfoDialog(activity.getString(R.string.report_create_fail_perm), activity)
            return null
        } catch(e: IOException) {
            showInfoDialog(activity.getString(R.string.report_create_fail_unknown), activity)
            return null
        }
        return docFiles.plus(clientSigFile).plus(employeeSigFile).toTypedArray()
    }

    fun isDocUpToDate(files: Array<File>): Boolean {
        return getHash(files) == report.lastStoreHash.toString() && report.lastStoreHash != 0
    }

    suspend fun create(files: Array<File>) {
        if(renderInUiThread) {
            var cont: Continuation<Boolean>? = null
            createDoc(files) {
                progressInfo(100, activity.getString(R.string.status_done))
                cont!!.resume(it)
            }
            val success = suspendCoroutine<Boolean> {
                cont = it
            }
        } else {
            val msg = Message()
            msg.what = MSG_CREATE
            val ret = suspendCoroutine<CreateReturn> {
                msg.obj = Create(files, it)
                docHandler.sendMessage(msg)
            }
            if (ret.success) return
            throw(Exception(ret.error))
        }
    }

    /////////////////////////
    // THREAD RELATED STUFF
    /////////////////////////
    // This class is used to handle messages within the doc generator thread
    private inner class DocThreadHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when(msg?.what) {
                MSG_CREATE -> handleCreate(msg)
            }
        }
    }

    /* This class is used to handle messages within the UI thread */
    private inner class UiThreadHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                MSG_PROGRESS_INFO -> handleProgressInfo(msg)
            }
        }
    }
    private val docHandlerThread = HandlerThread("DocGen Thread").apply {
        start()
    }
    private val docHandler = DocThreadHandler(docHandlerThread.looper)
    private val uiHandler = UiThreadHandler(Looper.getMainLooper())

    ///////////////////////////////////////////////////////
    // BELOW FUNCTIONS ARE SUPPOSED TO RUN IN THE UI THREAD
    ///////////////////////////////////////////////////////
    fun handleProgressInfo(msg: Message) {
        val info = msg.obj as ProgressInfo
        progressBar?.progress = info.progress
        textView?.text = info.status
    }

    ////////////////////////////////////////////////////////
    // BELOW FUNCTIONS ARE SUPPOSED TO RUN IN THE DOC THREAD
    ////////////////////////////////////////////////////////
    fun progressInfo(progress: Int, status: String) {
        if(renderInUiThread) {
            progressBar?.progress = progress
            textView?.text = status
        } else {
            val msg = Message()
            msg.what = MSG_PROGRESS_INFO
            msg.obj = ProgressInfo(progress, status)
            uiHandler.sendMessage(msg)
        }
    }

    private fun handleCreate(msg: Message) {
        Log.d(TAG, "create()")
        val cm = msg.obj as Create
        createDoc(cm.files) {
            progressInfo(100, activity.getString(R.string.status_done))
            cm.cont.resume(CreateReturn((it)))
        }
    }
}