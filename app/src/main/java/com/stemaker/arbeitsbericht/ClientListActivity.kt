package com.stemaker.arbeitsbericht

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.stemaker.arbeitsbericht.databinding.ActivityClientListBinding
import com.stemaker.arbeitsbericht.helpers.ClientListAdapter

class ClientListActivity:
    ArbeitsberichtActivity()
{
    lateinit var binding: ActivityClientListBinding
    private var clientViewModel: ClientViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if(!onCreateWrapper(savedInstanceState))
            return

        // Here we expect that the app initialization is done
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_client_list)
        binding.lifecycleOwner = this

        val recyclerView = binding.clientListRv
        clientViewModel = ViewModelProvider(this@ClientListActivity, ClientViewModelFactory(app.clientRepo)).get(ClientViewModel::class.java)
        val adapter = ClientListAdapter(this@ClientListActivity, clientViewModel!!)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        requestedOrientation = when(prefs.lockScreenOrientation.value) {
            true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
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