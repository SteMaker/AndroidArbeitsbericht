package com.stemaker.arbeitsbericht

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.stemaker.arbeitsbericht.databinding.ActivityClientListBinding
import com.stemaker.arbeitsbericht.databinding.ActivityMainBinding
import com.stemaker.arbeitsbericht.helpers.ClientListAdapter
import com.stemaker.arbeitsbericht.helpers.ReportListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ClientListActivity : AppCompatActivity() {
    lateinit var binding: ActivityClientListBinding
    private var clientViewModel: ClientViewModel? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_client_list)
        binding.lifecycleOwner = this

        val recyclerView = binding.clientListRv
        clientViewModel = ViewModelProvider(this@ClientListActivity, ClientViewModelFactory(ClientRepository)).get(ClientViewModel::class.java)
        val adapter = ClientListAdapter(this@ClientListActivity, clientViewModel!!)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        storageHandler().initialize()
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