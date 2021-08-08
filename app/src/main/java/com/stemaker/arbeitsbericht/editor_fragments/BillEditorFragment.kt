package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.AboutDialogFragment
import com.stemaker.arbeitsbericht.ClientSelectDialog
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.BillData
import com.stemaker.arbeitsbericht.databinding.FragmentBillEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BillEditorFragment : ReportEditorSectionFragment() {
    private var listener: OnBillEditorInteractionListener? = null
    lateinit var dataBinding: FragmentBillEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht","BillEditorFragment.onCreate called")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d("Arbeitsbericht","BillEditorFragment.onCreateView called")
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_bill_editor, null, false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)
        dataBinding.clientSelectButton.setOnClickListener {
            val clientSelectDialog = ClientSelectDialog()
            clientSelectDialog.setOnSelectListener { client ->
                dataBinding.billData?.name?.value = client.name.value
                dataBinding.billData?.street?.value = client.street.value
                dataBinding.billData?.zip?.value = client.zip.value
                dataBinding.billData?.city?.value = client.city.value
            }
            clientSelectDialog.show(childFragmentManager, "ClientSelectDialog")
        }

        setHeadline("Rechnungsadresse")

        dataBinding.lifecycleOwner = this
        GlobalScope.launch(Dispatchers.Main) {
            dataBinding.billData = listener!!.getBillData()
        }
        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnBillEditorInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnBillEditorInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.root.findViewById<LinearLayout>(R.id.bill_content_container).setVisibility(if(vis) View.VISIBLE else View.GONE)
    }

    override fun getVisibility(): Boolean {
        return dataBinding.root.findViewById<LinearLayout>(R.id.bill_content_container).visibility != View.GONE
    }

    interface OnBillEditorInteractionListener {
        suspend fun getBillData(): BillData
    }
}
