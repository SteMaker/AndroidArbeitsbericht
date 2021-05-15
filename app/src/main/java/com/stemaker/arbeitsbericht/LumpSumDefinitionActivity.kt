package com.stemaker.arbeitsbericht

//import kotlinx.android.synthetic.main.activity_lump_sum_definition.*
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stemaker.arbeitsbericht.data.configuration
import com.stemaker.arbeitsbericht.databinding.ActivityLumpSumDefinitionBinding
import com.stemaker.arbeitsbericht.helpers.SftpProvider
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

private const val TAG = "LumpSumDefinitionAct"
private const val PERMISSION_CODE_REQUEST_INTERNET = 1

class LumpSumDefinitionActivity : AppCompatActivity() {
    private var internetPermissionContinuation: Continuation<Boolean>? = null
    private lateinit var binding: ActivityLumpSumDefinitionBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLumpSumDefinitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val storageInitJob = storageHandler().initialize()

        GlobalScope.launch(Dispatchers.Main) {
            storageInitJob?.let {
                if (!it.isCompleted) {
                    findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.VISIBLE
                    it.join()
                    findViewById<ProgressBar>(R.id.sftp_progress).visibility = View.GONE
                }
            } ?: run { Log.e(TAG, "storageHandler job was null :(") }
            binding.lumpSumFtpPath.setText(configuration().lumpSumServerPath)
            val lumpSums = configuration().lumpSums
            for (ls in lumpSums) {
                addLumpSumView(ls)
            }
        }
        binding.lumpSumConfigurationActivityToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.config_save_button -> {
                    save()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onClickAddLumpSumDefinition(btn: View) {
        addLumpSumView("Unbekannt")
    }

    private fun addLumpSumView(ls: String) {
        // Prepare a lump_sum_layout instance
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val cV = inflater.inflate(R.layout.lump_sum_layout, null) as CardView
        cV.findViewById<TextView>(R.id.lump_sum_text).setText(ls)

        val btnDel = cV.findViewById<Button>(R.id.lump_sum_del_button)
        btnDel.setTag(R.id.TAG_CARDVIEW, cV)

        val pos = binding.lumpSumsContainer.childCount
        Log.d(TAG, "Adding lump-sum card $pos to UI")
        binding.lumpSumsContainer.addView(cV, pos)
    }

    private fun save() {
        GlobalScope.launch(Dispatchers.Main) {
            Log.d(TAG, "saving ${binding.lumpSumsContainer.childCount} lump-sums ...")
            val lumpSums = mutableListOf<String>()
            for (pos in 0 until binding.lumpSumsContainer.childCount) {
                val cV = binding.lumpSumsContainer.getChildAt(pos) as CardView
                val lumpSum = cV.findViewById<EditText>(R.id.lump_sum_text).text.toString()
                lumpSums.add(lumpSum)
                Log.d(TAG, "saving $pos")
            }
            configuration().lock()
            configuration().lumpSums = lumpSums
            configuration().lumpSumServerPath = findViewById<EditText>(R.id.lump_sum_ftp_path).text.toString()
            configuration().save()
            storageHandler().updateLumpSums()
            configuration().unlock()
            storageHandler().saveConfigurationToFile(getApplicationContext())
            val intent = Intent(this@LumpSumDefinitionActivity, MainActivity::class.java).apply {}
            startActivity(intent)
        }
    }

    fun onClickDelLumpSum(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            val answer = showConfirmationDialog(getString(R.string.del_confirmation), this@LumpSumDefinitionActivity)
            if(answer == AlertDialog.BUTTON_POSITIVE) {
                Log.d(TAG, "deleting a lump sum element")
                binding.lumpSumsContainer.removeView(btn.getTag(R.id.TAG_CARDVIEW) as View)
            } else {
                Log.d(TAG, "Cancelled deleting a lump sum element")
            }
        }
    }

    fun onClickLoad(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            if(checkAndObtainInternetPermission()) {
                findViewById<ProgressBar>(R.id.load_progress).visibility = View.VISIBLE
                val host = configuration().sFtpHost
                val port = configuration().sFtpPort
                val user = configuration().sFtpUser
                val pwd = configuration().sFtpEncryptedPassword
                val path = findViewById<EditText>(R.id.lump_sum_ftp_path).text.toString()
                try {
                    val sftpProvider = SftpProvider(this@LumpSumDefinitionActivity)
                    sftpProvider.connect(user, pwd, host, port)
                    Log.d(TAG, "Connection success")
                    val lumpSumsFileContent = sftpProvider.getFileContentAsString(path)
                    val lumpSums = lumpSumsFileContent.lines()
                    val filtered = mutableListOf<String>()
                    for (e in lumpSums)
                        if (e != "")
                            filtered.add(e)
                    if (filtered.size == 0) {
                        val answer = showConfirmationDialog(getString(R.string.no_lump_sums_found), this@LumpSumDefinitionActivity)
                        if (answer == AlertDialog.BUTTON_POSITIVE) {
                            binding.lumpSumsContainer.removeAllViews()
                        }
                    } else {
                        val answer = showConfirmationDialog(getString(R.string.lump_sums_found, filtered.size), this@LumpSumDefinitionActivity)
                        if (answer == AlertDialog.BUTTON_POSITIVE) {
                            binding.lumpSumsContainer.removeAllViews()
                            for (line in filtered)
                                addLumpSumView(line)
                        }
                    }
                } catch (e: Exception) {
                    val toast = Toast.makeText(this@LumpSumDefinitionActivity, e.message, Toast.LENGTH_LONG)
                    toast.show()
                }
                findViewById<ProgressBar>(R.id.load_progress).visibility = View.GONE
            }
        }
    }

    private suspend fun checkAndObtainInternetPermission(): Boolean {
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
        if(internetPermissionGranted) {
            Log.d(TAG, "Permission for Internet is granted")
            return true
        } else {
            val toast =
                Toast.makeText(this, R.string.no_internet_access_abort, Toast.LENGTH_LONG)
            toast.show()
            return false
        }
    }
}
