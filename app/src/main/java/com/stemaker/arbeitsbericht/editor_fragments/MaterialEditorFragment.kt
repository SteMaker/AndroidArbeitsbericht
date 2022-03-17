package com.stemaker.arbeitsbericht.editor_fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.stemaker.arbeitsbericht.R
import com.stemaker.arbeitsbericht.data.MaterialContainerData
import com.stemaker.arbeitsbericht.data.MaterialData
import com.stemaker.arbeitsbericht.databinding.FragmentMaterialEditorBinding
import com.stemaker.arbeitsbericht.databinding.MaterialLayoutBinding
import com.stemaker.arbeitsbericht.helpers.showConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MaterialEditorFragment : ReportEditorSectionFragment() {
    lateinit var dataBinding: FragmentMaterialEditorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentMaterialEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.material))

        dataBinding.lifecycleOwner = viewLifecycleOwner
        GlobalScope.launch(Dispatchers.Main) {
            listener?.let { listener ->
                val report = listener.getReportData()
                report?.let { report ->
                    val materialContainerData = report.materialContainer
                    dataBinding.materialContainerData = materialContainerData

                    for (wt in materialContainerData.items) {
                        addMaterialView(wt, materialContainerData)
                    }

                    dataBinding.materialAddButton.setOnClickListener(object : View.OnClickListener {
                        override fun onClick(btn: View) {
                            val wi = materialContainerData.addMaterial()
                            addMaterialView(wi, materialContainerData)
                        }
                    })
                }
            }
        }

        return root
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.materialContentContainer.visibility = when(vis) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun getVisibility(): Boolean {
        return dataBinding.materialContentContainer.visibility != View.GONE
    }

    fun addMaterialView(wi: MaterialData, materialContainerData: MaterialContainerData) {
        val inflater = layoutInflater
        val container = dataBinding.materialContentContainer
        val materialDataBinding: MaterialLayoutBinding = MaterialLayoutBinding.inflate(inflater, null, false)
        materialDataBinding.material = wi
        materialDataBinding.materialContainer = materialContainerData
        materialDataBinding.lifecycleOwner = activity
        materialDataBinding.materialDelButton.setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer =
                        showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        container.removeView(materialDataBinding.root)
                        materialContainerData.removeMaterial(wi)
                    } else {
                    }
                }
            }
        })
        materialDataBinding.materialUnit.setOnFocusChangeListener { v, hasFocus -> run {
            val tv = v as AutoCompleteTextView
            if(hasFocus && tv.text.toString() == "") tv.showDropDown()
        } }

        val pos = container.getChildCount()
        container.addView(materialDataBinding.root, pos)
    }

}
