package com.stemaker.arbeitsbericht

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stemaker.arbeitsbericht.helpers.SftpProvider
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.android.synthetic.main.activity_lump_sum_definition.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

private const val TAG = "LumpSumDefinitionActivity"
private const val PERMISSION_CODE_REQUEST_INTERNET = 1

class LumpSumDefinitionActivity : AppCompatActivity() {
    var internetPermissionContinuation: Continuation<Boolean>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lump_sum_definition)

        findViewById<CheckBox>(R.id.checkbox_server).setChecked(configuration().lumpSumsFromServer)
        findViewById<EditText>(R.id.lump_sum_ftp_host).setText(configuration().lumpSumServerHost)
        findViewById<EditText>(R.id.lump_sum_ftp_port).setText(configuration().lumpSumServerPort.toString())
        findViewById<EditText>(R.id.lump_sum_ftp_path).setText(configuration().lumpSumServerPath)
        findViewById<EditText>(R.id.lump_sum_ftp_user).setText(configuration().lumpSumServerUser)
        val lumpSums = configuration().lumpSums
        for (ls in lumpSums) {
            addLumpSumView(ls)
        }
        if(configuration().lumpSumsFromServer) serverViewsEnabled()
        else serverViewsDisabled()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickAddLumpSumDefinition(btn: View) {
        addLumpSumView("Unbekannt")
    }

    fun addLumpSumView(ls: String) {
        // Prepare a lump_sum_layout instance
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val cV = inflater.inflate(R.layout.lump_sum_layout, null) as CardView
        cV.findViewById<TextView>(R.id.lump_sum_text).setText(ls)

        val btnDel = cV.findViewById<ImageButton>(R.id.lump_sum_del_button)
        btnDel.setTag(R.id.TAG_CARDVIEW, cV)

        val pos = lump_sums_container.getChildCount()
        Log.d("Arbeitsbericht.LumpSumDefinitionActivity.addLumpSumView", "Adding lump-sum card $pos to UI")
        lump_sums_container.addView(cV, pos)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickSave(btn: View) {
        Log.d("Arbeitsbericht.LumpSumDefinitionActivity.onClickSave", "saving ${lump_sums_container.getChildCount()} lump-sums ...")
        val lumpSums = mutableListOf<String>()
        for (pos in 0 until lump_sums_container.getChildCount()) {
            val cV = lump_sums_container.getChildAt(pos) as CardView
            val lumpSum = cV.findViewById<EditText>(R.id.lump_sum_text).text.toString()
            lumpSums.add(lumpSum)
            Log.d("Arbeitsbericht.LumpSumDefinitionActivity.onClickSave", "saving $pos")
        }
        configuration().lumpSums = lumpSums
        configuration().lumpSumsFromServer = findViewById<CheckBox>(R.id.checkbox_server).isChecked()
        if(configuration().lumpSumsFromServer) {
            configuration().lumpSumServerHost= findViewById<EditText>(R.id.lump_sum_ftp_host).text.toString()
            configuration().lumpSumServerPort = findViewById<EditText>(R.id.lump_sum_ftp_port).text.toString().toInt()
            configuration().lumpSumServerPath = findViewById<EditText>(R.id.lump_sum_ftp_path).text.toString()
            configuration().lumpSumServerUser = findViewById<EditText>(R.id.lump_sum_ftp_user).text.toString()
            val pwd = findViewById<EditText>(R.id.lump_sum_ftp_pwd).text.toString()
            if(pwd != "") // only overwrite if a new one has been set
                configuration().lumpSumServerEncryptedPassword = pwd
        }
        storageHandler().saveConfigurationToFile(getApplicationContext())
        val intent = Intent(this, MainActivity::class.java).apply {}
        startActivity(intent)
    }

    fun onClickDelLumpSum(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@LumpSumDefinitionActivity)
            if(answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d("Arbeitsbericht.LumpSumDefinitionActivity.onClickDelLumpSum", "deleting a lump sum element")
                lump_sums_container.removeView(btn.getTag(R.id.TAG_CARDVIEW) as View)
            } else {
                Log.d("Arbeitsbericht.LumpSumDefinitionActivity.onClickDelLumpSum", "Cancelled deleting a lump sum element")
            }
        }
    }

    fun onCheckboxServerClicked(chkb: View) {
        val checkbox = chkb as CheckBox
        when(checkbox.isChecked()) {
            true -> serverViewsEnabled()
            false -> serverViewsDisabled()
        }
    }

    fun onClickLoad(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            findViewById<ProgressBar>(R.id.load_progress).visibility = View.VISIBLE
            val host = findViewById<AutoCompleteTextView>(R.id.lump_sum_ftp_host).text.toString()
            val port = findViewById<AutoCompleteTextView>(R.id.lump_sum_ftp_port).text.toString().toInt()
            val user = findViewById<AutoCompleteTextView>(R.id.lump_sum_ftp_user).text.toString()
            var pwd = findViewById<AutoCompleteTextView>(R.id.lump_sum_ftp_pwd).text.toString()
            if(pwd == "") pwd = configuration().lumpSumServerEncryptedPassword
            val path = findViewById<AutoCompleteTextView>(R.id.lump_sum_ftp_path).text.toString()
            try {
                val sftpProvider = SftpProvider(this@LumpSumDefinitionActivity)
                sftpProvider.connect(user, pwd, host, port)
                Log.d(TAG, "Connection success")
                val lumpSumsFileContent = sftpProvider.getAsciiFromFile(path)
                Log.d(TAG, lumpSumsFileContent)
            } catch(e: Exception) {
                val toast = Toast.makeText(this@LumpSumDefinitionActivity, e.message, Toast.LENGTH_LONG)
                toast.show()
            }
            findViewById<ProgressBar>(R.id.load_progress).visibility = View.GONE
        }
    }

    private fun serverViewsEnabled() {

        GlobalScope.launch(Dispatchers.Main) {
            checkAndObtainInternetPermission()
        }
        findViewById<View>(R.id.add_lump_sum_element_button).visibility = View.GONE
        findViewById<View>(R.id.ftp_config_container).visibility = View.VISIBLE

        // Lump sums shall no longer be editable
        for (pos in 0 until lump_sums_container.getChildCount()) {
            val cV = lump_sums_container.getChildAt(pos) as CardView
            cV.findViewById<EditText>(R.id.lump_sum_text).setEnabled(false)
            cV.findViewById<View>(R.id.lump_sum_del_button).visibility = View.GONE
        }
    }

    private fun serverViewsDisabled() {
        findViewById<View>(R.id.add_lump_sum_element_button).visibility = View.VISIBLE
        findViewById<View>(R.id.ftp_config_container).visibility = View.GONE

        // Make lump sums editable
        for (pos in 0 until lump_sums_container.getChildCount()) {
            val cV = lump_sums_container.getChildAt(pos) as CardView
            cV.findViewById<EditText>(R.id.lump_sum_text).setEnabled(true)
            cV.findViewById<View>(R.id.lump_sum_del_button).visibility = View.VISIBLE
        }
    }

    suspend fun checkAndObtainInternetPermission() {
        var internetPermissionGranted = true
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "No permission for Internet, requesting it")
            /* TODO Add a Dialog that explains why we need network access */
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.INTERNET),
                PERMISSION_CODE_REQUEST_INTERNET
            )
            internetPermissionGranted = suspendCoroutine<Boolean> {
                internetPermissionContinuation = it
            }
        }
        if (internetPermissionGranted)
            Log.d(TAG, "Permission for Internet is granted")
        else {
            val toast =
                Toast.makeText(this, R.string.no_internet_access_abort, Toast.LENGTH_LONG)
            toast.show()
        }
    }
}
