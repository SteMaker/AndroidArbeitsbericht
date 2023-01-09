package com.stemaker.arbeitsbericht

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.stemaker.arbeitsbericht.data.client.Client
import com.stemaker.arbeitsbericht.data.client.ClientRepository
import com.stemaker.arbeitsbericht.databinding.ClientSelectDialogBinding
import com.stemaker.arbeitsbericht.helpers.ClientSelectListAdapter

class ClientSelectDialog(private val clientRepository: ClientRepository) : DialogFragment() {
    lateinit var binding: ClientSelectDialogBinding
    private var clientViewModel: ClientViewModel? = null
    private var listener: (client: Client) ->Unit = {}
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.client_select_dialog, null, false)
        binding.lifecycleOwner = this

        val recyclerView = binding.clientListRv
        clientViewModel = ViewModelProvider(this, ClientViewModelFactory(clientRepository)).get(ClientViewModel::class.java)
        val adapter = ClientSelectListAdapter(this, clientViewModel!!)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        adapter.setOnSelectListener { client ->
            listener(client)
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
        return binding.root
    }

    override fun onStart()
    {
        super.onStart();
        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT;
            val height = ViewGroup.LayoutParams.MATCH_PARENT;
            it.window?.setLayout(width, height);
        }
    }

    override fun onPause() {
        dismiss()
        super.onPause()
    }

    fun setOnSelectListener(listener: (client: Client) -> Unit) {
        this.listener = listener
    }
}