package com.stemaker.arbeitsbericht.helpers

import android.app.Activity
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.jcraft.jsch.*
import com.stemaker.arbeitsbericht.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import kotlin.Exception
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "sftpFileGetter"
private const val MSG_CONNECT = 0x01
private const val MSG_GET_FILE = 0x02
private const val MSG_SHOW_TOAST = 0x03
private const val MSG_PROMPT_YES_NO = 0x04

/* We are using a dedicated sFTP thread for handling the network tasks. Communication between
 * UI and sFTP is using Message class retrieved by Handlers (handleMessage).
 * We need to distinguish between UI waiting for a sFTP tasks to be done and sFTP task
 * waiting for feedback from the UI (mainly promptYesNo).
 * A) When the UI waits for the sFTP thread we shall use coroutines, therefore those functions
 *    invoked externally from the UI (like connect, get files, ...) are marked suspend and the caller
 *    needs to establish the coroutine context
 * B) When the sFTP thread waits for feedback from the UI (user interaction) then we use a CountDownLatch
 *    sFTP creates the object with a value of 1, waits on it and the UI once done with the request
 *    will do a count down on it.
 */

private class Connect(val user: String, val pwd: String, val host: String, val port: Int, val cont: Continuation<ConnectReturn>)
private class ConnectReturn(val success: Boolean, val error: String = "")
private class GetAsciiFile(val path: String, val cont: Continuation<GetAsciiFileReturn>)
private class GetAsciiFileReturn(val content: String, val success: Boolean, val error: String = "")
private class PromptYesNo(val text: String, val cdl: CountDownLatch)

class SftpProvider(val activity: Activity): UserInfo {
    val jsch = JSch()
    var session: Session? = null
    var channelSftp: ChannelSftp? = null
    var promptYesNoAnswer = false
    var pwd = ""

    // This class is used to handle messages within the sFTP thread
    private inner class FtpThreadHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            Log.d(TAG, "handleMessage ${msg?.what?:-1} in FTP Thread")
            when(msg?.what) {
                MSG_CONNECT -> handleConnect(msg)
                MSG_GET_FILE -> handleGetFile(msg)
            }
        }
    }

    // This class is used to handle messages within the UI thread
    private inner class UiThreadHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            Log.d(TAG, "handleMessage ${msg?.what?:-1} in UI Thread")
            when (msg?.what) {
                MSG_SHOW_TOAST -> {
                    val toast = Toast.makeText(activity, msg.obj as String, Toast.LENGTH_LONG)
                    toast.show()
                }
                MSG_PROMPT_YES_NO -> {
                    val pyn = msg.obj as PromptYesNo
                    GlobalScope.launch(Dispatchers.Main) {
                        val answer = showConfirmationDialog("sFTP query", activity, pyn.text)
                        if(answer == AlertDialog.BUTTON_POSITIVE) {
                            Log.d(TAG, "sFTP Prompt Yes/No answered with Yes")
                            promptYesNoAnswer = true
                        } else {
                            Log.d(TAG, "sFTP Prompt Yes/No answered with No")
                            promptYesNoAnswer = false
                        }
                        pyn.cdl.countDown()
                    }
                }
            }
        }
    }

    private val ftpHandlerThread = HandlerThread("sFTP Thread").apply {
        start()
    }

    private val ftpHandler = FtpThreadHandler(ftpHandlerThread.looper)
    private val uiHandler = UiThreadHandler(Looper.getMainLooper())

    // Handler functions
    private fun handleConnect(msg: Message) {
        Log.d(TAG, "handleConnect()")
        val ci = msg.obj as Connect
        pwd = ci.pwd
        try {
            session = jsch.getSession(ci.user, ci.host, ci.port)
            session?.userInfo = this
            session?.connect(20000)
            val channel = session?.openChannel("sftp") ?: throw (Exception("No session object"))
            channel.connect()
            channelSftp = channel as ChannelSftp
            Log.d(TAG, "FTPThread: Connection established")
            ci.cont.resume(ConnectReturn((true)))
        } catch (e: Exception) {
            ci.cont.resume(ConnectReturn(false, e.toString()))
        }
    }

    private fun handleGetFile(msg: Message) {
        val gaf = msg.obj as GetAsciiFile
        val os = ByteArrayOutputStream()
        try {
            channelSftp?.get(gaf.path, os)
            val ret = GetAsciiFileReturn(os.toString(), true)
            gaf.cont.resume(ret)
        } catch(e: Exception) {
            val ret = GetAsciiFileReturn(os.toString(), false, "${activity.getString(R.string.sftp_file_get_error)}${e.message}")
            gaf.cont.resume(ret)
        }
    }

    // Interface functions
    suspend fun connect(user: String, pwd: String, host: String, port: Int) {
        val msg = Message()
        msg.what = MSG_CONNECT
        val ret = suspendCoroutine<ConnectReturn> {
            msg.obj = Connect(user, pwd, host, port, it)
            ftpHandler.sendMessage(msg)
            Log.d(TAG, "connect() Coroutine: suspended")
        }
        if(ret.success) return
        throw(Exception(ret.error))
    }

    fun disconnect() {}

    // Load the file, read its content and deliver it as ASCII
    suspend fun getAsciiFromFile(path: String): String {
        val msg = Message()
        msg.what = MSG_GET_FILE
        val ret = suspendCoroutine<GetAsciiFileReturn> {
            msg.obj = GetAsciiFile(path, it)
            ftpHandler.sendMessage(msg)
            Log.d(TAG, "getAsciiFile() Coroutine: suspended")
        }
        if(ret.success) return ret.content
        throw(Exception(ret.error))
    }

    // JSCH UserInfo interface
    override fun getPassword(): String {
        Log.d(TAG, "sFTP getPassword")
        return pwd
    }

    override fun getPassphrase(): String {
        Log.d(TAG, "sFTP getPassphrase")
        uiHandler.sendMessage(Message().apply {
            what = MSG_SHOW_TOAST
            obj = "Unexpected call to getPassphrase(), no ssh keys supported"
        })
        return pwd
    }

    override fun promptPassword(message: String?): Boolean {
        Log.d(TAG, "promptPassword")
        if(pwd == "") return false
        return true
    }

    override fun promptPassphrase(message: String?): Boolean {
        Log.d(TAG, "sFTP promptPassphrase")
        uiHandler.sendMessage(Message().apply {
            what = MSG_SHOW_TOAST
            obj = "Unexpected call to promptPassphrase(), no ssh keys supported"
        })
        if(pwd == "") return false
        return true
    }

    override fun promptYesNo(message: String?): Boolean {
        Log.d(TAG, "sFTP promptYesNo")
        val cdl = CountDownLatch(1)
        uiHandler.sendMessage(Message().apply {
            what = MSG_PROMPT_YES_NO
            obj = PromptYesNo(message?:"Unidentified yes/no prompt from sFTP library JSch", cdl)
        })
        cdl.await()
        return promptYesNoAnswer
    }

    override fun showMessage(message: String?) {
        Log.d(TAG, "sFTP showMessage")
        uiHandler.sendMessage(Message().apply {
            what = MSG_SHOW_TOAST
            obj = message
        })
    }
}

