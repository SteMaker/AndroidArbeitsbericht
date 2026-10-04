package com.stemaker.arbeitsbericht

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.stemaker.arbeitsbericht.databinding.ActivityLumpSumDefinitionBinding
import com.stemaker.arbeitsbericht.helpers.SftpProvider
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val TAG = "LumpSumDefinitionAct"

class LumpSumDefinitionActivity:
    ArbeitsberichtActivity()
{
    private lateinit var binding: ActivityLumpSumDefinitionBinding

    private val lumpSumFilePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                Log.d(TAG, "No lump sum file was selected")
                return@registerForActivityResult
            }
            result.data?.data?.also { fileUri -> readLumpSumsFromUri(fileUri) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if(!onCreateWrapper(savedInstanceState))
            return

        // Here we expect that the app initialization is done
        super.onCreate(savedInstanceState)
        binding = ActivityLumpSumDefinitionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addBackPressedHandler { save() }

        requestedOrientation = when(prefs.lockScreenOrientation.value) {
            true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        binding.lumpSumFtpPath.setText(prefs.lumpSumServerPath.value)
        val lumpSums = prefs.lumpSums.value
        for (ls in lumpSums) {
            addLumpSumView(ls)
        }
        binding.lumpSumConfigurationActivityToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.config_save_button -> {
                    save()
                    val intent = Intent(this@LumpSumDefinitionActivity, MainActivity::class.java).apply {}
                    startActivity(intent)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        save()
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
            prefs.lumpSums.setValue(lumpSums.toSet())
            prefs.lumpSumServerPath.value = findViewById<EditText>(R.id.lump_sum_ftp_path).text.toString()
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

    private suspend fun loadLumpSums(lumpSumsFileContent: String) {
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
    }

    fun onClickLoad(btn: View) {
        val mimetypes = arrayOf("text/plain")
        val intent = Intent()
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
            .setAction(Intent.ACTION_GET_CONTENT)
        lumpSumFilePicker.launch(Intent.createChooser(intent, getString(R.string.lump_sum_file_choose)))
    }

    private fun readLumpSumsFromUri(fileUri: android.net.Uri) {
        try {
            contentResolver.openInputStream(fileUri)?.also { istream ->
                val lumpSumsFileContent = istream.bufferedReader().use { it.readText() }
                GlobalScope.launch(Dispatchers.Main) {
                    loadLumpSums(lumpSumsFileContent)
                }
            }
        } catch (e: Exception) {
            val toast = Toast.makeText(this@LumpSumDefinitionActivity, "Fehler beim Lesen der Pauschalen: ${e.message}", Toast.LENGTH_LONG)
            toast.show()
        }
    }

    fun onClickSftpLoad(btn: View) {
        GlobalScope.launch(Dispatchers.Main) {
            findViewById<ProgressBar>(R.id.load_progress).visibility = View.VISIBLE
            val host = prefs.sFtpHost.value
            val port = prefs.sFtpPort.value
            val user = prefs.sFtpUser.value
            val pwd = prefs.sFtpPassword.value
            val path = findViewById<EditText>(R.id.lump_sum_ftp_path).text.toString()
            try {
                val sftpProvider = SftpProvider(this@LumpSumDefinitionActivity)
                sftpProvider.connect(user, pwd, host, port)
                Log.d(TAG, "Connection success")
                val lumpSumsFileContent = sftpProvider.getFileContentAsString(path)
                loadLumpSums(lumpSumsFileContent)
            } catch (e: Exception) {
                val toast = Toast.makeText(this@LumpSumDefinitionActivity, e.message, Toast.LENGTH_LONG)
                toast.show()
            }
            findViewById<ProgressBar>(R.id.load_progress).visibility = View.GONE
        }
    }

}
