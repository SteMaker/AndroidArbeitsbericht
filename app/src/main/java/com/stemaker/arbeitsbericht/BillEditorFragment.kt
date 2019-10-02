package com.stemaker.arbeitsbericht

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import com.stemaker.arbeitsbericht.databinding.FragmentBillEditorBinding
import com.stemaker.arbeitsbericht.databinding.FragmentProjectEditorBinding

class BillEditorFragment : ReportEditorSectionFragment(),
                           ReportEditorSectionFragment.OnExpandChange {
    private var listener: OnBillEditorInteractionListener? = null
    var billData: BillData? = null
    lateinit var dataBinding: FragmentBillEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht","BillEditorFragment.onCreate called")
        billData = listener!!.getBillData()
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

        setHeadline("Rechnungsadresse")

        dataBinding.lifecycleOwner = this
        dataBinding.billData = billData!!
        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context, this)
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

    interface OnBillEditorInteractionListener {
        fun getBillData(): BillData
    }

}
