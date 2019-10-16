package com.stemaker.arbeitsbericht

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.stemaker.arbeitsbericht.data.MaterialContainerData
import com.stemaker.arbeitsbericht.data.MaterialData
import com.stemaker.arbeitsbericht.databinding.FragmentMaterialEditorBinding
import com.stemaker.arbeitsbericht.databinding.MaterialLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MaterialEditorFragment : ReportEditorSectionFragment(),
    ReportEditorSectionFragment.OnExpandChange {
    private var listener: OnMaterialEditorInteractionListener? = null
    var materialContainerData: MaterialContainerData? = null
    lateinit var dataBinding: FragmentMaterialEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Arbeitsbericht","MaterialEditorFragment.onCreate called")
        materialContainerData = listener!!.getMaterialContainerData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        Log.d("Arbeitsbericht","MaterialEditorFragment.onCreateView called")
        val root = super.onCreateView(inflater, container, savedInstanceState)
        dataBinding = FragmentMaterialEditorBinding.inflate(inflater, container,false)
        root!!.findViewById<LinearLayout>(R.id.section_container).addView(dataBinding.root)

        setHeadline(getString(R.string.material))

        dataBinding.lifecycleOwner = this
        dataBinding.materialContainerData = materialContainerData!!

        for(wt in materialContainerData!!.items) {
            addMaterialView(wt)
        }

        dataBinding.root.findViewById<ImageButton>(R.id.material_add_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                val wi = materialContainerData!!.addMaterial()
                addMaterialView(wi)
            }
        })

        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context, this)
        if (context is OnMaterialEditorInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnMaterialEditorInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun setVisibility(vis: Boolean) {
        dataBinding.root.findViewById<LinearLayout>(R.id.material_content_container).setVisibility(if(vis) View.VISIBLE else View.GONE)
    }

    interface OnMaterialEditorInteractionListener {
        fun getMaterialContainerData(): MaterialContainerData
    }

    fun addMaterialView(wi: MaterialData) {
        val inflater = layoutInflater
        val container = dataBinding.root.findViewById<LinearLayout>(R.id.material_content_container) as LinearLayout
        val materialDataBinding: MaterialLayoutBinding = MaterialLayoutBinding.inflate(inflater, null, false)
        materialDataBinding.material = wi
        materialDataBinding.materialContainer = materialContainerData
        materialDataBinding.lifecycleOwner = activity
        materialDataBinding.root.findViewById<ImageButton>(R.id.material_del_button).setOnClickListener(object: View.OnClickListener {
            override fun onClick(btn: View) {
                GlobalScope.launch(Dispatchers.Main) {
                    val answer = showConfirmationDialog(getString(R.string.del_confirmation), btn.context)
                    if (answer == AlertDialog.BUTTON_POSITIVE) {
                        Log.d("Arbeitsbericht.MaterialEditorFragment.material_del_button.onClick", "deleting material element")
                        container.removeView(materialDataBinding.root)
                        materialContainerData!!.removeMaterial(wi)
                    } else {
                        Log.d("Arbeitsbericht.MaterialEditorFragment.material_del_button.onClick", "cancelled deleting material element")
                    }
                }
            }
        })

        val pos = container.getChildCount()
        Log.d("Arbeitsbericht", "Adding material card $pos to UI")
        container.addView(materialDataBinding.root, pos)
    }

}
