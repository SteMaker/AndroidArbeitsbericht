package com.stemaker.arbeitsbericht.helpers

import android.app.Activity
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import com.stemaker.arbeitsbericht.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "sftpFileGetter"
private const val MSG_CONNECT = 0x01
private const val MSG_DISCONNECT = 0x02
private const val MSG_GET_FILE_CONTENT_AS_STRING = 0x03
private const val MSG_COPY_FILE = 0x04
private const val MSG_SHOW_TOAST = 0x05
private const val MSG_PROMPT_YES_NO = 0x06

/* We are using a dedicated SFTP thread for handling the network tasks. Communication between
 * UI and SFTP is using Message class retrieved by Handlers (handleMessage).
 * We need to distinguish between UI waiting for a SFTP tasks to be done and SFTP task
 * waiting for feedback from the UI (mainly promptYesNo).
 * A) When the UI waits for the SFTP thread we shall use coroutines, therefore those functions
 *    invoked externally from the UI (like connect, get files, ...) are marked suspend and the caller
 *    needs to establish the coroutine context
 * B) When the SFTP thread waits for feedback from the UI (user interaction) then we use a CountDownLatch
 *    SFTP creates the object with a value of 1, waits on it and the UI once done with the request
 *    will do a count down on it.
 */

private class Connect(val user: String, val pwd: String, val host: String, val port: Int, val cont: Continuation<ConnectReturn>)
private class ConnectReturn(val success: Boolean, val errorMsg: String = "")
private class Disconnect(val cont: Continuation<DisconnectReturn>)
private class DisconnectReturn(val success: Boolean, val errorMsg: String = "")
private class GetFileContentAsString(val path: String, val cont: Continuation<GetFileContentAsStringReturn>)
private class GetFileContentAsStringReturn(val content: String, val success: Boolean, val errorMsg: String = "")
private class CopyFile(val srcPath: String, val dstPath: String, val cont: Continuation<CopyFileReturn>)
private class CopyFileReturn(val success: Boolean, val errorMsg: String = "")
private class PromptYesNo(val text: String, val cdl: CountDownLatch)

class SftpProvider(val activity: Activity): UserInfo {
    val jsch = JSch()
    var session: Session? = null
    var channelSftp: ChannelSftp? = null
    var promptYesNoAnswer = false
    var pwd = ""

    // This class is used to handle messages within the SFTP thread
    private inner class FtpThreadHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.d(TAG, "handleMessage ${msg.what} in FTP Thread")
            when(msg.what) {
                MSG_CONNECT -> handleConnect(msg)
                MSG_DISCONNECT -> handleDisconnect(msg)
                MSG_GET_FILE_CONTENT_AS_STRING -> handleGetFileContentAsString(msg)
                MSG_COPY_FILE -> handleCopyFile(msg)
            }
        }
    }

    // This class is used to handle messages within the UI thread
    private inner class UiThreadHandler(looper: Looper): Handler(looper) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.d(TAG, "handleMessage ${msg.what} in UI Thread")
            when (msg.what) {
                MSG_SHOW_TOAST -> {
                    val toast = Toast.makeText(activity, msg.obj as String, Toast.LENGTH_LONG)
                    toast.show()
                }
                MSG_PROMPT_YES_NO -> {
                    val pyn = msg.obj as PromptYesNo
                    GlobalScope.launch(Dispatchers.Main) {
                        val answer = showConfirmationDialog("SFTP query", activity, pyn.text)
                        if(answer == AlertDialog.BUTTON_POSITIVE) {
                            Log.d(TAG, "SFTP Prompt Yes/No answered with Yes")
                            promptYesNoAnswer = true
                        } else {
                            Log.d(TAG, "SFTP Prompt Yes/No answered with No")
                            promptYesNoAnswer = false
                        }
                        pyn.cdl.countDown()
                    }
                }
            }
        }
    }

    private val ftpHandlerThread = HandlerThread("SFTP Thread").apply {
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
            channelSftp?.disconnect()
            session?.disconnect()
            session = jsch.getSession(ci.user, ci.host, ci.port)
            session?.userInfo = this
            session?.setConfig("MaxAuthTries", "1")
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

    private fun handleDisconnect(msg: Message) {
        Log.d(TAG, "handleDisconnect()")
        val ci = msg.obj as Disconnect
        try {
            channelSftp?.disconnect()
            session?.disconnect()
            channelSftp = null
            session = null
            ci.cont.resume(DisconnectReturn((true)))
        } catch (e: Exception) {
            ci.cont.resume(DisconnectReturn(false, e.toString()))
        }
    }

    private fun handleGetFileContentAsString(msg: Message) {
        val gaf = msg.obj as GetFileContentAsString
        val os = ByteArrayOutputStream()
        try {
            channelSftp?.get(gaf.path, os)
            val ret = GetFileContentAsStringReturn(os.toString(), true)
            gaf.cont.resume(ret)
        } catch(e: Exception) {
            val ret = GetFileContentAsStringReturn(os.toString(), false, "${activity.getString(R.string.sftp_file_get_error)}${e.message}")
            gaf.cont.resume(ret)
        }
    }

    private fun handleCopyFile(msg: Message) {
        val cfMsg = msg.obj as CopyFile
        val of = File(cfMsg.dstPath)

        try {
            if (!of.exists()) {
                of.createNewFile()
            }
            val os = FileOutputStream(of)
            channelSftp?.get(cfMsg.srcPath, os, null, ChannelSftp.OVERWRITE, 0)
            os.close()
            cfMsg.cont.resume(CopyFileReturn(true))
        } catch (e: java.lang.Exception) {
            cfMsg.cont.resume(CopyFileReturn(false, e.message?:activity.getString(R.string.unknown)))
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
        throw(Exception(ret.errorMsg))
    }

    // Disconnect the channel and session
    suspend fun disconnect() {
        val msg = Message()
        msg.what = MSG_DISCONNECT
        val ret = suspendCoroutine<DisconnectReturn> {
            msg.obj = Disconnect(it)
            ftpHandler.sendMessage(msg)
            Log.d(TAG, "disconnect() Coroutine: suspended")
        }
        if (ret.success) return
        throw(Exception(ret.errorMsg))
    }

    // Load the file, read its content and deliver it as String
    suspend fun getFileContentAsString(path: String): String {
        val msg = Message()
        msg.what = MSG_GET_FILE_CONTENT_AS_STRING
        val ret = suspendCoroutine<GetFileContentAsStringReturn> {
            msg.obj = GetFileContentAsString(path, it)
            ftpHandler.sendMessage(msg)
            Log.d(TAG, "getFileContentAsString() Coroutine: suspended")
        }
        if(ret.success) return ret.content
        throw(Exception(ret.errorMsg))
    }

    suspend fun copyFile(srcPath: String, dstPath: String) {
        val msg = Message()
        msg.what = MSG_COPY_FILE
        val ret = suspendCoroutine<CopyFileReturn> {
            msg.obj = CopyFile(srcPath, dstPath, it)
            ftpHandler.sendMessage(msg)
            Log.d(TAG, "copyFile() Coroutine: suspended")
        }
        if(ret.success) return
        throw(Exception(ret.errorMsg))
    }

    // JSCH UserInfo interface
    override fun getPassword(): String {
        Log.d(TAG, "SFTP getPassword")
        return pwd
    }

    override fun getPassphrase(): String {
        Log.d(TAG, "SFTP getPassphrase")
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
        Log.d(TAG, "SFTP promptPassphrase")
        uiHandler.sendMessage(Message().apply {
            what = MSG_SHOW_TOAST
            obj = "Unexpected call to promptPassphrase(), no ssh keys supported"
        })
        if(pwd == "") return false
        return true
    }

    override fun promptYesNo(message: String?): Boolean {
        Log.d(TAG, "SFTP promptYesNo")
        val cdl = CountDownLatch(1)
        uiHandler.sendMessage(Message().apply {
            what = MSG_PROMPT_YES_NO
            obj = PromptYesNo(message?:"Unidentified yes/no prompt from SFTP library JSch", cdl)
        })
        cdl.await()
        return promptYesNoAnswer
    }

    override fun showMessage(message: String?) {
        Log.d(TAG, "SFTP showMessage")
        uiHandler.sendMessage(Message().apply {
            what = MSG_SHOW_TOAST
            obj = message
        })
    }
}

