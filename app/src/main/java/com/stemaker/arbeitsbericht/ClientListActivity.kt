package com.stemaker.arbeitsbericht

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.stemaker.arbeitsbericht.data.configuration
import com.stemaker.arbeitsbericht.databinding.ActivityClientListBinding
import com.stemaker.arbeitsbericht.databinding.ActivityMainBinding
import com.stemaker.arbeitsbericht.helpers.ClientListAdapter
import com.stemaker.arbeitsbericht.helpers.ReportListAdapter
import com.stemaker.arbeitsbericht.helpers.VersionDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ClientListActivity : AppCompatActivity() {
    lateinit var binding: ActivityClientListBinding
    private var clientViewModel: ClientViewModel? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storageInitJob = storageHandler().initialize()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_client_list)
        binding.lifecycleOwner = this

        val recyclerView = binding.clientListRv
        clientViewModel = ViewModelProvider(this@ClientListActivity, ClientViewModelFactory(ClientRepository)).get(ClientViewModel::class.java)
        val adapter = ClientListAdapter(this@ClientListActivity, clientViewModel!!)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        GlobalScope.launch(Dispatchers.Main) {
            binding.progressBar.visibility = View.VISIBLE
            binding.initNotify.visibility = View.VISIBLE
            storageInitJob?.join()
            requestedOrientation = when(configuration().lockScreenOrientation) {
                true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            }
            binding.progressBar.visibility = View.GONE
            binding.initNotify.visibility = View.GONE

            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }

        binding.clientActivityToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.client_save_button -> {
                    saveData()
                    val intent = Intent(this@ClientListActivity, MainActivity::class.java).apply {}
                    startActivity(intent)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveData()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveData()
    }

    fun onClickNewClient(v_: View) {
        clientViewModel?.addClient()
    }

    private fun saveData() {
        clientViewModel?.store()
    }

}